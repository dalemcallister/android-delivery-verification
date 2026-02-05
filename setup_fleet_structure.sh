#!/bin/bash
# setup_fleet_structure.sh
# Creates Fleet Management organization structure in DHIS2

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

echo "================================================"
echo "Setting Up Fleet Organization Structure"
echo "================================================"
echo ""

# Step 1: Get parent orgUnit (Kaduna Central Depot)
echo "Finding parent organization unit..."
PARENT_ID=$(curl -s "$DHIS2_URL/api/organisationUnits?filter=name:like:Kaduna&fields=id,name" -u "$AUTH" | jq -r '.organisationUnits[0].id // empty')

if [ -z "$PARENT_ID" ]; then
  echo "❌ Could not find Kaduna parent orgUnit"
  echo "Using fallback - will create under root"
  # Get root orgUnit
  PARENT_ID=$(curl -s "$DHIS2_URL/api/organisationUnits?level=1&fields=id,name" -u "$AUTH" | jq -r '.organisationUnits[0].id')
fi

echo "✅ Parent orgUnit: $PARENT_ID"
echo ""

# Step 2: Create or find Fleet Management orgUnit
echo "Creating Fleet Management parent..."
FLEET_RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Fleet Management",
    "shortName": "Fleet",
    "openingDate": "2026-01-01",
    "parent": {"id": "'"$PARENT_ID"'"}
  }')

FLEET_ID=$(echo "$FLEET_RESPONSE" | jq -r '.response.uid // empty')

if [ -z "$FLEET_ID" ]; then
  # Check if it already exists
  FLEET_ID=$(curl -s "$DHIS2_URL/api/organisationUnits?filter=name:eq:Fleet%20Management&fields=id" -u "$AUTH" | jq -r '.organisationUnits[0].id // empty')
fi

if [ -n "$FLEET_ID" ]; then
  echo "✅ Fleet Management orgUnit: $FLEET_ID"
else
  echo "❌ Failed to create Fleet Management orgUnit"
  exit 1
fi
echo ""

# Step 3: Create Truck orgUnits
echo "Creating truck organization units..."
echo ""

declare -a TRUCKS=(
  "TRK-001:TRUCK:3000:5000:Truck TRK-001 (3-ton)"
  "TRK-002:VAN:1000:1500:Van TRK-002"
  "TRK-003:MOTORCYCLE:200:300:Motorcycle TRK-003"
)

# Store truck IDs for later use
echo "# Fleet Organization Unit IDs" > fleet_ids.txt
echo "FLEET_ID=$FLEET_ID" >> fleet_ids.txt

for truck_data in "${TRUCKS[@]}"; do
  IFS=':' read -r CODE TYPE CAP_KG CAP_L NAME <<< "$truck_data"

  echo "Creating $CODE..."

  TRUCK_RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/organisationUnits" \
    -H "Content-Type: application/json" \
    -u "$AUTH" \
    -d '{
      "name": "'"$NAME"'",
      "shortName": "'"$CODE"'",
      "code": "'"$CODE"'",
      "openingDate": "2026-01-01",
      "parent": {"id": "'"$FLEET_ID"'"}
    }')

  TRUCK_ID=$(echo "$TRUCK_RESPONSE" | jq -r '.response.uid // empty')

  if [ -z "$TRUCK_ID" ]; then
    # Check if it already exists
    TRUCK_ID=$(curl -s "$DHIS2_URL/api/organisationUnits?filter=code:eq:$CODE&fields=id" -u "$AUTH" | jq -r '.organisationUnits[0].id // empty')
  fi

  if [ -n "$TRUCK_ID" ]; then
    echo "✅ Created $CODE: $TRUCK_ID"
    # Replace dashes with underscores for bash variable compatibility
    VAR_NAME="${CODE//-/_}_ID"
    echo "$VAR_NAME=$TRUCK_ID" >> fleet_ids.txt
  else
    echo "❌ Failed to create $CODE"
  fi
done

echo ""
echo "================================================"
echo "Fleet Structure Setup Complete!"
echo "================================================"
echo ""
cat fleet_ids.txt
echo ""
echo "Next: Run ./create_driver_users.sh"
