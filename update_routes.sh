#!/bin/bash
# update_routes.sh
# Updates existing events with route data

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/dhis2_config.txt"

echo "Updating routes with data..."

# Get event IDs for each truck
EVENT_TRK001=$(curl -s "$DHIS2_URL/api/events?program=$PROGRAM_ID&orgUnit=uMce0FFzxd0&paging=false" -u "$AUTH" | jq -r '.events[0].event')
EVENT_TRK002=$(curl -s "$DHIS2_URL/api/events?program=$PROGRAM_ID&orgUnit=LRwEvhB6MnP&paging=false" -u "$AUTH" | jq -r '.events[0].event')
EVENT_TRK003=$(curl -s "$DHIS2_URL/api/events?program=$PROGRAM_ID&orgUnit=WMPvZnzJ2Z2&paging=false" -u "$AUTH" | jq -r '.events[0].event')

echo "Event IDs:"
echo "  TRK-001: $EVENT_TRK001"
echo "  TRK-002: $EVENT_TRK002"
echo "  TRK-003: $EVENT_TRK003"
echo ""

# Update TRK-001 event
echo "Updating TRK-001..."
curl -s -X PUT "$DHIS2_URL/api/events/$EVENT_TRK001" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "programStage": "'"$STAGE_ID"'",
    "orgUnit": "uMce0FFzxd0",
    "eventDate": "2026-02-04",
    "status": "ACTIVE",
    "dataValues": [
      {"dataElement": "'"$ROUTE_ID_DE"'", "value": "ROUTE-TRK001-001"},
      {"dataElement": "'"$VEHICLE_TYPE_DE"'", "value": "TRUCK"},
      {"dataElement": "'"$TOTAL_DISTANCE_DE"'", "value": "12500"},
      {"dataElement": "'"$ROUTE_DETAILS_DE"'", "value": "{\"routeId\":\"ROUTE-TRK001-001\",\"vehicleType\":\"TRUCK\",\"totalStops\":3,\"totalDistance\":12500,\"totalVolume\":600,\"totalWeight\":2400,\"stops\":[{\"facilityId\":\"rnOqS94pcjW\",\"facilityName\":\"Ahmadu Bello University Teaching Hospital\",\"latitude\":11.0898,\"longitude\":7.6974,\"orderVolume\":200,\"orderWeight\":800,\"stopNumber\":1,\"distanceFromPrevious\":0},{\"facilityId\":\"DTTyIGBEDft\",\"facilityName\":\"Barau Dikko Specialist Hospital\",\"latitude\":10.5230,\"longitude\":7.4387,\"orderVolume\":200,\"orderWeight\":800,\"stopNumber\":2,\"distanceFromPrevious\":5000},{\"facilityId\":\"jZX6zvDSSgq\",\"facilityName\":\"General Hospital Kawo\",\"latitude\":10.5461,\"longitude\":7.4261,\"orderVolume\":200,\"orderWeight\":800,\"stopNumber\":3,\"distanceFromPrevious\":7500}]}"}
    ]
  }' | jq -r 'if .status == "OK" then "✅ TRK-001 updated" else "❌ TRK-001 failed: " + .message end'

# Update TRK-002 event
echo "Updating TRK-002..."
curl -s -X PUT "$DHIS2_URL/api/events/$EVENT_TRK002" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "programStage": "'"$STAGE_ID"'",
    "orgUnit": "LRwEvhB6MnP",
    "eventDate": "2026-02-04",
    "status": "ACTIVE",
    "dataValues": [
      {"dataElement": "'"$ROUTE_ID_DE"'", "value": "ROUTE-TRK002-001"},
      {"dataElement": "'"$VEHICLE_TYPE_DE"'", "value": "VAN"},
      {"dataElement": "'"$TOTAL_DISTANCE_DE"'", "value": "8000"},
      {"dataElement": "'"$ROUTE_DETAILS_DE"'", "value": "{\"routeId\":\"ROUTE-TRK002-001\",\"vehicleType\":\"VAN\",\"totalStops\":2,\"totalDistance\":8000,\"totalVolume\":300,\"totalWeight\":1200,\"stops\":[{\"facilityId\":\"aWQTfvgPA5v\",\"facilityName\":\"Kachia PHC\",\"latitude\":9.8667,\"longitude\":7.9167,\"orderVolume\":150,\"orderWeight\":600,\"stopNumber\":1,\"distanceFromPrevious\":0},{\"facilityId\":\"lR25vyP8pjX\",\"facilityName\":\"Kagarko PHC\",\"latitude\":9.4833,\"longitude\":7.7000,\"orderVolume\":150,\"orderWeight\":600,\"stopNumber\":2,\"distanceFromPrevious\":8000}]}"}
    ]
  }' | jq -r 'if .status == "OK" then "✅ TRK-002 updated" else "❌ TRK-002 failed: " + .message end'

# Update TRK-003 event
echo "Updating TRK-003..."
curl -s -X PUT "$DHIS2_URL/api/events/$EVENT_TRK003" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "programStage": "'"$STAGE_ID"'",
    "orgUnit": "WMPvZnzJ2Z2",
    "eventDate": "2026-02-04",
    "status": "ACTIVE",
    "dataValues": [
      {"dataElement": "'"$ROUTE_ID_DE"'", "value": "ROUTE-TRK003-001"},
      {"dataElement": "'"$VEHICLE_TYPE_DE"'", "value": "MOTORCYCLE"},
      {"dataElement": "'"$TOTAL_DISTANCE_DE"'", "value": "5000"},
      {"dataElement": "'"$ROUTE_DETAILS_DE"'", "value": "{\"routeId\":\"ROUTE-TRK003-001\",\"vehicleType\":\"MOTORCYCLE\",\"totalStops\":2,\"totalDistance\":5000,\"totalVolume\":150,\"totalWeight\":400,\"stops\":[{\"facilityId\":\"mBLn0NZgLyE\",\"facilityName\":\"Kaura PHC\",\"latitude\":9.6667,\"longitude\":8.4667,\"orderVolume\":75,\"orderWeight\":200,\"stopNumber\":1,\"distanceFromPrevious\":0},{\"facilityId\":\"pn5v8bR3KDz\",\"facilityName\":\"Kubau PHC\",\"latitude\":10.7333,\"longitude\":7.8667,\"orderVolume\":75,\"orderWeight\":200,\"stopNumber\":2,\"distanceFromPrevious\":5000}]}"}
    ]
  }' | jq -r 'if .status == "OK" then "✅ TRK-003 updated" else "❌ TRK-003 failed: " + .message end'

echo ""
echo "✅ All routes updated!"
echo ""
echo "🧪 Test on Android app:"
echo "  Login as driver001 → Should see ROUTE-TRK001-001 with 3 stops"
echo "  Login as driver002 → Should see ROUTE-TRK002-001 with 2 stops"
echo "  Login as driver003 → Should see ROUTE-TRK003-001 with 2 stops"
