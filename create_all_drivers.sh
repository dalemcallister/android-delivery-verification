#!/bin/bash
# Create driver users for all trucks
DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

echo "================================================"
echo "Creating Driver Users for All Trucks"
echo "================================================"
echo ""

# Get the Delivery Driver role ID
echo "Finding Delivery Driver role..."
DRIVER_ROLE_ID=$(curl -s "$DHIS2_URL/api/userRoles?filter=name:eq:Delivery%20Driver&fields=id" -u "$AUTH" | python3 -c "import sys, json; roles=json.load(sys.stdin).get('userRoles',[]); print(roles[0]['id'] if roles else '')")

if [ -z "$DRIVER_ROLE_ID" ]; then
    echo "❌ Delivery Driver role not found"
    exit 1
fi

echo "✅ Delivery Driver role: $DRIVER_ROLE_ID"
echo ""

# Load truck IDs
source fleet_ids.txt

# Driver configurations
declare -a DRIVERS=(
    "driver002:Driver:002:$TRK_002_ID:TRK-002:+234701000002:driver002@example.com"
    "driver003:Driver:003:$TRK_003_ID:TRK-003:+234701000003:driver003@example.com"
)

for driver_data in "${DRIVERS[@]}"; do
    IFS=':' read -r USERNAME FIRST LAST TRUCK_ID TRUCK_CODE PHONE EMAIL <<< "$driver_data"

    if [ -z "$TRUCK_ID" ]; then
        echo "⚠️  Skipping $USERNAME - Truck ID not found for $TRUCK_CODE"
        continue
    fi

    echo "Creating $USERNAME..."
    echo "  Name: $FIRST $LAST"
    echo "  Truck: $TRUCK_CODE ($TRUCK_ID)"
    echo "  Phone: $PHONE"

    RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/users" \
        -H "Content-Type: application/json" \
        -u "$AUTH" \
        -d '{
            "firstName": "'"$FIRST"'",
            "surname": "'"$LAST"'",
            "email": "'"$EMAIL"'",
            "phoneNumber": "'"$PHONE"'",
            "userCredentials": {
                "username": "'"$USERNAME"'",
                "password": "Driver123!",
                "userRoles": [{"id": "'"$DRIVER_ROLE_ID"'"}]
            },
            "organisationUnits": [{"id": "'"$TRUCK_ID"'"}],
            "dataViewOrganisationUnits": [{"id": "'"$TRUCK_ID"'"}]
        }')

    STATUS=$(echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('status', 'unknown'))" 2>/dev/null)
    USER_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; resp=json.load(sys.stdin); uid=resp.get('response',{}).get('uid',''); print(uid)" 2>/dev/null)

    if [ "$STATUS" = "OK" ] && [ -n "$USER_ID" ]; then
        echo "  ✅ Created $USERNAME (ID: $USER_ID)"

        # Test login
        TEST_RESPONSE=$(curl -s "$DHIS2_URL/api/me" -u $USERNAME:Driver123!)
        TEST_USER=$(echo "$TEST_RESPONSE" | python3 -c "import sys, json; u=json.load(sys.stdin); print(u.get('username','FAILED'))" 2>/dev/null)

        if [ "$TEST_USER" = "$USERNAME" ]; then
            echo "  ✅ Login verified"
        else
            echo "  ⚠️  Login test failed"
        fi
    else
        echo "  ❌ Failed to create user"
        # Check if user already exists
        EXISTING=$(curl -s "$DHIS2_URL/api/users?filter=username:eq:$USERNAME&fields=id" -u "$AUTH" | python3 -c "import sys, json; users=json.load(sys.stdin).get('users',[]); print(users[0]['id'] if users else '')" 2>/dev/null)
        if [ -n "$EXISTING" ]; then
            echo "  ℹ️  User already exists (ID: $EXISTING)"
        fi
    fi
    echo ""
done

echo "================================================"
echo "Testing All Drivers"
echo "================================================"
echo ""

# Test all three drivers
for driver in driver001 driver002 driver003; do
    echo "Testing $driver..."

    # Get user info
    USER_INFO=$(curl -s "$DHIS2_URL/api/me?fields=username,organisationUnits[id,name,code]" -u $driver:Driver123!)
    USERNAME=$(echo "$USER_INFO" | python3 -c "import sys, json; u=json.load(sys.stdin); print(u.get('username','FAILED'))" 2>/dev/null)

    if [ "$USERNAME" = "$driver" ]; then
        echo "  ✅ Login successful"

        # Get assigned truck
        TRUCK=$(echo "$USER_INFO" | python3 -c "import sys, json; u=json.load(sys.stdin); orgs=u.get('organisationUnits',[]); print(orgs[0].get('code','N/A') if orgs else 'N/A')" 2>/dev/null)
        echo "  📦 Assigned truck: $TRUCK"

        # Check route access for each program
        for prog_id in qzRN0undLV4 nnYQNh2XW8m; do
            EVENT_COUNT=$(curl -s "$DHIS2_URL/api/events?program=$prog_id&pageSize=100" -u $driver:Driver123! | python3 -c "import sys, json; events=json.load(sys.stdin).get('events',[]); print(len(events))" 2>/dev/null)
            echo "  📋 Program $prog_id: $EVENT_COUNT routes visible"
        done
    else
        echo "  ❌ Login failed"
    fi
    echo ""
done

echo "================================================"
echo "Summary - Driver Credentials"
echo "================================================"
echo ""
echo "driver001 / Driver123!  → TRK-001 (3-ton Truck)"
echo "driver002 / Driver123!  → TRK-002 (Van)"
echo "driver003 / Driver123!  → TRK-003 (Motorcycle)"
echo ""
echo "All drivers can now login to the Android app!"
