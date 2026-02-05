#!/bin/bash
# recreate_trucks.sh
# Properly creates truck organization units

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

echo "================================================"
echo "Creating Truck Organization Units"
echo "================================================"
echo ""

# Use the latest Fleet Management org unit
FLEET_ID="qrTv6Pity95"
echo "Using Fleet Management: $FLEET_ID"
echo ""

# Truck 1: TRK-001
echo "Creating TRK-001..."
TRUCK_1=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Truck TRK-001",
    "shortName": "TRK-001",
    "code": "TRK-001",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$FLEET_ID"'"}
  }')

TRK_001_ID=$(echo "$TRUCK_1" | jq -r '.response.uid // .uid // empty')
echo "$TRUCK_1" | jq '.status, .message'

if [ -n "$TRK_001_ID" ]; then
  echo "✅ TRK-001 created: $TRK_001_ID"
else
  echo "❌ Failed to create TRK-001"
  echo "$TRUCK_1" | jq '.'
fi
echo ""

# Truck 2: TRK-002
echo "Creating TRK-002..."
TRUCK_2=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Van TRK-002",
    "shortName": "TRK-002",
    "code": "TRK-002",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$FLEET_ID"'"}
  }')

TRK_002_ID=$(echo "$TRUCK_2" | jq -r '.response.uid // .uid // empty')
echo "$TRUCK_2" | jq '.status, .message'

if [ -n "$TRK_002_ID" ]; then
  echo "✅ TRK-002 created: $TRK_002_ID"
else
  echo "❌ Failed to create TRK-002"
fi
echo ""

# Truck 3: TRK-003
echo "Creating TRK-003..."
TRUCK_3=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Motorcycle TRK-003",
    "shortName": "TRK-003",
    "code": "TRK-003",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$FLEET_ID"'"}
  }')

TRK_003_ID=$(echo "$TRUCK_3" | jq -r '.response.uid // .uid // empty')
echo "$TRUCK_3" | jq '.status, .message'

if [ -n "$TRK_003_ID" ]; then
  echo "✅ TRK-003 created: $TRK_003_ID"
else
  echo "❌ Failed to create TRK-003"
fi
echo ""

# Save IDs
echo "# Fleet Organization Unit IDs (Updated)" > fleet_ids_new.txt
echo "FLEET_ID=$FLEET_ID" >> fleet_ids_new.txt
echo "TRK_001_ID=$TRK_001_ID" >> fleet_ids_new.txt
echo "TRK_002_ID=$TRK_002_ID" >> fleet_ids_new.txt
echo "TRK_003_ID=$TRK_003_ID" >> fleet_ids_new.txt

echo "✅ IDs saved to fleet_ids_new.txt"
cat fleet_ids_new.txt
