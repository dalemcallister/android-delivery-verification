#!/bin/bash
# Create a complete working program from scratch

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

echo "Creating complete Route Assignment Program..."

# Create program with metadata API
curl -s -X POST "$DHIS2_URL/api/metadata?importStrategy=CREATE" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "programs": [{
      "name": "Route Assignment Program v2",
      "shortName": "Routes v2",
      "programType": "WITHOUT_REGISTRATION",
      "organisationUnits": [
        {"id": "uMce0FFzxd0"},
        {"id": "LRwEvhB6MnP"},
        {"id": "WMPvZnzJ2Z2"}
      ],
      "programStages": [{
        "name": "Route Assignment Stage",
        "repeatable": false,
        "minDaysFromStart": 0,
        "programStageDataElements": [
          {
            "dataElement": {"id": "kLPeW2Yx9Zy"},
            "compulsory": false
          },
          {
            "dataElement": {"id": "nBv8JxPq1Rs"},
            "compulsory": false
          },
          {
            "dataElement": {"id": "mXc7V2Np5Wq"},
            "compulsory": false
          },
          {
            "dataElement": {"id": "amFQACwoYth"},
            "compulsory": false
          }
        ]
      }]
    }]
  }' > /tmp/program_create.json

echo ""
echo "Result:"
cat /tmp/program_create.json | jq '{status: .status, message: .message}'

echo ""
echo "Finding new program ID..."
NEW_PROGRAM_ID=$(curl -s "$DHIS2_URL/api/programs?filter=name:like:v2&fields=id,name" -u "$AUTH" | jq -r '.programs[0].id')
NEW_STAGE_ID=$(curl -s "$DHIS2_URL/api/programs/$NEW_PROGRAM_ID?fields=programStages[id]" -u "$AUTH" | jq -r '.programStages[0].id')

echo "New Program ID: $NEW_PROGRAM_ID"
echo "New Stage ID: $NEW_STAGE_ID"

if [ -n "$NEW_PROGRAM_ID" ] && [ -n "$NEW_STAGE_ID" ]; then
  echo ""
  echo "Updating dhis2_config.txt..."
  cat > dhis2_config.txt <<EOF
# DHIS2 Configuration for Android App
# Updated: $(date)

PROGRAM_ID=$NEW_PROGRAM_ID
ROUTE_ID_DE=kLPeW2Yx9Zy
ROUTE_DETAILS_DE=nBv8JxPq1Rs
VEHICLE_TYPE_DE=mXc7V2Np5Wq
TOTAL_DISTANCE_DE=amFQACwoYth
ROUTE_STATUS_DE=pYzQ3Wm8Ktx
ORDER_VOLUME_DE=cNfWOj9OlyR
ORDER_WEIGHT_DE=MtydVLMZaEN
STAGE_ID=$NEW_STAGE_ID
EOF

  echo "✅ Configuration updated!"
  cat dhis2_config.txt
fi
