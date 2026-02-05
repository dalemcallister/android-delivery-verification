# Multi-Driver, Multi-Truck Architecture

## Overview
Design for production deployment where multiple drivers use the app, each assigned to specific trucks, and only see routes assigned to their truck.

## System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    DHIS2 System                              │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐  │
│  │   Users    │  │  Vehicles  │  │  Route Assignments   │  │
│  │ (Drivers)  │  │  (Trucks)  │  │  (Truck → Routes)    │  │
│  └────────────┘  └────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                    DRO Service                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Route Optimization                                   │  │
│  │  - Fetches orders & facilities from DHIS2            │  │
│  │  - Optimizes routes considering truck capacity       │  │
│  │  - Assigns optimized routes to trucks                │  │
│  │  - Pushes route assignments back to DHIS2            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│              Android Delivery App (Multiple Drivers)         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Driver Login → Fetch ONLY their truck's routes       │ │
│  │  Complete Deliveries → Sync verifications to DHIS2    │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## Phase 1: User & Truck Management in DHIS2

### 1.1 Driver User Creation

**DHIS2 User Roles:**
- Create a "Delivery Driver" role with permissions:
  - View assigned routes (READ)
  - Create delivery verifications (WRITE events)
  - No admin access
  - No access to other drivers' data

**User Structure:**
```
Driver User Account:
├── Username: driver001 (unique)
├── Password: Set by admin
├── Full Name: John Doe
├── Phone: +254712345678
├── Role: Delivery Driver
└── Custom Attributes:
    ├── Driver ID: DRV-001
    ├── Assigned Truck: TRK-001
    ├── License Number: DL12345
    └── Employment Status: ACTIVE
```

### 1.2 Truck/Vehicle Management

**Option A: DHIS2 Organization Units** (Recommended)
Create trucks as special Organization Units under a "Fleet" parent:

```
Organization Hierarchy:
├── Fleet (Parent)
    ├── TRK-001 (3-ton Truck)
    │   ├── Capacity: 3000 kg, 5000 L
    │   ├── Type: TRUCK
    │   └── Assigned Driver: DRV-001
    ├── TRK-002 (Van)
    │   ├── Capacity: 1000 kg, 1500 L
    │   ├── Type: VAN
    │   └── Assigned Driver: DRV-002
    └── TRK-003 (Motorcycle)
        ├── Capacity: 200 kg, 300 L
        ├── Type: MOTORCYCLE
        └── Assigned Driver: DRV-003
```

**Option B: DHIS2 Tracked Entities**
Create a "Vehicle" program to track trucks as entities.

**Recommendation**: Use Option A (Organization Units) because:
- Routes can be assigned to organization units naturally
- Easier filtering in queries
- Integrates well with DHIS2 permissions

### 1.3 User-Truck Assignment

**Link users to trucks using:**
1. DHIS2 User Attribute: "Assigned Truck" → Store truck OrgUnit ID
2. Or Organization Unit assignment: Assign user to truck OrgUnit

**Implementation:**
```json
{
  "username": "driver001",
  "firstName": "John",
  "surname": "Doe",
  "userCredentials": {
    "username": "driver001",
    "password": "SecurePass123!",
    "userRoles": [{"id": "DRIVER_ROLE_ID"}]
  },
  "organisationUnits": [
    {"id": "TRK-001"}  // Assigned truck
  ],
  "attributeValues": [
    {
      "attribute": {"id": "DRIVER_ID_ATTR"},
      "value": "DRV-001"
    },
    {
      "attribute": {"id": "TRUCK_ASSIGNMENT_ATTR"},
      "value": "TRK-001"
    }
  ]
}
```

---

## Phase 2: Route Assignment in DRO

### 2.1 DRO Enhancement: Truck-Aware Optimization

**Update DRO to:**
1. Fetch truck specifications from DHIS2
2. Consider truck capacity in route optimization
3. Assign each optimized route to a specific truck
4. Push truck-route assignments to DHIS2

**DRO API Enhancement:**
```python
# New endpoint
@app.post("/api/v1/optimize-with-trucks")
async def optimize_routes_for_fleet(
    orders: List[Order],
    trucks: List[Truck]
):
    """
    Optimizes routes considering multiple trucks
    Returns truck-specific routes
    """
    optimized_routes = []

    for truck in trucks:
        # Get orders that fit truck capacity
        suitable_orders = filter_by_capacity(orders, truck)

        # Optimize route for this truck
        route = optimize_route(suitable_orders, truck)
        route.assigned_truck_id = truck.id

        optimized_routes.append(route)

    # Push routes to DHIS2 with truck assignments
    push_routes_to_dhis2(optimized_routes)

    return optimized_routes
```

### 2.2 Route Assignment Data Structure

**In DHIS2, store route with truck assignment:**
```json
{
  "program": "ROUTE_ASSIGNMENT_PROGRAM",
  "orgUnit": "TRK-001",  // ← The assigned truck!
  "eventDate": "2026-02-04",
  "dataValues": [
    {
      "dataElement": "ROUTE_ID",
      "value": "ROUTE-001"
    },
    {
      "dataElement": "ASSIGNED_TRUCK",
      "value": "TRK-001"
    },
    {
      "dataElement": "ASSIGNED_DRIVER",
      "value": "DRV-001"
    },
    {
      "dataElement": "ROUTE_DETAILS",
      "value": "{...route JSON...}"
    }
  ]
}
```

---

## Phase 3: Android App - Driver-Specific Routes

### 3.1 Login Enhancement

**Current**: App logs in as "admin"
**New**: App logs in as specific driver

```kotlin
// LoginViewModel.kt
fun login(username: String, password: String) {
    // Login as driver
    val result = authRepository.login(username, password)

    if (result.isSuccess) {
        // Fetch driver's profile
        val driverProfile = authRepository.getDriverProfile()

        // Store driver info locally
        saveDriverInfo(
            driverId = driverProfile.driverId,
            assignedTruck = driverProfile.assignedTruck
        )
    }
}
```

### 3.2 Fetch Only Driver's Routes

**Update RouteRepository.kt:**
```kotlin
suspend fun fetchRoutesFromRemote(): Result<List<Route>> {
    return try {
        val service = authRepository.getDHIS2Service()
            ?: return Result.failure(Exception("Not logged in"))

        // Get current driver's assigned truck
        val assignedTruck = authRepository.getAssignedTruck()
            ?: return Result.failure(Exception("No truck assigned"))

        Timber.d("Fetching routes for truck: $assignedTruck")

        // Fetch ONLY routes assigned to this truck
        val response = service.getEvents(
            programId = ROUTE_PROGRAM_ID,
            orgUnit = assignedTruck,  // ← Filter by truck!
            ouMode = "SELECTED"  // Only this orgUnit, not descendants
        )

        if (response.isSuccessful) {
            val events = response.body()?.events ?: emptyList()
            Timber.d("Driver sees ${events.size} routes for their truck")

            // Parse and return routes
            val routes = parseRoutesFromEvents(events)

            // Save to local database
            saveRoutesLocally(routes)

            Result.success(routes)
        } else {
            Result.failure(Exception("Failed to fetch routes"))
        }
    } catch (e: Exception) {
        Timber.e(e, "Error fetching routes")
        Result.failure(e)
    }
}
```

### 3.3 Display Driver Info

**Add to RoutesScreen:**
```kotlin
TopAppBar(
    title = {
        Column {
            Text("My Routes")
            Text(
                text = "Driver: ${driverName} | Truck: ${truckId}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
)
```

---

## Phase 4: Verification Assignment

### 4.1 Link Verifications to Driver & Truck

When creating verifications, include driver and truck info:

```kotlin
// VerificationRepository.kt
suspend fun createVerification(...) {
    val driverInfo = authRepository.getDriverInfo()

    val event = EventDto(
        program = VERIFICATION_PROGRAM_ID,
        orgUnit = delivery.facilityId,
        dataValues = listOf(
            // ... existing fields ...
            DataValueDto("VERIFIED_BY_DRIVER", driverInfo.driverId),
            DataValueDto("VERIFIED_BY_TRUCK", driverInfo.assignedTruck)
        )
    )
}
```

---

## Implementation Plan

### Step 1: DHIS2 Setup (1-2 hours)
1. ✅ Create "Delivery Driver" user role
2. ✅ Create driver user accounts
3. ✅ Create truck organization units (Fleet structure)
4. ✅ Create user attributes (Driver ID, Truck Assignment)
5. ✅ Assign users to trucks

### Step 2: DRO Enhancement (2-3 hours)
1. ✅ Fetch truck data from DHIS2
2. ✅ Update optimization to consider truck capacity
3. ✅ Assign routes to specific trucks
4. ✅ Push truck-specific routes to DHIS2

### Step 3: Android App Updates (2-3 hours)
1. ✅ Update login to use driver credentials
2. ✅ Fetch driver profile after login
3. ✅ Filter routes by assigned truck
4. ✅ Display driver/truck info in UI
5. ✅ Include driver/truck in verifications

### Step 4: Testing (1 hour)
1. ✅ Create 2-3 test drivers
2. ✅ Assign to different trucks
3. ✅ Create routes for each truck
4. ✅ Login as each driver
5. ✅ Verify they only see their routes

---

## Security Considerations

### Access Control
- ✅ Drivers can only see their assigned truck's routes
- ✅ Drivers cannot see other drivers' routes
- ✅ Drivers cannot modify route assignments
- ✅ Drivers can only create verifications for their deliveries
- ✅ Admin users can see all routes

### Data Privacy
- ✅ Driver phone numbers encrypted
- ✅ Passwords hashed (DHIS2 handles this)
- ✅ Verification photos/signatures only accessible by admin

---

## Database Schema Updates

### Android Local Database - Add Driver Info

```kotlin
// Add to RouteEntity
data class RouteEntity(
    // ... existing fields ...
    val assignedTruck: String,
    val assignedDriver: String?
)

// Add to VerificationEntity
data class VerificationEntity(
    // ... existing fields ...
    val verifiedByDriver: String,
    val verifiedByTruck: String
)
```

### DataStore - Store Driver Profile

```kotlin
// AuthRepository.kt
data class DriverProfile(
    val driverId: String,
    val driverName: String,
    val assignedTruck: String,
    val truckType: String
)

suspend fun saveDriverProfile(profile: DriverProfile)
suspend fun getDriverProfile(): DriverProfile?
```

---

## API Endpoints Needed

### DHIS2 API Calls (Android App)
```
1. POST /api/auth - Driver login
2. GET /api/me?fields=* - Get driver profile
3. GET /api/userAttributes - Get driver attributes
4. GET /api/events?program=ROUTE_PROG&orgUnit=TRUCK_ID - Get truck routes
5. POST /api/events - Create verifications
```

### DRO API Endpoints (New/Updated)
```
1. GET /api/v1/trucks - Get all trucks from DHIS2
2. POST /api/v1/optimize-with-trucks - Optimize routes for fleet
3. POST /api/v1/routes/assign-to-truck - Assign route to truck
4. POST /api/v1/routes/push-to-dhis2 - Push truck assignments to DHIS2
```

---

## Next Steps - Decision Point

**Option A: Implement Complete Multi-User System** (Recommended)
- Full production-ready architecture
- Proper user/truck management
- Secure driver-specific route access
- Takes 6-8 hours to implement fully

**Option B: Simplified Multi-User** (Faster)
- Create 2-3 test drivers
- Hardcode truck assignments
- Basic filtering in app
- Takes 2-3 hours

**Option C: Keep Single-User for Now**
- Continue with current setup
- Add multi-user later when needed
- Focus on perfecting delivery workflow first

---

## My Recommendation

**Start with Option B (Simplified Multi-User)** to validate the concept:

1. Create 2-3 test drivers in DHIS2
2. Create 2-3 trucks as orgUnits
3. Assign drivers to trucks manually
4. Update app to filter by truck
5. Test with multiple driver logins

Then once proven, scale to Option A for production.

**What do you think?**

