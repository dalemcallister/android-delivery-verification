#!/bin/bash
# Create test routes for trucks

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Load config
source dhis2_config.txt

if [ -z "$1" ]; then
  echo "Usage: $0 <truck-number>"
  echo "Example: $0 1  (for TRK-001)"
  echo ""
  echo "Available trucks:"
  echo "  1 - TRK-001 (uMce0FFzxd0)"
  echo "  2 - TRK-002 (LRwEvhB6MnP)"
  echo "  3 - TRK-003 (WMPvZnzJ2Z2)"
  exit 1
fi

# Map truck number to org unit ID
case $1 in
  1)
    ORG_UNIT="uMce0FFzxd0"
    TRUCK_CODE="TRK-001"
    ;;
  2)
    ORG_UNIT="LRwEvhB6MnP"
    TRUCK_CODE="TRK-002"
    ;;
  3)
    ORG_UNIT="WMPvZnzJ2Z2"
    TRUCK_CODE="TRK-003"
    ;;
  *)
    echo "Invalid truck number. Use 1, 2, or 3"
    exit 1
    ;;
esac

ROUTE_ID="ROUTE-$TRUCK_CODE-$(date +%Y%m%d-%H%M)"

echo "Creating test route for $TRUCK_CODE..."
echo "Route ID: $ROUTE_ID"
echo ""

# Route JSON with 3 stops
ROUTE_JSON=$(cat <<'EOF' | jq -Rs .
{"routeId":"ROUTE_ID_PLACEHOLDER","vehicleType":"TRUCK","totalStops":3,"totalDistance":12500,"totalVolume":600,"totalWeight":2400,"stops":[{"facilityId":"rnOqS94pcjW","facilityName":"Ahmadu Bello University Teaching Hospital","latitude":11.0898,"longitude":7.6974,"orderVolume":200,"orderWeight":800,"stopNumber":1,"distanceFromPrevious":0},{"facilityId":"DTTyIGBEDft","facilityName":"Barau Dikko Specialist Hospital","latitude":10.5230,"longitude":7.4387,"orderVolume":200,"orderWeight":800,"stopNumber":2,"distanceFromPrevious":5000},{"facilityId":"jZX6zvDSSgq","facilityName":"General Hospital Kawo","latitude":10.5461,"longitude":7.4261,"orderVolume":200,"orderWeight":800,"stopNumber":3,"distanceFromPrevious":7500}]}
EOF
)

# Replace placeholder with actual route ID
ROUTE_JSON=$(echo "$ROUTE_JSON" | sed "s/ROUTE_ID_PLACEHOLDER/$ROUTE_ID/")

# Create event payload
cat > /tmp/event_payload.json <<EOF
{
  "program": "$PROGRAM_ID",
  "programStage": "$STAGE_ID",
  "orgUnit": "$ORG_UNIT",
  "eventDate": "$(date +%Y-%m-%d)",
  "status": "ACTIVE",
  "dataValues": [
    {"dataElement": "$ROUTE_ID_DE", "value": "$ROUTE_ID"},
    {"dataElement": "$ROUTE_DETAILS_DE", "value": $ROUTE_JSON},
    {"dataElement": "$VEHICLE_TYPE_DE", "value": "TRUCK"},
    {"dataElement": "$TOTAL_DISTANCE_DE", "value": "12500"}
  ]
}
EOF

# Create the route
RESULT=$(curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d @/tmp/event_payload.json)

STATUS=$(echo "$RESULT" | jq -r '.response.importSummaries[0].status')
IMPORTED=$(echo "$RESULT" | jq -r '.response.importSummaries[0].importCount.imported')
REFERENCE=$(echo "$RESULT" | jq -r '.response.importSummaries[0].reference')

if [ "$STATUS" = "SUCCESS" ] || [ "$IMPORTED" = "1" ]; then
  echo "✅ Route created successfully!"
  echo "   Event ID: $REFERENCE"
  echo ""
  echo "Route details:"
  echo "  - Truck: $TRUCK_CODE"
  echo "  - Route ID: $ROUTE_ID"
  echo "  - Stops: 3"
  echo "  - Distance: 12.5 km"
  echo "  - Volume: 600 units"
  echo "  - Weight: 2400 kg"
else
  echo "❌ Failed to create route"
  echo "Status: $STATUS"
  echo ""
  echo "Full response:"
  echo "$RESULT" | jq .
fi
