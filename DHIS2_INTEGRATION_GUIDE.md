

# DHIS2 Integration - Complete Guide

## ✅ What We've Set Up

### DHIS2 Structure Created:
```
✅ Program: Route Assignment Program (VAXQ9BEVGGw)
✅ Program Stage: Route Assignment (rlnN0IoAatus)
✅ Data Elements:
   - Route ID (BQeA274HyeH)
   - Route Details JSON (bBa7bxDWhxg)
   - Vehicle Type (nyuVA4V87bh)
   - Total Distance (amFQACwoYth)
   - Order Volume (cNfWOj9OlyR) - existing
   - Order Weight (MtydVLMZaEN) - existing
```

## 📱 Update App to Use DHIS2 Data

### Step 1: Update RouteRepository.kt

Replace the placeholder IDs (around line 60-63):

```kotlin
// Data element UIDs for route information
val ROUTE_ID_DE = "BQeA274HyeH"  // ← Update this
val ROUTE_DETAILS_DE = "bBa7bxDWhxg"  // ← Update this
val ROUTE_STATUS_DE = "pYzQ3Wm8Ktx"  // ← Keep or remove
val VEHICLE_TYPE_DE = "nyuVA4V87bh"  // ← Update this
```

### Step 2: Update to Use Events API Instead

The current app tries to fetch from dataValueSets, but we created Events. Let's update the fetching logic:

**File:** `RouteRepository.kt`

Replace the `fetchRoutesFromRemote()` function with this simpler version:

```kotlin
suspend fun fetchRoutesFromRemote(): Result<List<Route>> {
    return try {
        val service = authRepository.getDHIS2Service()
            ?: return Result.failure(Exception("Not logged in"))

        // Fetch events from Route Assignment Program
        val programId = "VAXQ9BEVGGw"  // Route Assignment Program

        val response = service.getEvents(
            programId = programId,
            ouMode = "ACCESSIBLE"
        )

        if (response.isSuccessful) {
            val events = response.body()?.events ?: emptyList()
            Timber.d("Fetched ${events.size} route events")

            val routes = events.mapNotNull { event ->
                try {
                    // Get data values from event
                    val dataValuesMap = event.dataValues.associate {
                        it.dataElement to it.value
                    }

                    // Extract route details JSON
                    val routeDetailsJson = dataValuesMap["bBa7bxDWhxg"] ?: return@mapNotNull null
                    val routeData = gson.fromJson(routeDetailsJson, RouteData::class.java)

                    // Create route
                    val routeId = routeData.routeId
                    Route(
                        id = routeId,
                        routeId = routeId,
                        vehicleType = routeData.vehicleType,
                        totalStops = routeData.stops.size,
                        totalDistance = routeData.totalDistance,
                        totalVolume = routeData.totalVolume,
                        totalWeight = routeData.totalWeight,
                        status = RouteStatus.PENDING,
                        syncStatus = SyncStatus.SYNCED,
                        createdAt = System.currentTimeMillis(),
                        deliveries = routeData.stops.map { stop ->
                            Delivery(
                                id = UUID.randomUUID().toString(),
                                routeId = routeId,
                                facilityId = stop.facilityId,
                                facilityName = stop.facilityName,
                                latitude = stop.latitude,
                                longitude = stop.longitude,
                                orderVolume = stop.orderVolume,
                                orderWeight = stop.orderWeight,
                                stopNumber = stop.stopNumber,
                                distanceFromPrevious = stop.distanceFromPrevious,
                                status = DeliveryStatus.PENDING,
                                syncStatus = SyncStatus.SYNCED
                            )
                        }
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse route from event")
                    null
                }
            }

            // Save to local database
            routes.forEach { route ->
                routeDao.insertRoute(route.toEntity())
                deliveryDao.insertDeliveries(route.deliveries.map { it.toEntity() })
            }

            Timber.d("Fetched ${routes.size} routes from DHIS2")
            Result.success(routes)
        } else {
            val error = "Failed to fetch routes: ${response.code()} ${response.message()}"
            Timber.e(error)
            Result.failure(Exception(error))
        }
    } catch (e: Exception) {
        Timber.e(e, "Error fetching routes")
        Result.failure(e)
    }
}
```

### Step 3: Enable DHIS2 Fetching in RoutesViewModel

**File:** `ui/routes/RoutesViewModel.kt`

Uncomment the real fetching code (around line 53-70):

```kotlin
fun refreshRoutes() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        // NOW USING REAL DHIS2 DATA!
        val result = fetchRoutesUseCase.fetchFromRemote()

        if (result.isSuccess) {
            Timber.d("Routes refreshed successfully from DHIS2")
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            Timber.e("Failed to refresh routes: ${result.exceptionOrNull()?.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message ?: "Failed to fetch routes"
            )
        }
    }
}
```

### Step 4: Add Sample Routes to DHIS2

We need to manually add route data since the scripts had issues. Use the DHIS2 API directly:

```bash
# Add this route data through DHIS2 Data Entry or API
# Or we can use the app to create mock data that gets synced
```

## 🚀 Simplified Approach: Use DRO Integration

Instead of manually creating routes in DHIS2, integrate with your DRO service:

1. **DRO API** generates optimized routes
2. **DRO pushes** routes to DHIS2 as events
3. **Android app fetches** routes from DHIS2
4. **App syncs** verifications back to DHIS2

This is the production workflow mentioned in the original plan.

## 🧪 For Now: Test with Mock + DHIS2 Sync

Current working setup:
- ✅ Mock routes in app (local)
- ✅ Complete deliveries
- ✅ Sync verifications to DHIS2 ← This works!
- ⏳ Fetch routes from DHIS2 ← Can enable after adding route data

## Next Steps

**Option A: Keep Using Mock Data (Recommended for Now)**
- App works perfectly with mock data
- Focus on testing delivery verification
- Sync verifications to DHIS2 (this works!)
- Add DHIS2 route fetching later

**Option B: Add Routes to DHIS2 Manually**
- Use DHIS2 Data Entry app
- Add events to "Route Assignment Program"
- Include route details as JSON
- App fetches on refresh

**Option C: Full DRO Integration**
- Extend DRO API to push routes to DHIS2
- App fetches routes from DHIS2
- Complete end-to-end workflow

## Recommendation

✅ **Stick with mock data for now** and focus on:
1. Testing delivery verification workflow
2. Testing GPS validation
3. Testing offline mode
4. Testing verification sync to DHIS2

Then later:
5. Integrate DRO → DHIS2 route pushing
6. Enable app → DHIS2 route fetching

The verification sync (delivery data → DHIS2) is the most important part and will work with either approach!

