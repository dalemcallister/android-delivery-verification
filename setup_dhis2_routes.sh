#!/bin/bash

# DHIS2 Route Setup Script
# This script creates all necessary data elements and programs for route management

DHIS2_URL="http://192.168.88.9:8080"
AUTH="admin:district"

echo "================================================"
echo "DHIS2 Route Data Setup"
echo "================================================"
echo ""

# Step 1: Create Data Elements for Route Information
echo "Step 1: Creating Route Data Elements..."
echo ""

# Route ID
echo "Creating: Route ID data element..."
ROUTE_ID=$(curl -s -X POST "$DHIS2_URL/api/dataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Route ID",
    "shortName": "Route ID",
    "code": "ROUTE_ID",
    "valueType": "TEXT",
    "aggregationType": "NONE",
    "domainType": "TRACKER"
  }' | jq -r '.response.uid // empty')

if [ -n "$ROUTE_ID" ]; then
  echo "✅ Created Route ID: $ROUTE_ID"
else
  echo "⚠️  Route ID may already exist, checking..."
  ROUTE_ID=$(curl -s "$DHIS2_URL/api/dataElements?filter=code:eq:ROUTE_ID&fields=id" -u "$AUTH" | jq -r '.dataElements[0].id // empty')
  if [ -n "$ROUTE_ID" ]; then
    echo "✅ Found existing Route ID: $ROUTE_ID"
  else
    echo "❌ Failed to create/find Route ID"
  fi
fi
echo ""

# Route Details (JSON)
echo "Creating: Route Details data element..."
ROUTE_DETAILS=$(curl -s -X POST "$DHIS2_URL/api/dataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Route Details JSON",
    "shortName": "Route Details",
    "code": "ROUTE_DETAILS",
    "valueType": "LONG_TEXT",
    "aggregationType": "NONE",
    "domainType": "TRACKER"
  }' | jq -r '.response.uid // empty')

if [ -n "$ROUTE_DETAILS" ]; then
  echo "✅ Created Route Details: $ROUTE_DETAILS"
else
  ROUTE_DETAILS=$(curl -s "$DHIS2_URL/api/dataElements?filter=code:eq:ROUTE_DETAILS&fields=id" -u "$AUTH" | jq -r '.dataElements[0].id // empty')
  if [ -n "$ROUTE_DETAILS" ]; then
    echo "✅ Found existing Route Details: $ROUTE_DETAILS"
  fi
fi
echo ""

# Vehicle Type
echo "Creating: Vehicle Type data element..."
VEHICLE_TYPE=$(curl -s -X POST "$DHIS2_URL/api/dataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Vehicle Type",
    "shortName": "Vehicle Type",
    "code": "VEHICLE_TYPE",
    "valueType": "TEXT",
    "aggregationType": "NONE",
    "domainType": "TRACKER"
  }' | jq -r '.response.uid // empty')

if [ -n "$VEHICLE_TYPE" ]; then
  echo "✅ Created Vehicle Type: $VEHICLE_TYPE"
else
  VEHICLE_TYPE=$(curl -s "$DHIS2_URL/api/dataElements?filter=code:eq:VEHICLE_TYPE&fields=id" -u "$AUTH" | jq -r '.dataElements[0].id // empty')
  if [ -n "$VEHICLE_TYPE" ]; then
    echo "✅ Found existing Vehicle Type: $VEHICLE_TYPE"
  fi
fi
echo ""

# Total Distance
echo "Creating: Total Distance data element..."
TOTAL_DISTANCE=$(curl -s -X POST "$DHIS2_URL/api/dataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Total Distance",
    "shortName": "Total Distance",
    "code": "TOTAL_DISTANCE",
    "valueType": "NUMBER",
    "aggregationType": "SUM",
    "domainType": "TRACKER"
  }' | jq -r '.response.uid // empty')

if [ -n "$TOTAL_DISTANCE" ]; then
  echo "✅ Created Total Distance: $TOTAL_DISTANCE"
else
  TOTAL_DISTANCE=$(curl -s "$DHIS2_URL/api/dataElements?filter=code:eq:TOTAL_DISTANCE&fields=id" -u "$AUTH" | jq -r '.dataElements[0].id // empty')
  if [ -n "$TOTAL_DISTANCE" ]; then
    echo "✅ Found existing Total Distance: $TOTAL_DISTANCE"
  fi
fi
echo ""

# Step 2: Get existing Order Volume and Weight
echo "Step 2: Getting existing Order data elements..."
ORDER_VOLUME=$(curl -s "$DHIS2_URL/api/dataElements/cNfWOj9OlyR" -u "$AUTH" | jq -r '.id // empty')
ORDER_WEIGHT=$(curl -s "$DHIS2_URL/api/dataElements/MtydVLMZaEN" -u "$AUTH" | jq -r '.id // empty')

if [ -n "$ORDER_VOLUME" ]; then
  echo "✅ Found Order Volume: $ORDER_VOLUME"
else
  echo "❌ Order Volume not found"
fi

if [ -n "$ORDER_WEIGHT" ]; then
  echo "✅ Found Order Weight: $ORDER_WEIGHT"
else
  echo "❌ Order Weight not found"
fi
echo ""

# Step 3: Create Program
echo "Step 3: Creating Route Assignment Program..."
PROGRAM_RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/programs" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Route Assignment Program",
    "shortName": "Routes",
    "programType": "WITHOUT_REGISTRATION",
    "code": "ROUTE_ASSIGN",
    "description": "Program for managing delivery route assignments"
  }')

PROGRAM_ID=$(echo "$PROGRAM_RESPONSE" | jq -r '.response.uid // empty')

if [ -n "$PROGRAM_ID" ]; then
  echo "✅ Created Program: $PROGRAM_ID"
else
  echo "⚠️  Program may already exist, checking..."
  PROGRAM_ID=$(curl -s "$DHIS2_URL/api/programs?filter=code:eq:ROUTE_ASSIGN&fields=id" -u "$AUTH" | jq -r '.programs[0].id // empty')
  if [ -n "$PROGRAM_ID" ]; then
    echo "✅ Found existing Program: $PROGRAM_ID"
  else
    echo "❌ Failed to create/find Program"
  fi
fi
echo ""

# Save IDs to file for app configuration
echo "Step 4: Saving configuration..."
cat > /Users/dalemcallister/Desktop/connexidevepod/dhis2_config.txt << EOF
# DHIS2 Configuration for Android App
# Generated: $(date)

PROGRAM_ID=$PROGRAM_ID
ROUTE_ID_DE=$ROUTE_ID
ROUTE_DETAILS_DE=$ROUTE_DETAILS
VEHICLE_TYPE_DE=$VEHICLE_TYPE
TOTAL_DISTANCE_DE=$TOTAL_DISTANCE
ORDER_VOLUME_DE=$ORDER_VOLUME
ORDER_WEIGHT_DE=$ORDER_WEIGHT
EOF

echo "✅ Configuration saved to: dhis2_config.txt"
echo ""

echo "================================================"
echo "Summary"
echo "================================================"
echo "Program ID:        $PROGRAM_ID"
echo "Route ID:          $ROUTE_ID"
echo "Route Details:     $ROUTE_DETAILS"
echo "Vehicle Type:      $VEHICLE_TYPE"
echo "Total Distance:    $TOTAL_DISTANCE"
echo "Order Volume:      $ORDER_VOLUME"
echo "Order Weight:      $ORDER_WEIGHT"
echo ""
echo "✅ DHIS2 structure created successfully!"
echo ""
echo "Next steps:"
echo "1. Run: ./add_sample_routes.sh to add test route data"
echo "2. Update app code with these IDs"
echo "3. Enable DHIS2 fetching in the app"
