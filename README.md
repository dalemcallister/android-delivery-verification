# Android Delivery Verification App

A native Android application built with Kotlin and Jetpack Compose that enables delivery drivers to verify deliveries via GPS, confirm quantities, capture signatures/comments, and sync with DHIS2.

## Features

- **GPS-based Delivery Verification**: Validates driver location within 100m of delivery target with <50m accuracy
- **Offline-First Architecture**: Works offline and syncs when connectivity is restored
- **DHIS2 Integration**: Fetches routes and uploads verification data to DHIS2
- **Route Management**: View assigned routes and delivery stops with progress tracking
- **Delivery Verification**:
  - Real-time GPS validation
  - Quantity confirmation (volume & weight)
  - Comments and notes
  - Signature capture (planned)
  - Photo capture (planned)
- **Background Sync**: Automatic synchronization of pending verifications

## Technology Stack

### Core
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material3
- **Architecture**: Clean Architecture (Presentation → Domain → Data)
- **Dependency Injection**: Hilt

### Data Layer
- **Local Database**: Room (SQLite)
- **Remote API**: Retrofit + OkHttp
- **Data Flow**: Kotlin Coroutines + Flow

### Services
- **Location**: Google Play Services FusedLocationProviderClient
- **Background Work**: WorkManager
- **Network Monitoring**: ConnectivityManager

## Architecture

```
┌─────────────────────────────────────┐
│    Presentation Layer (UI)          │
│  Jetpack Compose + ViewModels       │
├─────────────────────────────────────┤
│    Domain Layer                     │
│  Use Cases + Business Logic         │
├─────────────────────────────────────┤
│    Data Layer                       │
│  ┌────────────┬──────────────────┐  │
│  │ Room DB    │  DHIS2 API       │  │
│  │ (Local)    │  (Retrofit)      │  │
│  └────────────┴──────────────────┘  │
└─────────────────────────────────────┘
```

## Database Schema

### Tables
- **routes**: Route information (vehicle, stops, distance, status)
- **deliveries**: Individual delivery stops with coordinates and order details
- **verifications**: Completed verification records with GPS data, signatures, photos

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 26+ (Android 8.0)
- DHIS2 instance (running on localhost:8080 or remote server)

### Configuration

1. **Clone and Open Project**
   ```bash
   cd /Users/dalemcallister/Desktop/connexidevepod
   ```
   Open in Android Studio

2. **Configure Server URLs**

   Edit `app/build.gradle.kts` to set your server URLs:
   ```kotlin
   buildConfigField("String", "DEFAULT_DHIS2_URL", "\"http://10.0.2.2:8080\"")
   buildConfigField("String", "DEFAULT_DRO_URL", "\"http://10.0.2.2:8000\"")
   ```

   Note: `10.0.2.2` is the Android emulator's host machine. For physical devices, use your computer's IP address.

3. **DHIS2 Setup**

   Create the following programs in DHIS2:

   **Program 1: Route Assignment Program**
   - Type: EVENT (WITHOUT_REGISTRATION)
   - Purpose: Store optimized routes from DRO service
   - Data Elements:
     - Route ID (TEXT)
     - Vehicle Type (TEXT)
     - Stops JSON (LONG_TEXT)
     - Total Distance (NUMBER)
     - Total Volume (NUMBER)
     - Total Weight (NUMBER)

   **Program 2: Delivery Verification Program**
   - Type: TRACKER or EVENT
   - Program Stage: Delivery Verification
   - Data Elements:
     - Order Volume (NUMBER) - Use existing: `cNfWOj9OlyR`
     - Order Weight (NUMBER) - Use existing: `MtydVLMZaEN`
     - Actual Volume (NUMBER)
     - Actual Weight (NUMBER)
     - GPS Latitude (NUMBER)
     - GPS Longitude (NUMBER)
     - GPS Accuracy (NUMBER)
     - Distance from Target (NUMBER)
     - Comments (LONG_TEXT)
     - Signature (LONG_TEXT - base64)
     - Photo (LONG_TEXT - base64)
     - Verification Timestamp (DATE)

4. **Update Program IDs**

   Edit the following files with your DHIS2 program/data element IDs:

   - `RouteRepository.kt` line 62: Replace `"ROUTE_PROGRAM_ID"`
   - `VerificationRepository.kt` line 116-117: Replace program and stage IDs
   - `VerificationRepository.kt` line 149-161: Replace data element IDs

### Build and Run

1. **Sync Gradle**
   ```
   ./gradlew clean build
   ```

2. **Run on Emulator**
   - Create an Android emulator (API 26+)
   - Enable location in emulator settings
   - Run the app from Android Studio

3. **Run on Physical Device**
   - Enable Developer Options and USB Debugging
   - Connect device via USB
   - Run the app from Android Studio

## Usage

### Login
1. Launch the app
2. Enter DHIS2 credentials (default: admin/district)
3. Server URL (default: http://10.0.2.2:8080)
4. Check "Remember credentials" to save login
5. Tap "Login"

### Fetch Routes
1. From Routes screen, tap refresh icon
2. Routes will be fetched from DHIS2 and stored locally
3. View route summary (stops, distance, progress)

### Complete Delivery
1. Select a route from Routes screen
2. View list of delivery stops
3. Tap on a delivery stop
4. Wait for GPS to lock (green indicator)
5. Confirm/adjust quantities
6. Add comments if needed
7. Tap "Complete Delivery"
8. Verification is saved locally

### Sync Verifications
1. From Routes screen, tap sync icon
2. View pending verifications
3. Tap "Sync Now" to upload to DHIS2
4. Synced verifications will be marked as completed

## Testing

### Emulator GPS Testing
1. Open emulator Extended Controls (⋮ icon)
2. Go to Location tab
3. Enter GPS coordinates manually or use GPX/KML file
4. Set location to match a delivery facility's coordinates

### Offline Testing
1. Disable network: `adb shell svc wifi disable`
2. Complete deliveries offline
3. Re-enable network: `adb shell svc wifi enable`
4. Trigger sync to upload pending verifications

## Integration with DRO Service

To integrate with the existing DRO route optimization service:

1. **Add DHIS2 Push Endpoint to DRO API**
   ```python
   # Add to DRO API service
   @app.post("/api/v1/routes/push-to-dhis2")
   async def push_routes_to_dhis2(routes: List[OptimizedRoute]):
       # Convert routes to DHIS2 events
       # Push to DHIS2 via dhis2_client
       pass
   ```

2. **Modify Route Fetching**
   - Android app queries DHIS2 Events API
   - Parses route data from event dataValues
   - Stores locally in Room database

## Project Structure

```
app/src/main/kotlin/com/connexi/deliveryverification/
├── data/
│   ├── local/
│   │   ├── entities/         # Room entities
│   │   ├── dao/              # Data access objects
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   ├── dto/              # API DTOs
│   │   ├── DHIS2Service.kt   # Retrofit interface
│   │   └── DHIS2Client.kt    # API client
│   └── repository/           # Repositories
├── domain/
│   ├── model/                # Domain models
│   └── usecase/              # Use cases
├── ui/
│   ├── theme/                # Material3 theme
│   ├── navigation/           # Navigation
│   ├── login/                # Login screen
│   ├── routes/               # Routes list screen
│   ├── route_detail/         # Route detail screen
│   ├── delivery/             # Delivery verification screen
│   └── sync/                 # Sync screen
├── service/
│   └── LocationService.kt    # GPS location service
├── worker/
│   └── SyncWorker.kt         # Background sync
├── util/                     # Utilities
└── di/                       # Hilt modules
```

## Known Issues & TODOs

- [ ] Replace placeholder DHIS2 program/data element IDs
- [ ] Implement signature capture canvas
- [ ] Implement photo capture with CameraX
- [ ] Add map view for route visualization
- [ ] Implement image compression before upload
- [ ] Add unit tests for repositories and use cases
- [ ] Add UI tests for critical flows
- [ ] Handle DHIS2 sync conflicts
- [ ] Add retry mechanism for failed uploads
- [ ] Implement route navigation to next stop
- [ ] Add delivery status filters

## Troubleshooting

### GPS Not Working
- Ensure location permissions are granted
- Check that location services are enabled
- For emulator, manually set GPS coordinates

### Login Failed
- Verify DHIS2 is running and accessible
- Check server URL (use 10.0.2.2 for emulator)
- Verify credentials
- Check network connectivity

### Sync Failed
- Check network connectivity
- Verify DHIS2 program IDs are correct
- Check DHIS2 logs for errors
- Verify user has permissions to create events

## License

Internal project - Connexi

## Contact

For questions or issues, contact the development team.
