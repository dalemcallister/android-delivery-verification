#!/bin/bash

# Complete DHIS2 Setup with Program Stage
DHIS2_URL="http://192.168.88.9:8080"
AUTH="admin:district"

echo "=== Setting up DHIS2 for Routes ==="
echo ""

# Load existing config
source /Users/dalemcallister/Desktop/connexidevepod/dhis2_config.txt

# Step 1: Create Program Stage
echo "Creating Program Stage..."
STAGE_RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/programStages" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Route Assignment",
    "description": "Route assignment details",
    "program": {"id": "'"$PROGRAM_ID"'"},
    "repeatable": false,
    "minDaysFromStart": 0
  }')

STAGE_ID=$(echo "$STAGE_RESPONSE" | jq -r '.response.uid // empty')

if [ -z "$STAGE_ID" ]; then
  # Check if stage already exists
  STAGE_ID=$(curl -s "$DHIS2_URL/api/programStages?filter=program.id:eq:$PROGRAM_ID&fields=id" -u "$AUTH" | jq -r '.programStages[0].id // empty')
fi

if [ -n "$STAGE_ID" ]; then
  echo "✅ Program Stage: $STAGE_ID"
else
  echo "❌ Failed to create program stage"
  exit 1
fi

# Step 2: Add Data Elements to Program Stage
echo "Adding data elements to program stage..."

for DE_ID in "$ROUTE_ID_DE" "$ROUTE_DETAILS_DE" "$VEHICLE_TYPE_DE" "$TOTAL_DISTANCE_DE"; do
  curl -s -X POST "$DHIS2_URL/api/programStageDataElements" \
    -H "Content-Type: application/json" \
    -u "$AUTH" \
    -d '{
      "programStage": {"id": "'"$STAGE_ID"'"},
      "dataElement": {"id": "'"$DE_ID"'"},
      "compulsory": false
    }' > /dev/null
done

echo "✅ Data elements added to program stage"

# Update config file
echo "STAGE_ID=$STAGE_ID" >> /Users/dalemcallister/Desktop/connexidevepod/dhis2_config.txt

echo ""
echo "✅ DHIS2 Setup Complete!"
echo "Program ID: $PROGRAM_ID"
echo "Stage ID: $STAGE_ID"
echo ""
echo "Now run: ./add_routes_to_dhis2.sh"
