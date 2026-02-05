#!/bin/bash
# create_trucks_final.sh
# Creates truck org units under Kaduna Central Depot

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Use Kaduna Central Depot as parent
PARENT_ID="wjDCrWdYslY"

echo "================================================"
echo "Creating Truck Organization Units"
echo "================================================"
echo ""
echo "Parent: Kaduna Central Depot ($PARENT_ID)"
echo ""

# Create TRK-001
echo "Creating TRK-001..."
curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Truck TRK-001",
    "shortName": "TRK-001",
    "code": "TRK-001",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$PARENT_ID"'"}
  }' > /tmp/truck1.json

TRK_001_ID=$(jq -r '.response.uid // .uid // empty' /tmp/truck1.json)

if [ -n "$TRK_001_ID" ]; then
  # Verify it was created
  VERIFY=$(curl -s "$DHIS2_URL/api/organisationUnits/$TRK_001_ID" -u "$AUTH" | jq -r '.id // empty')
  if [ -n "$VERIFY" ]; then
    echo "✅ TRK-001 created and verified: $TRK_001_ID"
  else
    echo "❌ TRK-001 UID returned but not found: $TRK_001_ID"
    TRK_001_ID=""
  fi
else
  echo "❌ Failed to create TRK-001"
  cat /tmp/truck1.json | jq '.'
fi
echo ""

# Create TRK-002
echo "Creating TRK-002..."
curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Van TRK-002",
    "shortName": "TRK-002",
    "code": "TRK-002",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$PARENT_ID"'"}
  }' > /tmp/truck2.json

TRK_002_ID=$(jq -r '.response.uid // .uid // empty' /tmp/truck2.json)

if [ -n "$TRK_002_ID" ]; then
  VERIFY=$(curl -s "$DHIS2_URL/api/organisationUnits/$TRK_002_ID" -u "$AUTH" | jq -r '.id // empty')
  if [ -n "$VERIFY" ]; then
    echo "✅ TRK-002 created and verified: $TRK_002_ID"
  else
    echo "❌ TRK-002 UID returned but not found: $TRK_002_ID"
    TRK_002_ID=""
  fi
else
  echo "❌ Failed to create TRK-002"
fi
echo ""

# Create TRK-003
echo "Creating TRK-003..."
curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Motorcycle TRK-003",
    "shortName": "TRK-003",
    "code": "TRK-003",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$PARENT_ID"'"}
  }' > /tmp/truck3.json

TRK_003_ID=$(jq -r '.response.uid // .uid // empty' /tmp/truck3.json)

if [ -n "$TRK_003_ID" ]; then
  VERIFY=$(curl -s "$DHIS2_URL/api/organisationUnits/$TRK_003_ID" -u "$AUTH" | jq -r '.id // empty')
  if [ -n "$VERIFY" ]; then
    echo "✅ TRK-003 created and verified: $TRK_003_ID"
  else
    echo "❌ TRK-003 UID returned but not found: $TRK_003_ID"
    TRK_003_ID=""
  fi
else
  echo "❌ Failed to create TRK-003"
fi
echo ""

# Save verified IDs
if [ -n "$TRK_001_ID" ] && [ -n "$TRK_002_ID" ] && [ -n "$TRK_003_ID" ]; then
  echo "# Fleet IDs (Verified)" > fleet_ids.txt
  echo "PARENT_ID=$PARENT_ID" >> fleet_ids.txt
  echo "TRK_001_ID=$TRK_001_ID" >> fleet_ids.txt
  echo "TRK_002_ID=$TRK_002_ID" >> fleet_ids.txt
  echo "TRK_003_ID=$TRK_003_ID" >> fleet_ids.txt

  echo "✅ All trucks created successfully!"
  echo ""
  cat fleet_ids.txt
else
  echo "❌ Some trucks failed to create"
fi
