# Where Data is Stored - Complete Explanation

## 🎯 Quick Answer

**Mock Routes**: ❌ NOT in DHIS2 (only on your phone)
**Delivery Verifications**: ✅ Can be synced to DHIS2

---

## 📱 Current State

### On Your Phone (Local Database)
```
Room Database (SQLite)
├── Routes Table
│   ├── Route 1: Nairobi City Route (4 stops)
│   └── Route 2: Westlands Route (3 stops)
├── Deliveries Table
│   ├── 7 delivery stops (all PENDING)
└── Verifications Table
    └── (empty - no deliveries completed yet)
```

### In DHIS2 Server
```
DHIS2 (192.168.88.9:8080)
├── Events
│   └── 0 events (empty)
├── Programs
│   └── (system programs only)
└── Data Values
    └── (empty)
```

---

## 🔄 Complete Data Flow

### Phase 1: Mock Data (CURRENT)
```
App Launches
    ↓
Mock Data Created
    ↓
Stored in Phone's Database
    ↓
❌ NOT sent to DHIS2
```

**Why?** Routes are just for organizing deliveries in the app. DHIS2 doesn't need to know about routes.

### Phase 2: Complete Delivery (WHEN YOU TEST)
```
User completes delivery
    ↓
GPS location captured
    ↓
Quantities entered
    ↓
Verification Created
    ↓
Stored in Phone's Database
    ↓
Status: PENDING SYNC
```

### Phase 3: Sync to DHIS2 (AFTER COMPLETING DELIVERY)
```
User taps "Sync Now"
    ↓
App reads pending verifications
    ↓
Creates DHIS2 Event for each verification
    ↓
POST to /api/events
    ↓
✅ Stored in DHIS2!
    ↓
Status: SYNCED
```

---

## 📊 What Gets Synced to DHIS2

### ✅ Synced (Verifications)
When you complete a delivery, this data goes to DHIS2:
- GPS Latitude
- GPS Longitude
- GPS Accuracy
- Distance from target
- Order Volume
- Order Weight
- Actual Volume delivered
- Actual Weight delivered
- Comments
- Signature (base64)
- Photo (base64)
- Verification timestamp

### ❌ NOT Synced (Routes)
These stay local only:
- Route ID
- Route list
- Vehicle type
- Total stops
- Route progress

**Why?** In production, routes would come FROM DHIS2 or the DRO API. For testing, we use mock routes that exist only in the app.

---

## 🧪 Test It Yourself

### 1. Check DHIS2 Now (Should be empty)
```bash
./VERIFY_DHIS2_DATA.sh
```
You'll see: **0 events**

### 2. Complete a Delivery in App
1. Open app
2. Route 1 → First delivery
3. Complete it

### 3. Sync to DHIS2
1. Tap Sync icon
2. See "Pending: 1"
3. Tap "Sync Now"

### 4. Check DHIS2 Again
```bash
./VERIFY_DHIS2_DATA.sh
```
You'll see: **1 event** ✅

### 5. View in DHIS2 Web Interface
1. Browser: http://192.168.88.9:8080
2. Login: admin / district
3. Go to: Apps → Event Reports or Data Entry
4. See your verification!

---

## 🔍 Check What's in DHIS2

### Method 1: Run Script
```bash
cd /Users/dalemcallister/Desktop/connexidevepod
./VERIFY_DHIS2_DATA.sh
```

### Method 2: Manual API Check
```bash
# Count events
curl -s "http://192.168.88.9:8080/api/events?paging=false" -u admin:district | jq '.events | length'

# List recent events
curl -s "http://192.168.88.9:8080/api/events?pageSize=5" -u admin:district | jq '.events'
```

### Method 3: DHIS2 Web UI
1. http://192.168.88.9:8080
2. Apps → Event Reports
3. Or: Apps → Event Capture

---

## 📈 Production vs Testing

### In Production (Future)
```
DRO API/DHIS2
    ↓ (Routes created)
DHIS2 Data Values/Events
    ↓ (App fetches)
Android App
    ↓ (Driver completes deliveries)
Verifications Created
    ↓ (Sync)
Back to DHIS2 ✅
```

### In Testing (Current)
```
Mock Data (hardcoded)
    ↓
Android App
    ↓ (Driver completes deliveries)
Verifications Created
    ↓ (Sync)
DHIS2 ✅
```

**Key Difference**: Routes are mocked locally, but verifications still sync to real DHIS2!

---

## 🎯 What You Can Test Right Now

### ✅ Working
1. Login to DHIS2 (authentication)
2. View routes (mock data)
3. GPS detection
4. Complete deliveries
5. Save verifications locally
6. Sync verifications to DHIS2
7. Offline mode (complete without internet)
8. View sync status

### ⏳ Needs DHIS2 Setup
1. Fetch real routes from DHIS2
2. Route optimization integration
3. Real facility coordinates from DHIS2

---

## 💡 Summary

### Current Architecture
```
┌──────────────────────┐
│   Android App        │
│   ┌──────────────┐   │
│   │ Mock Routes  │   │ ← Local only
│   └──────────────┘   │
│   ┌──────────────┐   │
│   │Verifications │   │ ← Syncs to DHIS2
│   └──────────────┘   │
└──────────────────────┘
         ↓ Sync
┌──────────────────────┐
│   DHIS2 Server       │
│   ┌──────────────┐   │
│   │   Events     │   │ ← Verifications here
│   └──────────────┘   │
└──────────────────────┘
```

**Mock routes = Testing convenience**
**Real data in DHIS2 = Verifications after sync**

---

## 🚀 Next Steps

1. **Complete a delivery** in the app
2. **Sync it** to DHIS2
3. **Run the verification script** to see it in DHIS2
4. **Open DHIS2 web interface** to view the data

Then you'll see real data in DHIS2! 🎉
