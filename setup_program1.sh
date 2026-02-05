#!/bin/bash
# Complete setup for Route Assignment Program 1

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"
PROGRAM_ID="nnYQNh2XW8m"
STAGE_ID="hYmuhfhaqoH"

echo "Setting up Route Assignment Program 1..."
echo ""

# Add data elements
echo "1. Adding data elements to stage..."
curl -s -X POST "$DHIS2_URL/api/programStageDataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{"programStage":{"id":"'"$STAGE_ID"'"},"dataElement":{"id":"kLPeW2Yx9Zy"},"compulsory":false}' > /dev/null
echo "  ✓ Route ID"

curl -s -X POST "$DHIS2_URL/api/programStageDataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{"programStage":{"id":"'"$STAGE_ID"'"},"dataElement":{"id":"nBv8JxPq1Rs"},"compulsory":false}' > /dev/null
echo "  ✓ Route Details JSON"

curl -s -X POST "$DHIS2_URL/api/programStageDataElements" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{"programStage":{"id":"'"$STAGE_ID"'"},"dataElement":{"id":"mXc7V2Np5Wq"},"compulsory":false}' > /dev/null
echo "  ✓ Vehicle Type"

echo ""
echo "2. Assigning program to truck org units..."
curl -s -X POST "$DHIS2_URL/api/metadata" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "programs": [{
      "id": "'"$PROGRAM_ID"'",
      "organisationUnits": [
        {"id": "uMce0FFzxd0"},
        {"id": "LRwEvhB6MnP"},
        {"id": "WMPvZnzJ2Z2"}
      ]
    }]
  }' | jq -r 'if .status == "OK" then "  ✓ Trucks assigned" else "  ⚠ " + .status end'

echo ""
echo "3. Updating configuration..."
cat > dhis2_config.txt <<EOF
# DHIS2 Configuration for Android App
# Updated: $(date)

PROGRAM_ID=$PROGRAM_ID
ROUTE_ID_DE=kLPeW2Yx9Zy
ROUTE_DETAILS_DE=nBv8JxPq1Rs
VEHICLE_TYPE_DE=mXc7V2Np5Wq
TOTAL_DISTANCE_DE=amFQACwoYth
ROUTE_STATUS_DE=pYzQ3Wm8Ktx
ORDER_VOLUME_DE=cNfWOj9OlyR
ORDER_WEIGHT_DE=MtydVLMZaEN
STAGE_ID=$STAGE_ID
EOF

echo "  ✓ Config saved"
echo ""
echo "4. Creating test route for TRK-001..."

ROUTE_JSON='{"routeId":"TEST-ROUTE-001","vehicleType":"TRUCK","totalStops":3,"totalDistance":12500,"totalVolume":600,"totalWeight":2400,"stops":[{"facilityId":"rnOqS94pcjW","facilityName":"Ahmadu Bello University Teaching Hospital","latitude":11.0898,"longitude":7.6974,"orderVolume":200,"orderWeight":800,"stopNumber":1,"distanceFromPrevious":0},{"facilityId":"DTTyIGBEDft","facilityName":"Barau Dikko Specialist Hospital","latitude":10.5230,"longitude":7.4387,"orderVolume":200,"orderWeight":800,"stopNumber":2,"distanceFromPrevious":5000},{"facilityId":"jZX6zvDSSgq","facilityName":"General Hospital Kawo","latitude":10.5461,"longitude":7.4261,"orderVolume":200,"orderWeight":800,"stopNumber":3,"distanceFromPrevious":7500}]}'

curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "programStage": "'"$STAGE_ID"'",
    "orgUnit": "uMce0FFzxd0",
    "eventDate": "2026-02-04",
    "status": "ACTIVE",
    "dataValues": [
      {"dataElement": "kLPeW2Yx9Zy", "value": "TEST-ROUTE-001"},
      {"dataElement": "nBv8JxPq1Rs", "value": '"$(echo "$ROUTE_JSON" | jq -Rc .)"'},
      {"dataElement": "mXc7V2Np5Wq", "value": "TRUCK"},
      {"dataElement": "amFQACwoYth", "value": "12500"}
    ]
  }' > /tmp/route_create.json

ROUTE_STATUS=$(cat /tmp/route_create.json | jq -r '.response.importSummaries[0].status')
if [ "$ROUTE_STATUS" = "SUCCESS" ]; then
  echo "  ✓ Test route created!"
else
  echo "  ⚠ Route creation: $ROUTE_STATUS"
fi

echo ""
echo "5. Verifying setup..."
EVENT_COUNT=$(curl -s "$DHIS2_URL/api/events?program=$PROGRAM_ID&paging=false" -u "$AUTH" | jq '.events | length')
echo "  Routes in system: $EVENT_COUNT"

echo ""
echo "================================"
echo "✅ Setup Complete!"
echo "================================"
echo ""
echo "Program ID: $PROGRAM_ID"
echo "Stage ID: $STAGE_ID"
echo ""
echo "Next step: Update Android app to use new program ID"
