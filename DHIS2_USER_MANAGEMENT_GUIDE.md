# Using DHIS2 Native User Management for Multi-Driver System

## Overview
Leverage DHIS2's built-in user management, roles, organization units, and attributes instead of building a custom system.

---

## DHIS2 Features We'll Use

### ✅ Built-in Capabilities:
1. **User Management** - Create driver accounts
2. **User Roles** - Define driver permissions
3. **User Groups** - Group drivers (e.g., by region)
4. **Organization Units** - Represent trucks/vehicles
5. **User Attributes** - Store driver metadata (license, truck ID)
6. **Data Sharing** - Control what each driver can see
7. **Authentication** - DHIS2 handles login/sessions

---

## Implementation Using DHIS2 Native Features

### Step 1: Create Organization Unit Structure for Fleet

**Organization Hierarchy:**
```
Nigeria (Root)
├── Kaduna State
│   └── Kaduna Central Depot
│       └── Fleet Management
│           ├── TRK-001 (3-ton Truck)
│           ├── TRK-002 (Van)
│           └── TRK-003 (Motorcycle)
```

**Why Organization Units for Trucks?**
- ✅ DHIS2's natural way to organize entities
- ✅ Users can be assigned to orgUnits
- ✅ Events/programs can be filtered by orgUnit
- ✅ Built-in hierarchical permissions
- ✅ Reporting and analytics work automatically

**Create via DHIS2 API:**
```bash
# Create Fleet parent orgUnit
curl -X POST "http://192.168.88.9:8080/api/organisationUnits" \
  -u admin:district \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Fleet Management",
    "shortName": "Fleet",
    "openingDate": "2026-01-01",
    "parent": {"id": "wjDCrWdYslY"}  // Kaduna Central Depot
  }'

# Create Truck 1
curl -X POST "http://192.168.88.9:8080/api/organisationUnits" \
  -u admin:district \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Truck TRK-001 (3-ton)",
    "shortName": "TRK-001",
    "code": "TRK-001",
    "openingDate": "2026-01-01",
    "parent": {"id": "FLEET_PARENT_ID"},
    "attributeValues": [
      {"attribute": {"id": "VEHICLE_TYPE_ATTR"}, "value": "TRUCK"},
      {"attribute": {"id": "CAPACITY_KG_ATTR"}, "value": "3000"},
      {"attribute": {"id": "CAPACITY_L_ATTR"}, "value": "5000"}
    ]
  }'
```

---

### Step 2: Create User Roles for Drivers

**DHIS2 User Role: "Delivery Driver"**

Permissions:
- ✅ View routes (Events in Route Program)
- ✅ Create delivery verifications (Events in Verification Program)
- ✅ View assigned organization units only
- ❌ No admin access
- ❌ Cannot view other drivers' data
- ❌ Cannot modify routes

**Create via DHIS2 Web UI:**
1. Go to: **Users → User Roles → Add**
2. Name: "Delivery Driver"
3. Authorities:
   - `F_TRACKED_ENTITY_INSTANCE_SEARCH`
   - `F_TRACKED_ENTITY_INSTANCE_ADD`
   - `F_PROGRAM_EVENT_ADD`
   - `F_PROGRAM_EVENT_UPDATE`
   - `F_VIEW_EVENT_ANALYTICS`

**Or via API:**
```bash
curl -X POST "http://192.168.88.9:8080/api/userRoles" \
  -u admin:district \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Delivery Driver",
    "description": "Role for delivery drivers using mobile app",
    "authorities": [
      "F_TRACKED_ENTITY_INSTANCE_SEARCH",
      "F_PROGRAM_EVENT_ADD",
      "F_PROGRAM_EVENT_UPDATE",
      "F_VIEW_EVENT_ANALYTICS",
      "M_dhis-web-dashboard"
    ]
  }'
```

---

### Step 3: Create User Attributes for Driver Metadata

**DHIS2 User Attributes:**

1. **Driver ID** (e.g., DRV-001)
2. **License Number** (e.g., DL123456)
3. **Phone Number** (e.g., +254712345678)
4. **Assigned Truck** (e.g., TRK-001)
5. **Employment Status** (ACTIVE/INACTIVE)

**Create via API:**
```bash
# Driver ID Attribute
curl -X POST "http://192.168.88.9:8080/api/attributes" \
  -u admin:district \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Driver ID",
    "shortName": "Driver ID",
    "code": "DRIVER_ID",
    "valueType": "TEXT",
    "userAttribute": true,
    "unique": true,
    "mandatory": true
  }'

# Assigned Truck Attribute
curl -X POST "http://192.168.88.9:8080/api/attributes" \
  -u admin:district \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Assigned Truck",
    "shortName": "Truck",
    "code": "ASSIGNED_TRUCK",
    "valueType": "TEXT",
    "userAttribute": true,
    "mandatory": true
  }'

# License Number Attribute
curl -X POST "http://192.168.88.9:8080/api/attributes" \
  -u admin:district \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Driver License Number",
    "shortName": "License",
    "code": "LICENSE_NUMBER",
    "valueType": "TEXT",
    "userAttribute": true
  }'
```

---

### Step 4: Create Driver User Accounts

**Driver User Structure:**
```
Username: driver001
Password: SecurePass123! (change on first login)
First Name: John
Surname: Doe
Email: john.doe@example.com
Phone: +254712345678

User Roles:
├── Delivery Driver (created above)

Organization Units (Data Capture):
├── TRK-001 (The truck assigned to this driver)

Organization Units (Data View):
├── TRK-001 (Can only view data from their truck)

User Attributes:
├── Driver ID: DRV-001
├── Assigned Truck: TRK-001
└── License Number: DL123456
```

**Create via API:**
```bash
curl -X POST "http://192.168.88.9:8080/api/users" \
  -u admin:district \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "surname": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+254712345678",
    "userCredentials": {
      "username": "driver001",
      "password": "SecurePass123!",
      "userRoles": [
        {"id": "DRIVER_ROLE_ID"}
      ]
    },
    "organisationUnits": [
      {"id": "TRK-001-ORGUNIT-ID"}
    ],
    "dataViewOrganisationUnits": [
      {"id": "TRK-001-ORGUNIT-ID"}
    ],
    "attributeValues": [
      {
        "attribute": {"id": "DRIVER_ID_ATTR_ID"},
        "value": "DRV-001"
      },
      {
        "attribute": {"id": "ASSIGNED_TRUCK_ATTR_ID"},
        "value": "TRK-001"
      },
      {
        "attribute": {"id": "LICENSE_ATTR_ID"},
        "value": "DL123456"
      }
    ]
  }'
```

---

### Step 5: Assign Routes to Trucks (Organization Units)

When DRO creates optimized routes, push to DHIS2 with truck orgUnit:

```json
{
  "program": "ROUTE_ASSIGNMENT_PROGRAM_ID",
  "orgUnit": "TRK-001-ORGUNIT-ID",  // ← Truck as orgUnit!
  "eventDate": "2026-02-04",
  "status": "ACTIVE",
  "dataValues": [
    {
      "dataElement": "ROUTE_ID_DE",
      "value": "ROUTE-001"
    },
    {
      "dataElement": "ROUTE_DETAILS_DE",
      "value": "{...JSON with stops...}"
    },
    {
      "dataElement": "ASSIGNED_DRIVER_DE",
      "value": "DRV-001"
    }
  ]
}
```

**Key Point:** By assigning the event to the truck's orgUnit, DHIS2's built-in security automatically ensures only users assigned to that orgUnit can see it!

---

### Step 6: Android App - Use DHIS2 User Session

**On Login:**
```kotlin
// AuthRepository.kt
suspend fun login(username: String, password: String): Result<DriverProfile> {
    return try {
        // Create DHIS2 client with user credentials
        val service = DHIS2Client.create(serverUrl, username, password)

        // Test authentication and get user info
        val userResponse = service.getCurrentUser()

        if (userResponse.isSuccessful) {
            val user = userResponse.body()

            // Extract driver info from DHIS2 user object
            val driverProfile = DriverProfile(
                driverId = user.getAttribute("DRIVER_ID"),
                driverName = "${user.firstName} ${user.surname}",
                assignedTruck = user.getAttribute("ASSIGNED_TRUCK"),
                truckOrgUnitId = user.organisationUnits.first().id
            )

            // Save profile and session
            saveDriverProfile(driverProfile)
            saveCredentials(username, password)

            Result.success(driverProfile)
        } else {
            Result.failure(Exception("Login failed"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Fetch Driver's Routes:**
```kotlin
suspend fun fetchRoutesFromRemote(): Result<List<Route>> {
    // Get driver's assigned truck orgUnit
    val driverProfile = getDriverProfile()
    val truckOrgUnitId = driverProfile.truckOrgUnitId

    // Fetch events for this orgUnit only
    // DHIS2 automatically filters based on user permissions!
    val response = service.getEvents(
        programId = ROUTE_PROGRAM_ID,
        orgUnit = truckOrgUnitId,
        ouMode = "SELECTED"  // Only this specific orgUnit
    )

    // Parse routes...
}
```

**DHIS2 API Endpoint Needed:**
```kotlin
// Add to DHIS2Service.kt
@GET("api/me")
suspend fun getCurrentUser(
    @Query("fields") fields: String = "id,username,firstName,surname,email,phoneNumber,organisationUnits[id,name],userCredentials[userRoles],attributeValues[attribute[id,code],value]"
): Response<UserDto>
```

**UserDto:**
```kotlin
data class UserDto(
    val id: String,
    val username: String,
    val firstName: String,
    val surname: String,
    val email: String?,
    val phoneNumber: String?,
    val organisationUnits: List<OrgUnitDto>,
    val attributeValues: List<AttributeValueDto>
) {
    fun getAttribute(code: String): String? {
        return attributeValues
            .find { it.attribute.code == code }
            ?.value
    }
}
```

---

## Complete Implementation Scripts

### Script 1: Setup Fleet Structure
```bash
#!/bin/bash
# setup_fleet_structure.sh

DHIS2_URL="http://192.168.88.9:8080"
AUTH="admin:district"

echo "Creating Fleet Organization Structure..."

# Get or create Fleet parent
FLEET_ID=$(curl -s "$DHIS2_URL/api/organisationUnits?filter=name:eq:Fleet%20Management&fields=id" -u "$AUTH" | jq -r '.organisationUnits[0].id // empty')

if [ -z "$FLEET_ID" ]; then
  echo "Creating Fleet Management orgUnit..."
  FLEET_ID=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
    -u "$AUTH" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "Fleet Management",
      "shortName": "Fleet",
      "openingDate": "2026-01-01",
      "parent": {"id": "wjDCrWdYslY"}
    }' | jq -r '.response.uid')
  echo "✅ Fleet created: $FLEET_ID"
fi

# Create trucks
declare -a TRUCKS=(
  "TRK-001:TRUCK:3000:5000"
  "TRK-002:VAN:1000:1500"
  "TRK-003:MOTORCYCLE:200:300"
)

for truck_data in "${TRUCKS[@]}"; do
  IFS=':' read -r CODE TYPE CAP_KG CAP_L <<< "$truck_data"

  echo "Creating $CODE..."
  TRUCK_ID=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
    -u "$AUTH" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "Vehicle '"$CODE"' ('"$TYPE"')",
      "shortName": "'"$CODE"'",
      "code": "'"$CODE"'",
      "openingDate": "2026-01-01",
      "parent": {"id": "'"$FLEET_ID"'"}
    }' | jq -r '.response.uid // empty')

  if [ -n "$TRUCK_ID" ]; then
    echo "✅ Created $CODE: $TRUCK_ID"
    echo "$CODE=$TRUCK_ID" >> fleet_ids.txt
  fi
done

echo ""
echo "✅ Fleet structure created!"
echo "Truck IDs saved to: fleet_ids.txt"
```

### Script 2: Create Driver Users
```bash
#!/bin/bash
# create_driver_users.sh

DHIS2_URL="http://192.168.88.9:8080"
AUTH="admin:district"

# Get driver role ID
DRIVER_ROLE_ID=$(curl -s "$DHIS2_URL/api/userRoles?filter=name:eq:Delivery%20Driver&fields=id" -u "$AUTH" | jq -r '.userRoles[0].id')

# Load truck IDs
source fleet_ids.txt

# Create drivers
declare -a DRIVERS=(
  "driver001:John:Doe:TRK-001:DRV-001:DL123456:+254712345001"
  "driver002:Jane:Smith:TRK-002:DRV-002:DL123457:+254712345002"
  "driver003:Mike:Johnson:TRK-003:DRV-003:DL123458:+254712345003"
)

for driver_data in "${DRIVERS[@]}"; do
  IFS=':' read -r USERNAME FIRST LAST TRUCK DRVID LICENSE PHONE <<< "$driver_data"

  # Get truck orgUnit ID
  TRUCK_VAR="${TRUCK//-/_}"
  TRUCK_ID="${!TRUCK_VAR}"

  echo "Creating user: $USERNAME (assigned to $TRUCK)..."

  curl -s -X POST "$DHIS2_URL/api/users" \
    -u "$AUTH" \
    -H "Content-Type: application/json" \
    -d '{
      "firstName": "'"$FIRST"'",
      "surname": "'"$LAST"'",
      "email": "'"$USERNAME@example.com"'",
      "phoneNumber": "'"$PHONE"'",
      "userCredentials": {
        "username": "'"$USERNAME"'",
        "password": "Driver123!",
        "userRoles": [{"id": "'"$DRIVER_ROLE_ID"'"}]
      },
      "organisationUnits": [{"id": "'"$TRUCK_ID"'"}],
      "dataViewOrganisationUnits": [{"id": "'"$TRUCK_ID"'"}]
    }' | jq '.status'

  echo "✅ Created $USERNAME"
done

echo ""
echo "✅ All drivers created!"
echo "Login credentials:"
echo "  Username: driver001, Password: Driver123!"
echo "  Username: driver002, Password: Driver123!"
echo "  Username: driver003, Password: Driver123!"
```

---

## Testing the Multi-User Setup

### Test Plan:

1. **Create fleet structure** (run script 1)
2. **Create driver users** (run script 2)
3. **Assign routes to trucks** (via DRO or manually)
4. **Login as driver001** in Android app
5. **Verify**: Only sees routes for TRK-001
6. **Login as driver002** in Android app
7. **Verify**: Only sees routes for TRK-002
8. **Complete delivery as driver001**
9. **Verify**: Verification linked to driver001 and TRK-001

---

## Benefits of Using DHIS2 Native Features

✅ **Built-in Security**: DHIS2 handles all permissions automatically
✅ **No Custom Code**: Use existing DHIS2 user management
✅ **Scalable**: Can add hundreds of drivers easily
✅ **Auditable**: DHIS2 tracks who did what
✅ **Integrated**: Works with all DHIS2 features (analytics, reports)
✅ **Mobile Support**: DHIS2 Android SDK available if needed
✅ **Industry Standard**: Healthcare systems already use this pattern

---

## Next Steps

1. **Run setup scripts** to create fleet structure and drivers
2. **Test login** with driver credentials in the app
3. **Update app** to fetch current user info
4. **Filter routes** by driver's assigned truck
5. **Test** with multiple driver accounts

Would you like me to run the scripts now to set this up in your DHIS2?

