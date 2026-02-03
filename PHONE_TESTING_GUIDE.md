# Testing the App on Your Android Phone

## Prerequisites
- Android phone with USB debugging enabled
- USB cable to connect phone to computer
- Computer and phone on the same WiFi network (for DHIS2 access)

## Step 1: Enable Developer Options on Your Phone

### For most Android devices:
1. Go to **Settings** → **About Phone**
2. Find **Build Number** (might be under "Software Information")
3. Tap **Build Number** 7 times
4. You'll see "You are now a developer!" message

### Enable USB Debugging:
1. Go back to **Settings**
2. Find **Developer Options** (usually under System or Advanced)
3. Turn on **Developer Options**
4. Enable **USB Debugging**
5. Enable **Install via USB** (if available)

## Step 2: Connect Your Phone to Computer

1. Connect phone to computer via USB cable
2. On your phone, you'll see "Allow USB debugging?" popup
3. Check "Always allow from this computer"
4. Tap **OK**

### Verify Connection:
```bash
adb devices
```
You should see your device listed like:
```
List of devices attached
ABC123XYZ    device
```

If you see "unauthorized", check your phone for the authorization popup.

## Step 3: Find Your Computer's IP Address

You need this so your phone can access DHIS2 on your computer.

### On Mac:
```bash
ipconfig getifaddr en0
```
Or check: System Preferences → Network → WiFi → Details

### On Windows:
```bash
ipconfig
```
Look for "IPv4 Address" under your WiFi adapter

### On Linux:
```bash
hostname -I
```

**Example IP**: `192.168.1.100` (yours will be different)

## Step 4: Update Server URL for Phone Testing

Edit `app/build.gradle.kts` and change the default DHIS2 URL:

```kotlin
buildConfigField("String", "DEFAULT_DHIS2_URL", "\"http://YOUR_IP_HERE:8080\"")
```

Replace `YOUR_IP_HERE` with your computer's IP address from Step 3.

**Example:**
```kotlin
buildConfigField("String", "DEFAULT_DHIS2_URL", "\"http://192.168.1.100:8080\"")
```

**Important**: Make sure DHIS2 is configured to accept connections from your network:
- If using Docker, map ports: `-p 8080:8080`
- If using Tomcat, ensure it binds to `0.0.0.0` not just `localhost`

## Step 5: Build and Install APK

### Method A: Direct Install from Android Studio (Easiest)

1. Open the project in Android Studio
2. Connect your phone via USB
3. In the device dropdown (top toolbar), select your phone
4. Click the **Run** button (▶️)
5. Wait for the build and installation
6. App will launch automatically on your phone

### Method B: Build APK and Install Manually

#### Build the APK:
```bash
cd /Users/dalemcallister/Desktop/connexidevepod
./gradlew assembleDebug
```

The APK will be created at:
```
app/build/outputs/apk/debug/app-debug.apk
```

#### Install to Phone:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The `-r` flag reinstalls if already installed.

### Method C: Transfer APK to Phone

1. Build the APK (see Method B)
2. Copy `app-debug.apk` to your phone via:
   - USB transfer
   - Email it to yourself
   - Upload to cloud storage
3. On your phone, open the APK file
4. Tap "Install" (you may need to allow "Install from unknown sources")

## Step 6: Configure Phone for GPS Testing

### Enable Location Services:
1. Settings → Location
2. Turn on Location
3. Set Location Mode to "High Accuracy"
4. Allow location access for the app when prompted

### Optional - Mock Location for Testing:
1. Install a GPS spoofing app like "Fake GPS location"
2. In Developer Options, set it as "Mock location app"
3. Set fake coordinates matching a delivery location

## Step 7: Test the App on Your Phone

### First Run:
1. Open "Delivery Verification" app
2. Grant Location permission when prompted
3. Grant Camera permission (for future photo feature)

### Login:
- **Server URL**: `http://YOUR_COMPUTER_IP:8080` (e.g., `http://192.168.1.100:8080`)
- **Username**: `admin`
- **Password**: `district`

### Test Workflow:
1. ✅ Login successfully
2. ✅ See routes list (or empty state)
3. ✅ Check GPS status in delivery verification screen
4. ✅ Test offline mode (turn off WiFi)
5. ✅ Test sync when back online

## Troubleshooting

### "App not installed" Error
**Solution**: Uninstall any existing version first
```bash
adb uninstall com.connexi.deliveryverification
```
Then try installing again.

### "USB Debugging Unauthorized"
**Solution**:
- Revoke USB debugging authorizations on phone
- Disconnect and reconnect USB
- Accept the authorization popup again

### "Cannot connect to DHIS2"
**Solutions**:
1. **Check WiFi**: Phone and computer on same network?
2. **Check IP address**: Is it correct in the app?
3. **Test connection**: From phone's browser, open `http://YOUR_IP:8080`
4. **Check firewall**: Allow port 8080 on your computer
5. **DHIS2 running**: Is DHIS2 actually running on your computer?

### "Location not working"
**Solutions**:
1. Enable Location Services on phone
2. Grant location permissions to the app
3. Go outside or near a window for better GPS signal
4. Use mock location for testing

### "App crashes on launch"
**Solution**: Check logs:
```bash
adb logcat | grep DeliveryApp
```

### Phone Not Detected by adb
**Solutions**:
1. Try a different USB cable
2. Enable "USB debugging" again
3. Restart adb:
   ```bash
   adb kill-server
   adb start-server
   ```
4. Check USB connection mode (should be "File Transfer" or "PTP")

## Testing Checklist

- [ ] App installs successfully
- [ ] App opens without crashing
- [ ] Login screen displays correctly
- [ ] Can login with DHIS2 credentials
- [ ] GPS location is detected
- [ ] Location permission is granted
- [ ] Can navigate between screens
- [ ] Can view routes (if data exists)
- [ ] Can open delivery verification
- [ ] GPS status updates in real-time
- [ ] Can complete a delivery
- [ ] Offline mode works
- [ ] Sync works when back online

## Building a Release APK (Optional)

For a production-ready APK:

1. Generate a keystore (one-time):
```bash
keytool -genkey -v -keystore delivery-app.keystore -alias delivery-key -keyalg RSA -keysize 2048 -validity 10000
```

2. Create `app/keystore.properties`:
```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=delivery-key
storeFile=../delivery-app.keystore
```

3. Update `app/build.gradle.kts` signing config

4. Build release APK:
```bash
./gradlew assembleRelease
```

Release APK will be at: `app/build/outputs/apk/release/app-release.apk`

## Wireless Debugging (Android 11+)

After initial USB connection, you can debug wirelessly:

1. Connect phone via USB
2. Run: `adb tcpip 5555`
3. Disconnect USB
4. Find phone's IP: Settings → About → Status → IP address
5. Connect: `adb connect PHONE_IP:5555`

Now you can deploy and test without USB cable!

## Tips for Real-World Testing

1. **Test in Actual Delivery Vehicle**: GPS works better outdoors
2. **Test Offline Scenario**: Turn off mobile data and WiFi during delivery
3. **Test Multiple Deliveries**: Complete 3-5 deliveries in sequence
4. **Test Sync**: Return to WiFi area and verify sync works
5. **Battery Testing**: Monitor battery usage during extended use
6. **Different GPS Conditions**: Test in urban, rural, indoor scenarios

## Performance Tips

- Keep app in foreground for best GPS accuracy
- Close other apps to free memory
- Disable battery optimization for the app
- Keep phone charged or plugged in during extended testing

## Need Help?

- Check app logs: `adb logcat | grep -i "delivery"`
- Check DHIS2 logs on your computer
- Verify network connectivity: `ping YOUR_COMPUTER_IP`
- Test DHIS2 API: `curl http://YOUR_IP:8080/api/system/info`

---

**Happy Testing! 📱✅**
