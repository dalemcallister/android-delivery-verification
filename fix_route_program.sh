#!/bin/bash
# fix_route_program.sh
# Properly creates Route Assignment Program with program stage

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Load existing config
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/dhis2_config.txt"
source "$SCRIPT_DIR/fleet_ids.txt"

echo "================================================"
echo "Fixing Route Assignment Program"
echo "================================================"
echo ""

# Step 1: Delete existing broken program
echo "Step 1: Removing existing program..."
curl -s -X DELETE "$DHIS2_URL/api/programs/$PROGRAM_ID" -u "$AUTH"
echo "✅ Old program deleted"
echo ""

# Step 2: Create new program with proper structure
echo "Step 2: Creating new Route Assignment Program..."

PROGRAM_RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/programs" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Route Assignment Program",
    "shortName": "Routes",
    "programType": "WITHOUT_REGISTRATION",
    "organisationUnits": [
      {"id": "'"$TRK_001_ID"'"},
      {"id": "'"$TRK_002_ID"'"},
      {"id": "'"$TRK_003_ID"'"}
    ]
  }')

NEW_PROGRAM_ID=$(echo "$PROGRAM_RESPONSE" | jq -r '.response.uid // .uid // empty')

if [ -z "$NEW_PROGRAM_ID" ]; then
  echo "❌ Failed to create program"
  echo "$PROGRAM_RESPONSE" | jq '.'
  exit 1
fi

echo "✅ Program created: $NEW_PROGRAM_ID"
echo ""

# Step 3: Create program stage
echo "Step 3: Creating program stage..."

STAGE_RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/programStages" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Route Assignment",
    "description": "Assign routes to trucks",
    "program": {"id": "'"$NEW_PROGRAM_ID"'"},
    "repeatable": true,
    "minDaysFromStart": 0,
    "generatedByEnrollmentDate": false,
    "blockEntryForm": false,
    "preGenerateUID": false,
    "autoGenerateEvent": true,
    "openAfterEnrollment": false,
    "reportDateToUse": "dateOfIncident",
    "remindCompleted": false,
    "allowGenerateNextVisit": false,
    "validCompleteOnly": false,
    "hideDueDate": false,
    "enableUserAssignment": false
  }')

NEW_STAGE_ID=$(echo "$STAGE_RESPONSE" | jq -r '.response.uid // .uid // empty')

if [ -z "$NEW_STAGE_ID" ]; then
  echo "❌ Failed to create program stage"
  echo "$STAGE_RESPONSE" | jq '.'
  exit 1
fi

echo "✅ Program stage created: $NEW_STAGE_ID"
echo ""

# Step 4: Add data elements to program stage
echo "Step 4: Adding data elements to program stage..."

for DE_ID in "$ROUTE_ID_DE" "$ROUTE_DETAILS_DE" "$VEHICLE_TYPE_DE" "$TOTAL_DISTANCE_DE"; do
  curl -s -X POST "$DHIS2_URL/api/programStageDataElements" \
    -H "Content-Type: application/json" \
    -u "$AUTH" \
    -d '{
      "programStage": {"id": "'"$NEW_STAGE_ID"'"},
      "dataElement": {"id": "'"$DE_ID"'"},
      "compulsory": false,
      "allowProvidedElsewhere": false,
      "displayInReports": true,
      "allowFutureDate": false,
      "renderOptionsAsRadio": false,
      "skipSynchronization": false
    }' > /dev/null
done

echo "✅ Data elements added to program stage"
echo ""

# Step 5: Update config file with new IDs
echo "Step 5: Updating configuration file..."

cat > dhis2_config.txt <<EOF
# DHIS2 Configuration for Android App
# Updated: $(date)

PROGRAM_ID=$NEW_PROGRAM_ID
ROUTE_ID_DE=$ROUTE_ID_DE
ROUTE_DETAILS_DE=$ROUTE_DETAILS_DE
VEHICLE_TYPE_DE=$VEHICLE_TYPE_DE
TOTAL_DISTANCE_DE=$TOTAL_DISTANCE_DE
ORDER_VOLUME_DE=$ORDER_VOLUME_DE
ORDER_WEIGHT_DE=$ORDER_WEIGHT_DE
STAGE_ID=$NEW_STAGE_ID
EOF

echo "✅ Configuration updated"
echo ""

# Step 6: Verify setup
echo "Step 6: Verifying setup..."

VERIFY=$(curl -s "$DHIS2_URL/api/programs/$NEW_PROGRAM_ID?fields=id,name,programStages[id,name,programStageDataElements[dataElement[id,name]]]" -u "$AUTH")

STAGE_COUNT=$(echo "$VERIFY" | jq '.programStages | length')
DE_COUNT=$(echo "$VERIFY" | jq '.programStages[0].programStageDataElements | length')

echo "Program: $(echo "$VERIFY" | jq -r '.name')"
echo "Program Stages: $STAGE_COUNT"
echo "Data Elements: $DE_COUNT"
echo ""

if [ "$STAGE_COUNT" -eq 1 ] && [ "$DE_COUNT" -eq 4 ]; then
  echo "✅✅✅ SUCCESS! ✅✅✅"
  echo ""
  echo "Route Assignment Program is now properly configured!"
  echo ""
  echo "New IDs:"
  echo "  Program ID: $NEW_PROGRAM_ID"
  echo "  Stage ID: $NEW_STAGE_ID"
  echo ""
  echo "Next step: Run ./add_sample_routes_multi_driver.sh"
else
  echo "⚠️  Setup may be incomplete"
  echo "$VERIFY" | jq '.'
fi
