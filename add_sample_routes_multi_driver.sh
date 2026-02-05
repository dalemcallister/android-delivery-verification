#!/bin/bash
# add_sample_routes_multi_driver.sh
# Adds sample routes to DHIS2 assigned to different trucks

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Load configuration
if [ ! -f "$SCRIPT_DIR/dhis2_config.txt" ]; then
  echo "❌ dhis2_config.txt not found in $SCRIPT_DIR"
  exit 1
fi

if [ ! -f "$SCRIPT_DIR/fleet_ids.txt" ]; then
  echo "❌ fleet_ids.txt not found. Run ./setup_fleet_structure.sh first"
  exit 1
fi

source "$SCRIPT_DIR/dhis2_config.txt"
source "$SCRIPT_DIR/fleet_ids.txt"

# Verify variables are loaded
if [ -z "$PROGRAM_ID" ] || [ -z "$TRK_001_ID" ]; then
  echo "❌ Failed to load configuration variables"
  echo "PROGRAM_ID=$PROGRAM_ID"
  echo "TRK_001_ID=$TRK_001_ID"
  exit 1
fi

echo "Loaded config: Program=$PROGRAM_ID, TRK-001=$TRK_001_ID"

echo "================================================"
echo "Adding Sample Routes for Multi-Driver Testing"
echo "================================================"
echo ""

# Get current date
CURRENT_DATE=$(date +%Y-%m-%d)

#
# Route 1: Assigned to TRK-001 (driver001)
#
echo "Creating Route 1 for TRK-001..."

ROUTE_1_JSON=$(cat <<'EOF'
{
  "routeId": "ROUTE-TRK001-001",
  "vehicleType": "TRUCK",
  "totalStops": 3,
  "totalDistance": 12500,
  "totalVolume": 600,
  "totalWeight": 2400,
  "stops": [
    {
      "facilityId": "rnOqS94pcjW",
      "facilityName": "Ahmadu Bello University Teaching Hospital",
      "latitude": 11.0898,
      "longitude": 7.6974,
      "orderVolume": 200,
      "orderWeight": 800,
      "stopNumber": 1,
      "distanceFromPrevious": 0
    },
    {
      "facilityId": "DTTyIGBEDft",
      "facilityName": "Barau Dikko Specialist Hospital",
      "latitude": 10.5230,
      "longitude": 7.4387,
      "orderVolume": 200,
      "orderWeight": 800,
      "stopNumber": 2,
      "distanceFromPrevious": 5000
    },
    {
      "facilityId": "jZX6zvDSSgq",
      "facilityName": "General Hospital Kawo",
      "latitude": 10.5461,
      "longitude": 7.4261,
      "orderVolume": 200,
      "orderWeight": 800,
      "stopNumber": 3,
      "distanceFromPrevious": 7500
    }
  ]
}
EOF
)

RESPONSE_1=$(curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "orgUnit": "'"$TRK_001_ID"'",
    "eventDate": "'"$CURRENT_DATE"'",
    "status": "COMPLETED",
    "dataValues": [
      {
        "dataElement": "'"$ROUTE_ID_DE"'",
        "value": "ROUTE-TRK001-001"
      },
      {
        "dataElement": "'"$ROUTE_DETAILS_DE"'",
        "value": '"$(echo "$ROUTE_1_JSON" | jq -Rc .)"'
      },
      {
        "dataElement": "'"$VEHICLE_TYPE_DE"'",
        "value": "TRUCK"
      },
      {
        "dataElement": "'"$TOTAL_DISTANCE_DE"'",
        "value": "12500"
      }
    ]
  }')

EVENT_1=$(echo "$RESPONSE_1" | jq -r '.response.importSummaries[0].reference // empty')

if [ -n "$EVENT_1" ]; then
  echo "✅ Route 1 created for TRK-001: $EVENT_1"
else
  echo "❌ Failed to create Route 1"
  echo "Error details:"
  echo "$RESPONSE_1" | jq '.'
fi
echo ""

#
# Route 2: Assigned to TRK-002 (driver002)
#
echo "Creating Route 2 for TRK-002..."

ROUTE_2_JSON=$(cat <<'EOF'
{
  "routeId": "ROUTE-TRK002-001",
  "vehicleType": "VAN",
  "totalStops": 2,
  "totalDistance": 8000,
  "totalVolume": 300,
  "totalWeight": 1200,
  "stops": [
    {
      "facilityId": "aWQTfvgPA5v",
      "facilityName": "Kachia PHC",
      "latitude": 9.8667,
      "longitude": 7.9167,
      "orderVolume": 150,
      "orderWeight": 600,
      "stopNumber": 1,
      "distanceFromPrevious": 0
    },
    {
      "facilityId": "lR25vyP8pjX",
      "facilityName": "Kagarko PHC",
      "latitude": 9.4833,
      "longitude": 7.7000,
      "orderVolume": 150,
      "orderWeight": 600,
      "stopNumber": 2,
      "distanceFromPrevious": 8000
    }
  ]
}
EOF
)

EVENT_2=$(curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "orgUnit": "'"$TRK_002_ID"'",
    "eventDate": "'"$CURRENT_DATE"'",
    "status": "COMPLETED",
    "dataValues": [
      {
        "dataElement": "'"$ROUTE_ID_DE"'",
        "value": "ROUTE-TRK002-001"
      },
      {
        "dataElement": "'"$ROUTE_DETAILS_DE"'",
        "value": '"$(echo "$ROUTE_2_JSON" | jq -Rc .)"'
      },
      {
        "dataElement": "'"$VEHICLE_TYPE_DE"'",
        "value": "VAN"
      },
      {
        "dataElement": "'"$TOTAL_DISTANCE_DE"'",
        "value": "8000"
      }
    ]
  }' | jq -r '.response.importSummaries[0].reference // empty')

if [ -n "$EVENT_2" ]; then
  echo "✅ Route 2 created for TRK-002: $EVENT_2"
else
  echo "❌ Failed to create Route 2"
fi
echo ""

#
# Route 3: Assigned to TRK-003 (driver003)
#
echo "Creating Route 3 for TRK-003..."

ROUTE_3_JSON=$(cat <<'EOF'
{
  "routeId": "ROUTE-TRK003-001",
  "vehicleType": "MOTORCYCLE",
  "totalStops": 2,
  "totalDistance": 5000,
  "totalVolume": 150,
  "totalWeight": 400,
  "stops": [
    {
      "facilityId": "mBLn0NZgLyE",
      "facilityName": "Kaura PHC",
      "latitude": 9.6667,
      "longitude": 8.4667,
      "orderVolume": 75,
      "orderWeight": 200,
      "stopNumber": 1,
      "distanceFromPrevious": 0
    },
    {
      "facilityId": "pn5v8bR3KDz",
      "facilityName": "Kubau PHC",
      "latitude": 10.7333,
      "longitude": 7.8667,
      "orderVolume": 75,
      "orderWeight": 200,
      "stopNumber": 2,
      "distanceFromPrevious": 5000
    }
  ]
}
EOF
)

EVENT_3=$(curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "orgUnit": "'"$TRK_003_ID"'",
    "eventDate": "'"$CURRENT_DATE"'",
    "status": "COMPLETED",
    "dataValues": [
      {
        "dataElement": "'"$ROUTE_ID_DE"'",
        "value": "ROUTE-TRK003-001"
      },
      {
        "dataElement": "'"$ROUTE_DETAILS_DE"'",
        "value": '"$(echo "$ROUTE_3_JSON" | jq -Rc .)"'
      },
      {
        "dataElement": "'"$VEHICLE_TYPE_DE"'",
        "value": "MOTORCYCLE"
      },
      {
        "dataElement": "'"$TOTAL_DISTANCE_DE"'",
        "value": "5000"
      }
    ]
  }' | jq -r '.response.importSummaries[0].reference // empty')

if [ -n "$EVENT_3" ]; then
  echo "✅ Route 3 created for TRK-003: $EVENT_3"
else
  echo "❌ Failed to create Route 3"
fi
echo ""

# Verify all routes
echo "Verifying routes in DHIS2..."
ROUTE_COUNT=$(curl -s "$DHIS2_URL/api/events?program=$PROGRAM_ID&paging=false" -u "$AUTH" | jq '.events | length')
echo "✅ Total routes in DHIS2: $ROUTE_COUNT"
echo ""

echo "================================================"
echo "Multi-Driver Routes Created Successfully!"
echo "================================================"
echo ""
echo "Route Assignments:"
echo "  • ROUTE-TRK001-001 → TRK-001 → driver001 (3 stops, TRUCK)"
echo "  • ROUTE-TRK002-001 → TRK-002 → driver002 (2 stops, VAN)"
echo "  • ROUTE-TRK003-001 → TRK-003 → driver003 (2 stops, MOTORCYCLE)"
echo ""
echo "Test Instructions:"
echo "  1. Login as driver001 → Should see only ROUTE-TRK001-001"
echo "  2. Login as driver002 → Should see only ROUTE-TRK002-001"
echo "  3. Login as driver003 → Should see only ROUTE-TRK003-001"
echo ""
