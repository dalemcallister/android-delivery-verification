#!/bin/bash
# add_all_truck_routes.sh
# Adds one route to each truck for testing

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/dhis2_config.txt"
source "$SCRIPT_DIR/fleet_ids.txt"

echo "================================================"
echo "Adding Routes for All Trucks"
echo "================================================"
echo ""

# Route JSON templates (compact, no extra escaping)
ROUTE_1='{"routeId":"ROUTE-TRK001-001","vehicleType":"TRUCK","totalStops":3,"totalDistance":12500,"totalVolume":600,"totalWeight":2400,"stops":[{"facilityId":"rnOqS94pcjW","facilityName":"Ahmadu Bello University Teaching Hospital","latitude":11.0898,"longitude":7.6974,"orderVolume":200,"orderWeight":800,"stopNumber":1,"distanceFromPrevious":0},{"facilityId":"DTTyIGBEDft","facilityName":"Barau Dikko Specialist Hospital","latitude":10.5230,"longitude":7.4387,"orderVolume":200,"orderWeight":800,"stopNumber":2,"distanceFromPrevious":5000},{"facilityId":"jZX6zvDSSgq","facilityName":"General Hospital Kawo","latitude":10.5461,"longitude":7.4261,"orderVolume":200,"orderWeight":800,"stopNumber":3,"distanceFromPrevious":7500}]}'

ROUTE_2='{"routeId":"ROUTE-TRK002-001","vehicleType":"VAN","totalStops":2,"totalDistance":8000,"totalVolume":300,"totalWeight":1200,"stops":[{"facilityId":"aWQTfvgPA5v","facilityName":"Kachia PHC","latitude":9.8667,"longitude":7.9167,"orderVolume":150,"orderWeight":600,"stopNumber":1,"distanceFromPrevious":0},{"facilityId":"lR25vyP8pjX","facilityName":"Kagarko PHC","latitude":9.4833,"longitude":7.7000,"orderVolume":150,"orderWeight":600,"stopNumber":2,"distanceFromPrevious":8000}]}'

ROUTE_3='{"routeId":"ROUTE-TRK003-001","vehicleType":"MOTORCYCLE","totalStops":2,"totalDistance":5000,"totalVolume":150,"totalWeight":400,"stops":[{"facilityId":"mBLn0NZgLyE","facilityName":"Kaura PHC","latitude":9.6667,"longitude":8.4667,"orderVolume":75,"orderWeight":200,"stopNumber":1,"distanceFromPrevious":0},{"facilityId":"pn5v8bR3KDz","facilityName":"Kubau PHC","latitude":10.7333,"longitude":7.8667,"orderVolume":75,"orderWeight":200,"stopNumber":2,"distanceFromPrevious":5000}]}'

# Clear existing test routes
echo "Clearing existing test routes..."
curl -s "http://localhost:8080/api/events?program=$PROGRAM_ID&paging=false" -u "$AUTH" | jq -r '.events[].event' | while read EVENT_ID; do
  curl -s -X DELETE "http://localhost:8080/api/events/$EVENT_ID" -u "$AUTH" > /dev/null
done
echo "✅ Cleared"
echo ""

# Create Route 1 for TRK-001
echo "Creating route for TRK-001..."
curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d "{
    \"program\": \"$PROGRAM_ID\",
    \"programStage\": \"$STAGE_ID\",
    \"orgUnit\": \"$TRK_001_ID\",
    \"eventDate\": \"2026-02-04\",
    \"status\": \"ACTIVE\",
    \"dataValues\": [
      {\"dataElement\": \"$ROUTE_ID_DE\", \"value\": \"ROUTE-TRK001-001\"},
      {\"dataElement\": \"$ROUTE_DETAILS_DE\", \"value\": $(echo "$ROUTE_1" | jq -R .)},
      {\"dataElement\": \"$VEHICLE_TYPE_DE\", \"value\": \"TRUCK\"},
      {\"dataElement\": \"$TOTAL_DISTANCE_DE\", \"value\": \"12500\"}
    ]
  }" | jq -r 'if .response.importSummaries[0].status == "SUCCESS" then "✅ TRK-001: " + .response.importSummaries[0].reference else "❌ TRK-001 failed" end'

# Create Route 2 for TRK-002
echo "Creating route for TRK-002..."
curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d "{
    \"program\": \"$PROGRAM_ID\",
    \"programStage\": \"$STAGE_ID\",
    \"orgUnit\": \"$TRK_002_ID\",
    \"eventDate\": \"2026-02-04\",
    \"status\": \"ACTIVE\",
    \"dataValues\": [
      {\"dataElement\": \"$ROUTE_ID_DE\", \"value\": \"ROUTE-TRK002-001\"},
      {\"dataElement\": \"$ROUTE_DETAILS_DE\", \"value\": $(echo "$ROUTE_2" | jq -R .)},
      {\"dataElement\": \"$VEHICLE_TYPE_DE\", \"value\": \"VAN\"},
      {\"dataElement\": \"$TOTAL_DISTANCE_DE\", \"value\": \"8000\"}
    ]
  }" | jq -r 'if .response.importSummaries[0].status == "SUCCESS" then "✅ TRK-002: " + .response.importSummaries[0].reference else "❌ TRK-002 failed" end'

# Create Route 3 for TRK-003
echo "Creating route for TRK-003..."
curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d "{
    \"program\": \"$PROGRAM_ID\",
    \"programStage\": \"$STAGE_ID\",
    \"orgUnit\": \"$TRK_003_ID\",
    \"eventDate\": \"2026-02-04\",
    \"status\": \"ACTIVE\",
    \"dataValues\": [
      {\"dataElement\": \"$ROUTE_ID_DE\", \"value\": \"ROUTE-TRK003-001\"},
      {\"dataElement\": \"$ROUTE_DETAILS_DE\", \"value\": $(echo "$ROUTE_3" | jq -R .)},
      {\"dataElement\": \"$VEHICLE_TYPE_DE\", \"value\": \"MOTORCYCLE\"},
      {\"dataElement\": \"$TOTAL_DISTANCE_DE\", \"value\": \"5000\"}
    ]
  }" | jq -r 'if .response.importSummaries[0].status == "SUCCESS" then "✅ TRK-003: " + .response.importSummaries[0].reference else "❌ TRK-003 failed" end'

echo ""
echo "================================================"
echo "Summary"
echo "================================================"
TOTAL=$(curl -s "$DHIS2_URL/api/events?program=$PROGRAM_ID&paging=false" -u "$AUTH" | jq '.events | length')
echo "Total routes in DHIS2: $TOTAL"
echo ""
echo "Test on Android app:"
echo "  1. Login as driver001 → should see 1 route (TRK-001)"
echo "  2. Login as driver002 → should see 1 route (TRK-002)"
echo "  3. Login as driver003 → should see 1 route (TRK-003)"
