#!/bin/bash
# Create a fresh driver user: driver001
DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

echo "Creating driver001 user..."
echo "=========================="

# Get the Delivery Driver role ID
DRIVER_ROLE_ID=$(curl -s "$DHIS2_URL/api/userRoles?filter=name:eq:Delivery%20Driver&fields=id" -u "$AUTH" | python3 -c "import sys, json; roles=json.load(sys.stdin).get('userRoles',[]); print(roles[0]['id'] if roles else '')")

if [ -z "$DRIVER_ROLE_ID" ]; then
    echo "❌ Delivery Driver role not found"
    exit 1
fi

echo "✅ Found Delivery Driver role: $DRIVER_ROLE_ID"

# TRK-001 ID
TRK_001_ID="uMce0FFzxd0"

echo "Creating user driver001..."
RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/users" \
    -H "Content-Type: application/json" \
    -u "$AUTH" \
    -d '{
        "firstName": "Driver",
        "surname": "001",
        "email": "driver001@example.com",
        "phoneNumber": "+234701000001",
        "userCredentials": {
            "username": "driver001",
            "password": "Driver123!",
            "userRoles": [{"id": "'"$DRIVER_ROLE_ID"'"}]
        },
        "organisationUnits": [{"id": "'"$TRK_001_ID"'"}],
        "dataViewOrganisationUnits": [{"id": "'"$TRK_001_ID"'"}]
    }')

STATUS=$(echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('status', 'unknown'))")
USER_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; resp=json.load(sys.stdin); uid=resp.get('response',{}).get('uid',''); print(uid)")

if [ "$STATUS" = "OK" ] && [ -n "$USER_ID" ]; then
    echo "✅ Created driver001 (ID: $USER_ID)"
    echo ""
    echo "Testing login..."
    TEST_RESPONSE=$(curl -s "$DHIS2_URL/api/me" -u driver001:Driver123!)
    TEST_USER=$(echo "$TEST_RESPONSE" | python3 -c "import sys, json; u=json.load(sys.stdin); print(u.get('username','FAILED'))")

    if [ "$TEST_USER" = "driver001" ]; then
        echo "✅ Login successful!"
        echo ""
        echo "Testing route access..."
        EVENT_COUNT=$(curl -s "$DHIS2_URL/api/events?program=qzRN0undLV4&orgUnit=$TRK_001_ID" -u driver001:Driver123! | python3 -c "import sys, json; events=json.load(sys.stdin).get('events',[]); print(len(events))")
        echo "✅ driver001 can see $EVENT_COUNT events"
    else
        echo "❌ Login failed"
    fi
else
    echo "❌ Failed to create user"
    echo "$RESPONSE" | python3 -c "import sys, json; print(json.dumps(json.load(sys.stdin), indent=2))"
fi
