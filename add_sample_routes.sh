#!/bin/bash

# Add Sample Routes to DHIS2
# This creates test route data using real facilities

DHIS2_URL="http://192.168.88.9:8080"
AUTH="admin:district"

# Load configuration
source /Users/dalemcallister/Desktop/connexidevepod/dhis2_config.txt

echo "================================================"
echo "Adding Sample Routes to DHIS2"
echo "================================================"
echo ""

# Get current date
CURRENT_DATE=$(date +%Y-%m-%d)

# Sample Route 1: Using Kenyan Hospitals
echo "Creating Route 1: Kenyan Hospitals Route..."

ROUTE_1_JSON='{
  "routeId": "ROUTE-001",
  "vehicleType": "TRUCK",
  "totalStops": 4,
  "totalDistance": 15000,
  "totalVolume": 800,
  "totalWeight": 3200,
  "stops": [
    {
      "facilityId": "HC004KNH001",
      "facilityName": "Kenyatta National Hospital",
      "latitude": -1.3018,
      "longitude": 36.8073,
      "orderVolume": 200,
      "orderWeight": 800,
      "stopNumber": 1,
      "distanceFromPrevious": 0
    },
    {
      "facilityId": "HC001KIB001",
      "facilityName": "Kibera Health Centre",
      "latitude": -1.3142,
      "longitude": 36.8472,
      "orderVolume": 150,
      "orderWeight": 600,
      "stopNumber": 2,
      "distanceFromPrevious": 4200
    },
    {
      "facilityId": "HC005MBA001",
      "facilityName": "Mbagathi Hospital",
      "latitude": -1.3281,
      "longitude": 36.7981,
      "orderVolume": 250,
      "orderWeight": 1000,
      "stopNumber": 3,
      "distanceFromPrevious": 5500
    },
    {
      "facilityId": "HC003PUM001",
      "facilityName": "Pumwani Maternity Hospital",
      "latitude": -1.2831,
      "longitude": 36.8520,
      "orderVolume": 200,
      "orderWeight": 800,
      "stopNumber": 4,
      "distanceFromPrevious": 5300
    }
  ]
}'

# Create event for Route 1
EVENT_1=$(curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "orgUnit": "HC004KNH001",
    "eventDate": "'"$CURRENT_DATE"'",
    "status": "COMPLETED",
    "dataValues": [
      {
        "dataElement": "'"$ROUTE_ID_DE"'",
        "value": "ROUTE-001"
      },
      {
        "dataElement": "'"$ROUTE_DETAILS_DE"'",
        "value": '"$(echo "$ROUTE_1_JSON" | jq -c | jq -R)"'
      },
      {
        "dataElement": "'"$VEHICLE_TYPE_DE"'",
        "value": "TRUCK"
      },
      {
        "dataElement": "'"$TOTAL_DISTANCE_DE"'",
        "value": "15000"
      }
    ]
  }' | jq -r '.response.importSummaries[0].reference // empty')

if [ -n "$EVENT_1" ]; then
  echo "✅ Created Route 1: $EVENT_1"
else
  echo "❌ Failed to create Route 1"
fi
echo ""

# Sample Route 2: Using Nigerian Hospitals
echo "Creating Route 2: Nigerian Hospitals Route..."

ROUTE_2_JSON='{
  "routeId": "ROUTE-002",
  "vehicleType": "VAN",
  "totalStops": 3,
  "totalDistance": 8500,
  "totalVolume": 450,
  "totalWeight": 1800,
  "stops": [
    {
      "facilityId": "rnOqS94pcjW",
      "facilityName": "Ahmadu Bello University Teaching Hospital",
      "latitude": 11.0898,
      "longitude": 7.6974,
      "orderVolume": 150,
      "orderWeight": 600,
      "stopNumber": 1,
      "distanceFromPrevious": 0
    },
    {
      "facilityId": "DTTyIGBEDft",
      "facilityName": "Barau Dikko Specialist Hospital",
      "latitude": 10.5230,
      "longitude": 7.4387,
      "orderVolume": 100,
      "orderWeight": 400,
      "stopNumber": 2,
      "distanceFromPrevious": 3000
    },
    {
      "facilityId": "jZX6zvDSSgq",
      "facilityName": "General Hospital Kawo",
      "latitude": 10.5461,
      "longitude": 7.4261,
      "orderVolume": 200,
      "orderWeight": 800,
      "stopNumber": 3,
      "distanceFromPrevious": 5500
    }
  ]
}'

# Create event for Route 2
EVENT_2=$(curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "program": "'"$PROGRAM_ID"'",
    "orgUnit": "rnOqS94pcjW",
    "eventDate": "'"$CURRENT_DATE"'",
    "status": "COMPLETED",
    "dataValues": [
      {
        "dataElement": "'"$ROUTE_ID_DE"'",
        "value": "ROUTE-002"
      },
      {
        "dataElement": "'"$ROUTE_DETAILS_DE"'",
        "value": '"$(echo "$ROUTE_2_JSON" | jq -c | jq -R)"'
      },
      {
        "dataElement": "'"$VEHICLE_TYPE_DE"'",
        "value": "VAN"
      },
      {
        "dataElement": "'"$TOTAL_DISTANCE_DE"'",
        "value": "8500"
      }
    ]
  }' | jq -r '.response.importSummaries[0].reference // empty')

if [ -n "$EVENT_2" ]; then
  echo "✅ Created Route 2: $EVENT_2"
else
  echo "❌ Failed to create Route 2"
fi
echo ""

# Verify events were created
echo "Verifying routes in DHIS2..."
ROUTE_COUNT=$(curl -s "$DHIS2_URL/api/events?program=$PROGRAM_ID&paging=false" -u "$AUTH" | jq '.events | length')
echo "✅ Total routes in DHIS2: $ROUTE_COUNT"
echo ""

echo "================================================"
echo "Sample Routes Added Successfully!"
echo "================================================"
echo ""
echo "Route 1: ROUTE-001 (Kenyan Hospitals) - Event: $EVENT_1"
echo "Route 2: ROUTE-002 (Nigerian Hospitals) - Event: $EVENT_2"
echo ""
echo "Next: Update app to fetch from DHIS2"
