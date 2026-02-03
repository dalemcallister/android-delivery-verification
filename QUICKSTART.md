# Quick Start Guide

## 5-Minute Setup

### Prerequisites
- ✅ Android Studio installed
- ✅ DHIS2 running on localhost:8080 (or remote server)
- ✅ Android emulator or physical device

### Step 1: Open Project (1 min)
```bash
# Open Android Studio
# File → Open → Select: /Users/dalemcallister/Desktop/connexidevepod
```

### Step 2: Sync & Build (2 min)
1. Wait for Gradle sync to complete
2. If prompted, accept SDK licenses
3. Click "Build" → "Make Project"

### Step 3: Run App (2 min)
1. Select emulator or device
2. Click Run (▶️) button
3. Wait for app to launch

### Step 4: Login
- **Server URL**: `http://10.0.2.2:8080` (emulator) or `http://YOUR_IP:8080` (device)
- **Username**: `admin`
- **Password**: `district`
- Check "Remember credentials"
- Tap "Login"

## Testing Without DHIS2 Setup

The app will work locally with an empty database. To test with data:

### Option A: Mock Data (Fastest)
Add this to `RoutesViewModel.kt` init block:
```kotlin
init {
    loadLocalRoutes()
    // loadMockData() // Uncomment this line
}

private fun loadMockData() {
    viewModelScope.launch {
        val mockRoute = Route(
            id = "route-1",
            routeId = "ROUTE-001",
            vehicleType = "TRUCK",
            totalStops = 5,
            totalDistance = 25000f,
            totalVolume = 500f,
            totalWeight = 2000f,
            status = RouteStatus.PENDING,
            syncStatus = SyncStatus.SYNCED,
            createdAt = System.currentTimeMillis(),
            deliveries = listOf(
                Delivery(
                    id = "del-1",
                    routeId = "route-1",
                    facilityId = "facility-1",
                    facilityName = "Health Center A",
                    latitude = -1.2921,
                    longitude = 36.8219,
                    orderVolume = 100f,
                    orderWeight = 400f,
                    stopNumber = 1,
                    distanceFromPrevious = 0f,
                    status = DeliveryStatus.PENDING,
                    syncStatus = SyncStatus.SYNCED
                )
            )
        )
        // Insert mock route to database
    }
}
```

### Option B: Configure DHIS2 (Full Setup)
See [DHIS2_SETUP.md](DHIS2_SETUP.md) for complete instructions.

## Emulator GPS Setup

1. Open emulator Extended Controls (⋮ icon)
2. Go to "Location" tab
3. Enter coordinates:
   - **Latitude**: `-1.2921`
   - **Longitude**: `36.8219`
4. Click "Send"

Now the app will detect your location!

## Common Issues

### "Login failed"
- ✅ Check DHIS2 is running: `curl http://localhost:8080/api/system/info`
- ✅ Use `10.0.2.2` for emulator (not `localhost`)
- ✅ Verify credentials: admin/district

### "No routes available"
- ✅ Routes need to be created in DHIS2 first
- ✅ Use mock data option above for testing
- ✅ Check server logs for errors

### GPS not working
- ✅ Grant location permissions when prompted
- ✅ Set GPS coordinates in emulator (see above)
- ✅ For device, enable location services

### Build errors
- ✅ File → Invalidate Caches → Restart
- ✅ Delete `.gradle` folder and sync again
- ✅ Check internet connection (downloads dependencies)

## Next Steps

1. ✅ Test login flow
2. ✅ Add mock data or configure DHIS2
3. ✅ Test delivery verification with GPS
4. ✅ Test offline mode (disable wifi)
5. ✅ Test sync functionality

## Development Workflow

```
1. Make code changes
2. Build → Make Project (Ctrl+F9)
3. Run app (Shift+F10)
4. Test feature
5. Check logs: View → Tool Windows → Logcat
```

## Useful Commands

```bash
# View logs
adb logcat | grep "DeliveryApp"

# Clear app data
adb shell pm clear com.connexi.deliveryverification

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Take screenshot
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png
```

## Project Structure (Key Files)

```
📁 Core Entry Points
├── DeliveryApp.kt          # Application class
├── MainActivity.kt         # Main activity
└── AppNavigation.kt        # Screen navigation

📁 UI Screens
├── LoginScreen.kt          # Login
├── RoutesScreen.kt         # Routes list
├── RouteDetailScreen.kt    # Route details
├── DeliveryVerificationScreen.kt  # Main verification
└── SyncScreen.kt           # Sync status

📁 Data Layer
├── AppDatabase.kt          # Room database
├── RouteRepository.kt      # Route data
├── DeliveryRepository.kt   # Delivery data
└── VerificationRepository.kt  # Verification data

📁 DHIS2 Integration
├── DHIS2Service.kt         # API interface
├── DHIS2Client.kt          # API client
└── AuthRepository.kt       # Authentication
```

## Getting Help

- 📖 Full documentation: [README.md](README.md)
- 🔧 Implementation details: [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
- 🐛 Check logs: Android Studio → Logcat → Filter: "DeliveryApp"

## Success Criteria

After setup, you should be able to:
- ✅ Login with DHIS2 credentials
- ✅ See routes list (or empty state)
- ✅ Navigate to route details
- ✅ Open delivery verification screen
- ✅ See GPS status indicator
- ✅ Complete a delivery
- ✅ View sync screen

If all above work, setup is complete! 🎉
