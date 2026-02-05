# DHIS2 Configuration Guide for Delivery Verification App

Complete guide for configuring DHIS2 to support the Android Delivery Verification application with multi-driver fleet management.

---

## Table of Contents

1. [Overview](#overview)
2. [Data Model](#data-model)
3. [Initial Setup](#initial-setup)
4. [Fleet Management (Trucks)](#fleet-management-trucks)
5. [Locations (Facilities)](#locations-facilities)
6. [Orders Management](#orders-management)
7. [Route Assignment](#route-assignment)
8. [Delivery Verification](#delivery-verification)
9. [User Management](#user-management)
10. [API Reference](#api-reference)

---

## Overview

### System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    DHIS2 Instance                        │
│                                                          │
│  ┌────────────┐  ┌────────────┐  ┌─────────────────┐   │
│  │   Fleet    │  │ Facilities │  │     Orders      │   │
│  │  (Trucks)  │  │ (Locations)│  │  (Deliveries)   │   │
│  └────────────┘  └────────────┘  └─────────────────┘   │
│                                                          │
│  ┌────────────┐  ┌────────────┐  ┌─────────────────┐   │
│  │   Routes   │  │   Drivers  │  │ Verifications   │   │
│  │(Assignments)│  │   (Users)  │  │   (Completed)   │   │
│  └────────────┘  └────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          ↕
┌─────────────────────────────────────────────────────────┐
│              Android Delivery App                        │
│         (Drivers login and complete deliveries)         │
└─────────────────────────────────────────────────────────┘
```

### Key Concepts

- **Trucks** = Organization Units (Level 2, under root country)
- **Facilities** = Organization Units (delivery locations)
- **Orders** = Events in "Order Management Program"
- **Routes** = Events in "Route Assignment Program" (assigned to trucks)
- **Verifications** = Events in "Delivery Verification Program"
- **Drivers** = DHIS2 Users assigned to truck organization units

---

## Data Model

### Organization Unit Hierarchy

```
Nigeria (Level 1 - Root)
├── TRK-001 (Level 2 - Fleet)
├── TRK-002 (Level 2 - Fleet)
├── TRK-003 (Level 2 - Fleet)
├── Kaduna State (Level 2 - Geography)
│   ├── Kaduna Central Depot (Level 3)
│   │   ├── Primary Health Centre A (Level 4 - Facility)
│   │   ├── Primary Health Centre B (Level 4 - Facility)
│   │   └── General Hospital Kaduna (Level 4 - Facility)
└── Other States...
```

**Important:** Trucks are separate from geographic hierarchy to enable proper permissions.

### Programs

#### 1. Order Management Program
- **Type:** Event Program (without registration)
- **Purpose:** Store delivery orders
- **OrgUnit Assignment:** Facility (delivery destination)

#### 2. Route Assignment Program
- **Type:** Event Program (without registration)
- **Purpose:** Assign routes to trucks
- **OrgUnit Assignment:** Truck (TRK-001, TRK-002, etc.)

#### 3. Delivery Verification Program
- **Type:** Event Program (without registration)
- **Purpose:** Record completed deliveries with proof
- **OrgUnit Assignment:** Facility (where delivery was made)

---

## Initial Setup

### Step 1: Configure Root Organization Unit

Your DHIS2 instance should have a root organization unit (typically the country):

```bash
# Get root org unit ID
curl -s "http://localhost:8080/api/organisationUnits?level=1&fields=id,name" \
  -u admin:district | jq '.organisationUnits[0]'
```

**Example Response:**
```json
{
  "id": "su0tIhYDDMo",
  "name": "Nigeria"
}
```

Save this ID as `ROOT_ORG_UNIT_ID`.

### Step 2: Create Programs and Data Elements

#### Create Order Management Program

**Data Elements Needed:**
```json
{
  "ORDER_ID": "Unique order identifier",
  "ORDER_DATE": "Order creation date",
  "FACILITY_NAME": "Delivery facility name",
  "PRODUCT_CODE": "Product/commodity code",
  "PRODUCT_NAME": "Product/commodity name",
  "QUANTITY_ORDERED_KG": "Quantity in kilograms",
  "QUANTITY_ORDERED_L": "Quantity in liters",
  "DELIVERY_DEADLINE": "Required delivery date",
  "PRIORITY": "Delivery priority (HIGH/MEDIUM/LOW)",
  "SPECIAL_INSTRUCTIONS": "Special handling notes"
}
```

**Create Data Elements:**
```bash
#!/bin/bash
DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Create ORDER_ID data element
curl -X POST "$DHIS2_URL/api/dataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Order ID",
    "shortName": "Order ID",
    "code": "ORDER_ID",
    "valueType": "TEXT",
    "domainType": "TRACKER",
    "aggregationType": "NONE"
  }'

# Create QUANTITY_ORDERED_KG data element
curl -X POST "$DHIS2_URL/api/dataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Quantity Ordered (kg)",
    "shortName": "Qty Ordered kg",
    "code": "QUANTITY_ORDERED_KG",
    "valueType": "NUMBER",
    "domainType": "TRACKER",
    "aggregationType": "SUM"
  }'

# Repeat for all data elements...
```

**Create Order Management Program:**
```bash
curl -X POST "$DHIS2_URL/api/programs" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Order Management Program",
    "shortName": "Orders",
    "programType": "WITHOUT_REGISTRATION",
    "publicAccess": "rwr-----"
  }'
```

#### Create Route Assignment Program

**Data Elements Needed:**
```json
{
  "ROUTE_ID": "Unique route identifier",
  "ROUTE_NAME": "Route name/description",
  "ASSIGNED_TRUCK": "Truck code (TRK-001, etc.)",
  "ASSIGNED_DRIVER": "Driver ID",
  "ROUTE_DATE": "Scheduled delivery date",
  "ROUTE_STATUS": "PENDING/IN_PROGRESS/COMPLETED",
  "ROUTE_DETAILS": "JSON with stop sequence and orders",
  "TOTAL_DISTANCE_KM": "Total route distance",
  "ESTIMATED_DURATION_MIN": "Estimated time in minutes",
  "STOP_COUNT": "Number of stops",
  "TOTAL_WEIGHT_KG": "Total cargo weight",
  "TOTAL_VOLUME_L": "Total cargo volume"
}
```

**Create Route Assignment Program:**
```bash
curl -X POST "$DHIS2_URL/api/programs" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Route Assignment Program",
    "shortName": "Routes",
    "programType": "WITHOUT_REGISTRATION",
    "publicAccess": "rwr-----"
  }'
```

#### Create Delivery Verification Program

**Data Elements Needed:**
```json
{
  "VERIFICATION_ID": "Unique verification ID",
  "ORDER_ID": "Reference to original order",
  "ROUTE_ID": "Reference to route",
  "VERIFIED_BY_DRIVER": "Driver ID who completed",
  "VERIFIED_BY_TRUCK": "Truck code",
  "DELIVERY_DATE": "Actual delivery date/time",
  "QUANTITY_DELIVERED_KG": "Actual quantity delivered (kg)",
  "QUANTITY_DELIVERED_L": "Actual quantity delivered (L)",
  "RECIPIENT_NAME": "Person who received delivery",
  "RECIPIENT_TITLE": "Recipient job title",
  "SIGNATURE_IMAGE": "Base64 signature image",
  "PHOTO_PROOF": "Base64 photo of delivery",
  "GPS_LATITUDE": "Delivery location latitude",
  "GPS_LONGITUDE": "Delivery location longitude",
  "NOTES": "Additional delivery notes",
  "DELIVERY_STATUS": "SUCCESS/PARTIAL/FAILED"
}
```

**Create Delivery Verification Program:**
```bash
curl -X POST "$DHIS2_URL/api/programs" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Delivery Verification Program",
    "shortName": "Verifications",
    "programType": "WITHOUT_REGISTRATION",
    "publicAccess": "rwr-----"
  }'
```

### Step 3: Create User Roles

**Delivery Driver Role:**
```bash
curl -X POST "$DHIS2_URL/api/userRoles" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Delivery Driver",
    "description": "Role for delivery drivers using mobile app",
    "authorities": [
      "F_PROGRAM_EVENT_ADD",
      "F_PROGRAM_EVENT_UPDATE",
      "F_VIEW_EVENT_ANALYTICS",
      "F_PROGRAM_PUBLIC_ADD",
      "F_UNCOMPLETED_EVENT_EDIT",
      "F_PROGRAM_ENROLLMENT",
      "F_PROGRAMSTAGE_ADD",
      "M_dhis-web-dashboard"
    ]
  }'
```

**Fleet Manager Role:**
```bash
curl -X POST "$DHIS2_URL/api/userRoles" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Fleet Manager",
    "description": "Role for managing fleet, routes, and orders",
    "authorities": [
      "F_PROGRAM_EVENT_ADD",
      "F_PROGRAM_EVENT_UPDATE",
      "F_PROGRAM_EVENT_DELETE",
      "F_VIEW_EVENT_ANALYTICS",
      "F_ORGANISATIONUNIT_ADD",
      "F_USER_ADD",
      "F_USER_VIEW",
      "ALL"
    ]
  }'
```

---

## Fleet Management (Trucks)

### Adding a New Truck

Trucks are represented as Organization Units at Level 2 (directly under the root country).

#### Step 1: Create Truck Organization Unit

```bash
#!/bin/bash
DHIS2_URL="http://localhost:8080"
AUTH="admin:district"
ROOT_ORG_UNIT_ID="su0tIhYDDMo"  # Your root org unit ID

# Truck details
TRUCK_CODE="TRK-004"
TRUCK_NAME="Truck TRK-004 (5-ton)"
CAPACITY_KG=5000
CAPACITY_L=8000
TRUCK_TYPE="TRUCK"

# Create truck org unit
RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "'"$TRUCK_NAME"'",
    "shortName": "'"$TRUCK_CODE"'",
    "code": "'"$TRUCK_CODE"'",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$ROOT_ORG_UNIT_ID"'"}
  }')

TRUCK_ID=$(echo "$RESPONSE" | jq -r '.response.uid')
echo "Created truck: $TRUCK_CODE (ID: $TRUCK_ID)"

# Save to fleet_ids.txt
echo "${TRUCK_CODE//-/_}_ID=$TRUCK_ID" >> fleet_ids.txt
```

#### Step 2: Add Truck Metadata (Optional)

You can store additional truck information using organization unit attributes:

```bash
# Create attributes for truck specifications
curl -X POST "$DHIS2_URL/api/attributes" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Vehicle Type",
    "code": "VEHICLE_TYPE",
    "valueType": "TEXT",
    "organisationUnitAttribute": true
  }'

curl -X POST "$DHIS2_URL/api/attributes" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Capacity (kg)",
    "code": "CAPACITY_KG",
    "valueType": "NUMBER",
    "organisationUnitAttribute": true
  }'

# Update truck with attributes
curl -X PATCH "$DHIS2_URL/api/organisationUnits/$TRUCK_ID" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "attributeValues": [
      {"attribute": {"id": "VEHICLE_TYPE_ATTR_ID"}, "value": "TRUCK"},
      {"attribute": {"id": "CAPACITY_KG_ATTR_ID"}, "value": "5000"}
    ]
  }'
```

#### Complete Script: Add Truck

```bash
#!/bin/bash
# add_truck.sh - Add a new truck to the fleet

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"
ROOT_ORG_UNIT_ID="su0tIhYDDMo"

# Truck configuration
TRUCK_CODE="$1"
TRUCK_NAME="$2"
TRUCK_TYPE="${3:-TRUCK}"  # TRUCK, VAN, MOTORCYCLE
CAPACITY_KG="${4:-3000}"
CAPACITY_L="${5:-5000}"

if [ -z "$TRUCK_CODE" ] || [ -z "$TRUCK_NAME" ]; then
  echo "Usage: ./add_truck.sh TRUCK_CODE \"TRUCK_NAME\" [TYPE] [CAPACITY_KG] [CAPACITY_L]"
  echo "Example: ./add_truck.sh TRK-004 \"Truck TRK-004 (5-ton)\" TRUCK 5000 8000"
  exit 1
fi

echo "Adding truck: $TRUCK_CODE..."

RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "'"$TRUCK_NAME"'",
    "shortName": "'"$TRUCK_CODE"'",
    "code": "'"$TRUCK_CODE"'",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$ROOT_ORG_UNIT_ID"'"}
  }')

STATUS=$(echo "$RESPONSE" | jq -r '.status // "ERROR"')
TRUCK_ID=$(echo "$RESPONSE" | jq -r '.response.uid // empty')

if [ "$STATUS" = "OK" ] && [ -n "$TRUCK_ID" ]; then
  echo "✅ Truck created successfully!"
  echo "   Code: $TRUCK_CODE"
  echo "   ID: $TRUCK_ID"
  echo "   Type: $TRUCK_TYPE"
  echo "   Capacity: ${CAPACITY_KG}kg / ${CAPACITY_L}L"

  # Save to fleet_ids.txt
  VAR_NAME="${TRUCK_CODE//-/_}_ID"
  echo "$VAR_NAME=$TRUCK_ID" >> fleet_ids.txt

  echo ""
  echo "Next: Create driver user for this truck"
  echo "   ./create_driver.sh <username> <name> $TRUCK_ID"
else
  echo "❌ Failed to create truck"
  echo "$RESPONSE" | jq
fi
```

**Usage:**
```bash
chmod +x add_truck.sh
./add_truck.sh TRK-004 "Truck TRK-004 (5-ton)" TRUCK 5000 8000
```

---

## Locations (Facilities)

Facilities are the delivery destinations (health centres, hospitals, warehouses, etc.).

### Adding a New Facility

#### Step 1: Determine Parent Organization Unit

Facilities should be organized geographically:

```bash
# Find the appropriate parent (e.g., a district or zone)
curl -s "$DHIS2_URL/api/organisationUnits?level=3&fields=id,name" \
  -u "$AUTH" | jq '.organisationUnits[] | select(.name | contains("Kaduna"))'
```

#### Step 2: Create Facility Organization Unit

```bash
#!/bin/bash
# add_facility.sh - Add a new delivery facility

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

FACILITY_CODE="$1"
FACILITY_NAME="$2"
PARENT_ORG_UNIT_ID="$3"
LATITUDE="$4"
LONGITUDE="$5"

if [ -z "$FACILITY_CODE" ] || [ -z "$FACILITY_NAME" ] || [ -z "$PARENT_ORG_UNIT_ID" ]; then
  echo "Usage: ./add_facility.sh FACILITY_CODE \"FACILITY_NAME\" PARENT_ORG_UNIT_ID [LATITUDE] [LONGITUDE]"
  echo "Example: ./add_facility.sh PHC-KD-001 \"Primary Health Centre Kaduna A\" wjDCrWdYslY 10.5264 7.4390"
  exit 1
fi

echo "Adding facility: $FACILITY_CODE..."

# Build JSON payload
PAYLOAD='{
  "name": "'"$FACILITY_NAME"'",
  "shortName": "'"${FACILITY_NAME:0:50}"'",
  "code": "'"$FACILITY_CODE"'",
  "openingDate": "2020-01-01",
  "parent": {"id": "'"$PARENT_ORG_UNIT_ID"'"}
}'

# Add coordinates if provided
if [ -n "$LATITUDE" ] && [ -n "$LONGITUDE" ]; then
  PAYLOAD=$(echo "$PAYLOAD" | jq --arg lat "$LATITUDE" --arg lon "$LONGITUDE" \
    '. + {coordinates: "[" + $lon + "," + $lat + "]"}')
fi

RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d "$PAYLOAD")

STATUS=$(echo "$RESPONSE" | jq -r '.status // "ERROR"')
FACILITY_ID=$(echo "$RESPONSE" | jq -r '.response.uid // empty')

if [ "$STATUS" = "OK" ] && [ -n "$FACILITY_ID" ]; then
  echo "✅ Facility created successfully!"
  echo "   Code: $FACILITY_CODE"
  echo "   Name: $FACILITY_NAME"
  echo "   ID: $FACILITY_ID"
  [ -n "$LATITUDE" ] && echo "   Location: $LATITUDE, $LONGITUDE"

  # Save to facility_ids.txt
  echo "$FACILITY_CODE=$FACILITY_ID" >> facility_ids.txt
else
  echo "❌ Failed to create facility"
  echo "$RESPONSE" | jq
fi
```

**Usage:**
```bash
chmod +x add_facility.sh

# Add facility with coordinates
./add_facility.sh PHC-KD-001 "Primary Health Centre Kaduna A" wjDCrWdYslY 10.5264 7.4390

# Add facility without coordinates
./add_facility.sh GH-KD-002 "General Hospital Kaduna B" wjDCrWdYslY
```

#### Step 3: Bulk Import Facilities

For importing many facilities at once:

```bash
#!/bin/bash
# bulk_import_facilities.sh - Import facilities from CSV

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"
CSV_FILE="$1"

if [ -z "$CSV_FILE" ] || [ ! -f "$CSV_FILE" ]; then
  echo "Usage: ./bulk_import_facilities.sh facilities.csv"
  echo ""
  echo "CSV Format:"
  echo "code,name,parent_id,latitude,longitude"
  echo "PHC-001,Primary Health Centre A,wjDCrWdYslY,10.5264,7.4390"
  exit 1
fi

echo "Importing facilities from $CSV_FILE..."
echo ""

# Skip header, read each line
tail -n +2 "$CSV_FILE" | while IFS=, read -r code name parent_id lat lon; do
  echo "Adding: $name ($code)..."

  PAYLOAD='{
    "name": "'"$name"'",
    "shortName": "'"${name:0:50}"'",
    "code": "'"$code"'",
    "openingDate": "2020-01-01",
    "parent": {"id": "'"$parent_id"'"}
  }'

  if [ -n "$lat" ] && [ -n "$lon" ]; then
    PAYLOAD=$(echo "$PAYLOAD" | jq --arg lat "$lat" --arg lon "$lon" \
      '. + {coordinates: "[" + $lon + "," + $lat + "]"}')
  fi

  RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
    -H "Content-Type: application/json" \
    -u "$AUTH" \
    -d "$PAYLOAD")

  STATUS=$(echo "$RESPONSE" | jq -r '.status // "ERROR"')
  if [ "$STATUS" = "OK" ]; then
    FACILITY_ID=$(echo "$RESPONSE" | jq -r '.response.uid')
    echo "  ✅ Created (ID: $FACILITY_ID)"
  else
    echo "  ❌ Failed"
  fi
done

echo ""
echo "Import complete!"
```

**Create facilities.csv:**
```csv
code,name,parent_id,latitude,longitude
PHC-KD-001,Primary Health Centre Kaduna A,wjDCrWdYslY,10.5264,7.4390
PHC-KD-002,Primary Health Centre Kaduna B,wjDCrWdYslY,10.5312,7.4421
GH-KD-001,General Hospital Kaduna Central,wjDCrWdYslY,10.5225,7.4387
```

**Usage:**
```bash
chmod +x bulk_import_facilities.sh
./bulk_import_facilities.sh facilities.csv
```

---

## Orders Management

Orders represent delivery requests - what needs to be delivered where.

### Adding a New Order

#### Single Order Creation

```bash
#!/bin/bash
# add_order.sh - Add a new delivery order

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"
ORDER_PROGRAM_ID="<YOUR_ORDER_PROGRAM_ID>"

ORDER_ID="$1"
FACILITY_ID="$2"
PRODUCT_NAME="$3"
QUANTITY_KG="$4"
DELIVERY_DATE="$5"

if [ -z "$ORDER_ID" ] || [ -z "$FACILITY_ID" ] || [ -z "$PRODUCT_NAME" ] || [ -z "$QUANTITY_KG" ]; then
  echo "Usage: ./add_order.sh ORDER_ID FACILITY_ID PRODUCT_NAME QUANTITY_KG DELIVERY_DATE"
  echo "Example: ./add_order.sh ORD-001 abc123def \"Medical Supplies\" 150 2026-02-10"
  exit 1
fi

DELIVERY_DATE="${DELIVERY_DATE:-$(date +%Y-%m-%d)}"

echo "Creating order: $ORDER_ID..."

RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$ORDER_PROGRAM_ID"'",
    "orgUnit": "'"$FACILITY_ID"'",
    "eventDate": "'"$DELIVERY_DATE"'",
    "status": "ACTIVE",
    "dataValues": [
      {
        "dataElement": "ORDER_ID_DE",
        "value": "'"$ORDER_ID"'"
      },
      {
        "dataElement": "PRODUCT_NAME_DE",
        "value": "'"$PRODUCT_NAME"'"
      },
      {
        "dataElement": "QUANTITY_ORDERED_KG_DE",
        "value": "'"$QUANTITY_KG"'"
      },
      {
        "dataElement": "DELIVERY_DEADLINE_DE",
        "value": "'"$DELIVERY_DATE"'"
      },
      {
        "dataElement": "ORDER_STATUS_DE",
        "value": "PENDING"
      }
    ]
  }')

STATUS=$(echo "$RESPONSE" | jq -r '.response.status // "ERROR"')
EVENT_ID=$(echo "$RESPONSE" | jq -r '.response.importSummaries[0].reference // empty')

if [ "$STATUS" = "SUCCESS" ] && [ -n "$EVENT_ID" ]; then
  echo "✅ Order created successfully!"
  echo "   Order ID: $ORDER_ID"
  echo "   Event ID: $EVENT_ID"
  echo "   Facility: $FACILITY_ID"
  echo "   Product: $PRODUCT_NAME"
  echo "   Quantity: ${QUANTITY_KG}kg"
  echo "   Delivery Date: $DELIVERY_DATE"
else
  echo "❌ Failed to create order"
  echo "$RESPONSE" | jq
fi
```

#### Bulk Order Import

```bash
#!/bin/bash
# bulk_import_orders.sh - Import orders from CSV

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"
ORDER_PROGRAM_ID="<YOUR_ORDER_PROGRAM_ID>"
CSV_FILE="$1"

if [ -z "$CSV_FILE" ] || [ ! -f "$CSV_FILE" ]; then
  echo "Usage: ./bulk_import_orders.sh orders.csv"
  echo ""
  echo "CSV Format:"
  echo "order_id,facility_id,product_name,quantity_kg,delivery_date,priority"
  echo "ORD-001,abc123def,Medical Supplies,150,2026-02-10,HIGH"
  exit 1
fi

echo "Importing orders from $CSV_FILE..."
echo ""

# Get data element IDs (you'll need to set these)
ORDER_ID_DE="<YOUR_ORDER_ID_DATA_ELEMENT>"
PRODUCT_NAME_DE="<YOUR_PRODUCT_NAME_DATA_ELEMENT>"
QUANTITY_KG_DE="<YOUR_QUANTITY_KG_DATA_ELEMENT>"
DELIVERY_DATE_DE="<YOUR_DELIVERY_DATE_DATA_ELEMENT>"
PRIORITY_DE="<YOUR_PRIORITY_DATA_ELEMENT>"

tail -n +2 "$CSV_FILE" | while IFS=, read -r order_id facility_id product quantity date priority; do
  echo "Creating order: $order_id..."

  RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/events" \
    -H "Content-Type: application/json" \
    -u "$AUTH" \
    -d '{
      "program": "'"$ORDER_PROGRAM_ID"'",
      "orgUnit": "'"$facility_id"'",
      "eventDate": "'"$date"'",
      "status": "ACTIVE",
      "dataValues": [
        {"dataElement": "'"$ORDER_ID_DE"'", "value": "'"$order_id"'"},
        {"dataElement": "'"$PRODUCT_NAME_DE"'", "value": "'"$product"'"},
        {"dataElement": "'"$QUANTITY_KG_DE"'", "value": "'"$quantity"'"},
        {"dataElement": "'"$DELIVERY_DATE_DE"'", "value": "'"$date"'"},
        {"dataElement": "'"$PRIORITY_DE"'", "value": "'"$priority"'"}
      ]
    }')

  STATUS=$(echo "$RESPONSE" | jq -r '.response.status // "ERROR"')
  if [ "$STATUS" = "SUCCESS" ]; then
    echo "  ✅ Created"
  else
    echo "  ❌ Failed"
  fi
done

echo ""
echo "Import complete!"
```

**Create orders.csv:**
```csv
order_id,facility_id,product_name,quantity_kg,delivery_date,priority
ORD-001,PHC-KD-001-ID,Medical Supplies,150,2026-02-10,HIGH
ORD-002,PHC-KD-001-ID,Vaccines,50,2026-02-10,HIGH
ORD-003,PHC-KD-002-ID,Medical Supplies,200,2026-02-10,MEDIUM
ORD-004,GH-KD-001-ID,Pharmaceuticals,500,2026-02-11,HIGH
```

---

## Route Assignment

Routes are created by the DRO (Delivery Route Optimization) system and assigned to trucks.

### Creating a Route Assignment

```bash
#!/bin/bash
# assign_route.sh - Assign optimized route to truck

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"
ROUTE_PROGRAM_ID="qzRN0undLV4"  # Your Route Assignment Program ID

ROUTE_ID="$1"
TRUCK_ID="$2"
ROUTE_DATE="$3"
ROUTE_DETAILS_JSON="$4"  # JSON string with route details

if [ -z "$ROUTE_ID" ] || [ -z "$TRUCK_ID" ] || [ -z "$ROUTE_DATE" ]; then
  echo "Usage: ./assign_route.sh ROUTE_ID TRUCK_ID ROUTE_DATE [ROUTE_DETAILS_JSON]"
  echo "Example: ./assign_route.sh ROUTE-001 uMce0FFzxd0 2026-02-10 '{\"stops\":[...]}'"
  exit 1
fi

ROUTE_DETAILS="${ROUTE_DETAILS_JSON:-{}}"

echo "Assigning route: $ROUTE_ID to truck $TRUCK_ID..."

RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$ROUTE_PROGRAM_ID"'",
    "orgUnit": "'"$TRUCK_ID"'",
    "eventDate": "'"$ROUTE_DATE"'",
    "status": "ACTIVE",
    "dataValues": [
      {
        "dataElement": "ROUTE_ID_DE",
        "value": "'"$ROUTE_ID"'"
      },
      {
        "dataElement": "ROUTE_STATUS_DE",
        "value": "PENDING"
      },
      {
        "dataElement": "ROUTE_DETAILS_DE",
        "value": '"$(echo "$ROUTE_DETAILS" | jq -c)"'
      }
    ]
  }')

STATUS=$(echo "$RESPONSE" | jq -r '.response.status // "ERROR"')
EVENT_ID=$(echo "$RESPONSE" | jq -r '.response.importSummaries[0].reference // empty')

if [ "$STATUS" = "SUCCESS" ] && [ -n "$EVENT_ID" ]; then
  echo "✅ Route assigned successfully!"
  echo "   Route ID: $ROUTE_ID"
  echo "   Event ID: $EVENT_ID"
  echo "   Truck: $TRUCK_ID"
  echo "   Date: $ROUTE_DATE"
  echo ""
  echo "Driver assigned to this truck can now see this route in the app!"
else
  echo "❌ Failed to assign route"
  echo "$RESPONSE" | jq
fi
```

### Route Details JSON Structure

```json
{
  "routeId": "ROUTE-001",
  "routeName": "Kaduna Central Route A",
  "totalDistance": 45.2,
  "estimatedDuration": 180,
  "stops": [
    {
      "stopNumber": 1,
      "facilityId": "PHC-KD-001-ID",
      "facilityName": "Primary Health Centre Kaduna A",
      "latitude": 10.5264,
      "longitude": 7.4390,
      "orders": [
        {
          "orderId": "ORD-001",
          "productName": "Medical Supplies",
          "quantityKg": 150,
          "quantityL": 0
        }
      ],
      "estimatedArrival": "2026-02-10T08:30:00",
      "distanceFromPrevious": 12.5
    },
    {
      "stopNumber": 2,
      "facilityId": "PHC-KD-002-ID",
      "facilityName": "Primary Health Centre Kaduna B",
      "latitude": 10.5312,
      "longitude": 7.4421,
      "orders": [
        {
          "orderId": "ORD-003",
          "productName": "Medical Supplies",
          "quantityKg": 200,
          "quantityL": 0
        }
      ],
      "estimatedArrival": "2026-02-10T09:15:00",
      "distanceFromPrevious": 5.8
    }
  ],
  "totalWeight": 350,
  "totalVolume": 0
}
```

---

## Delivery Verification

Verifications are created by drivers in the mobile app when they complete deliveries.

### Verification Data Structure

When a driver completes a delivery, the app creates a verification event:

```json
{
  "program": "VERIFICATION_PROGRAM_ID",
  "orgUnit": "FACILITY_ID",  // Where delivery was made
  "eventDate": "2026-02-10T09:45:00",
  "status": "COMPLETED",
  "dataValues": [
    {
      "dataElement": "VERIFICATION_ID_DE",
      "value": "VER-001"
    },
    {
      "dataElement": "ORDER_ID_DE",
      "value": "ORD-001"
    },
    {
      "dataElement": "ROUTE_ID_DE",
      "value": "ROUTE-001"
    },
    {
      "dataElement": "VERIFIED_BY_DRIVER_DE",
      "value": "driver001"
    },
    {
      "dataElement": "VERIFIED_BY_TRUCK_DE",
      "value": "TRK-001"
    },
    {
      "dataElement": "QUANTITY_DELIVERED_KG_DE",
      "value": "150"
    },
    {
      "dataElement": "RECIPIENT_NAME_DE",
      "value": "Dr. Ahmed Ibrahim"
    },
    {
      "dataElement": "RECIPIENT_TITLE_DE",
      "value": "Medical Officer"
    },
    {
      "dataElement": "SIGNATURE_IMAGE_DE",
      "value": "data:image/png;base64,iVBORw0KGgo..."
    },
    {
      "dataElement": "PHOTO_PROOF_DE",
      "value": "data:image/jpeg;base64,/9j/4AAQSkZJ..."
    },
    {
      "dataElement": "GPS_LATITUDE_DE",
      "value": "10.5264"
    },
    {
      "dataElement": "GPS_LONGITUDE_DE",
      "value": "7.4390"
    },
    {
      "dataElement": "DELIVERY_STATUS_DE",
      "value": "SUCCESS"
    },
    {
      "dataElement": "NOTES_DE",
      "value": "Delivered successfully, all items received in good condition"
    }
  ]
}
```

---

## User Management

### Creating Driver Users

```bash
#!/bin/bash
# create_driver.sh - Create a new driver user

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

USERNAME="$1"
FIRST_NAME="$2"
LAST_NAME="$3"
TRUCK_ID="$4"
PASSWORD="${5:-Driver123!}"
PHONE="$6"

if [ -z "$USERNAME" ] || [ -z "$FIRST_NAME" ] || [ -z "$LAST_NAME" ] || [ -z "$TRUCK_ID" ]; then
  echo "Usage: ./create_driver.sh USERNAME FIRST_NAME LAST_NAME TRUCK_ID [PASSWORD] [PHONE]"
  echo "Example: ./create_driver.sh driver004 John Doe uMce0FFzxd0 SecurePass123 +234701000004"
  exit 1
fi

# Get Delivery Driver role ID
DRIVER_ROLE_ID=$(curl -s "$DHIS2_URL/api/userRoles?filter=name:eq:Delivery%20Driver&fields=id" \
  -u "$AUTH" | jq -r '.userRoles[0].id // empty')

if [ -z "$DRIVER_ROLE_ID" ]; then
  echo "❌ Delivery Driver role not found. Create it first."
  exit 1
fi

echo "Creating driver user: $USERNAME..."

RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/users" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "firstName": "'"$FIRST_NAME"'",
    "surname": "'"$LAST_NAME"'",
    "email": "'"$USERNAME@example.com"'",
    "phoneNumber": "'"${PHONE:-+234700000000}"'",
    "userCredentials": {
      "username": "'"$USERNAME"'",
      "password": "'"$PASSWORD"'",
      "userRoles": [{"id": "'"$DRIVER_ROLE_ID"'"}]
    },
    "organisationUnits": [{"id": "'"$TRUCK_ID"'"}],
    "dataViewOrganisationUnits": [{"id": "'"$TRUCK_ID"'"}]
  }')

STATUS=$(echo "$RESPONSE" | jq -r '.status // "ERROR"')
USER_ID=$(echo "$RESPONSE" | jq -r '.response.uid // empty')

if [ "$STATUS" = "OK" ] && [ -n "$USER_ID" ]; then
  echo "✅ Driver created successfully!"
  echo "   Username: $USERNAME"
  echo "   Password: $PASSWORD"
  echo "   User ID: $USER_ID"
  echo "   Assigned Truck: $TRUCK_ID"
  echo ""
  echo "Driver can now login to the Android app!"
else
  echo "❌ Failed to create driver"
  echo "$RESPONSE" | jq
fi
```

**Usage:**
```bash
chmod +x create_driver.sh
./create_driver.sh driver004 John Doe uMce0FFzxd0 SecurePass123 +234701000004
```

---

## API Reference

### Quick Reference for Common Operations

#### Get All Trucks
```bash
curl -s "http://localhost:8080/api/organisationUnits?level=2&fields=id,name,code" \
  -u admin:district | jq '.organisationUnits[] | select(.code | startswith("TRK"))'
```

#### Get All Facilities
```bash
curl -s "http://localhost:8080/api/organisationUnits?level=4&fields=id,name,code,coordinates" \
  -u admin:district | jq
```

#### Get All Orders
```bash
curl -s "http://localhost:8080/api/events?program=ORDER_PROGRAM_ID&pageSize=100" \
  -u admin:district | jq '.events'
```

#### Get Routes for Specific Truck
```bash
curl -s "http://localhost:8080/api/events?program=ROUTE_PROGRAM_ID&orgUnit=TRUCK_ID" \
  -u admin:district | jq '.events'
```

#### Get Verifications for Specific Facility
```bash
curl -s "http://localhost:8080/api/events?program=VERIFICATION_PROGRAM_ID&orgUnit=FACILITY_ID" \
  -u admin:district | jq '.events'
```

#### Update Order Status
```bash
curl -X PUT "http://localhost:8080/api/events/EVENT_ID" \
  -H "Content-Type: application/json" \
  -u admin:district \
  -d '{
    "dataValues": [
      {
        "dataElement": "ORDER_STATUS_DE",
        "value": "COMPLETED"
      }
    ]
  }'
```

---

## Complete Setup Checklist

### Initial Configuration
- [ ] Root organization unit identified
- [ ] Fleet organization structure created (trucks at Level 2)
- [ ] Geographic organization structure created (states, districts, facilities)
- [ ] Data elements created for all programs
- [ ] Order Management Program created
- [ ] Route Assignment Program created
- [ ] Delivery Verification Program created
- [ ] User roles created (Delivery Driver, Fleet Manager)
- [ ] Program sharing configured (publicAccess: "rwr-----")

### Fleet Setup
- [ ] TRK-001 created (with capacity metadata)
- [ ] TRK-002 created (with capacity metadata)
- [ ] TRK-003 created (with capacity metadata)
- [ ] Additional trucks added as needed
- [ ] fleet_ids.txt populated with truck IDs

### Facilities Setup
- [ ] Facilities imported or created
- [ ] Facility coordinates added
- [ ] facility_ids.txt populated with facility IDs

### User Management
- [ ] driver001 created and assigned to TRK-001
- [ ] driver002 created and assigned to TRK-002
- [ ] driver003 created and assigned to TRK-003
- [ ] Additional drivers created as needed
- [ ] Driver logins tested

### Testing
- [ ] Test order created
- [ ] Test route assigned to truck
- [ ] Driver login tested
- [ ] Driver can see assigned routes
- [ ] Delivery verification tested
- [ ] Security verified (drivers only see their routes)

---

## Troubleshooting

### Drivers Can't See Routes
**Problem:** Driver logs in but sees no routes.

**Solutions:**
1. Check program sharing: `publicAccess` must be `rwr-----`
   ```bash
   curl -s "http://localhost:8080/api/sharing?type=program&id=ROUTE_PROGRAM_ID" \
     -u admin:district | jq '.object.publicAccess'
   ```

2. Check route orgUnit matches driver's assigned truck:
   ```bash
   # Get driver's truck
   curl -s "http://localhost:8080/api/me" -u driver001:Password | jq '.organisationUnits[0].id'

   # Check route orgUnit
   curl -s "http://localhost:8080/api/events/ROUTE_EVENT_ID" -u admin:district | jq '.orgUnit'
   ```

3. Verify truck is at Level 2:
   ```bash
   curl -s "http://localhost:8080/api/organisationUnits/TRUCK_ID" -u admin:district | jq '.level'
   ```

### Orders Not Appearing
**Problem:** Orders created but not visible.

**Solutions:**
1. Check order was created successfully
2. Verify facility orgUnit ID is correct
3. Check order program ID is correct

### Permission Denied Errors
**Problem:** User gets 401/403 errors.

**Solutions:**
1. Verify user role has correct authorities
2. Check user is assigned to correct orgUnit
3. Verify program sharing allows access

---

## Support and Resources

### DHIS2 Documentation
- [DHIS2 API Documentation](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/introduction.html)
- [Organization Units](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_organisation_units)
- [Events](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/tracker.html#webapi_events)
- [User Management](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/users.html)

### Scripts Repository
All scripts mentioned in this guide are available in the project repository.

---

**Last Updated:** 2026-02-04
**Version:** 1.0
**DHIS2 Version:** 2.40+
