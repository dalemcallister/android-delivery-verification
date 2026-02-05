#!/bin/bash
# Simple test script to add one route

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Load IDs
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/dhis2_config.txt"
source "$SCRIPT_DIR/fleet_ids.txt"

echo "Creating test route for TRK-001..."
echo "Program: $PROGRAM_ID"
echo "OrgUnit: $TRK_001_ID"
echo ""

# Create the route JSON payload directly
ROUTE_JSON='{"routeId":"ROUTE-TRK001-001","vehicleType":"TRUCK","totalStops":3,"totalDistance":12500,"totalVolume":600,"totalWeight":2400,"stops":[{"facilityId":"rnOqS94pcjW","facilityName":"Ahmadu Bello University Teaching Hospital","latitude":11.0898,"longitude":7.6974,"orderVolume":200,"orderWeight":800,"stopNumber":1,"distanceFromPrevious":0},{"facilityId":"DTTyIGBEDft","facilityName":"Barau Dikko Specialist Hospital","latitude":10.5230,"longitude":7.4387,"orderVolume":200,"orderWeight":800,"stopNumber":2,"distanceFromPrevious":5000},{"facilityId":"jZX6zvDSSgq","facilityName":"General Hospital Kawo","latitude":10.5461,"longitude":7.4261,"orderVolume":200,"orderWeight":800,"stopNumber":3,"distanceFromPrevious":7500}]}'

# Create event
curl -s -X POST "$DHIS2_URL/api/events" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d "{
    \"program\": \"$PROGRAM_ID\",
    \"programStage\": \"$STAGE_ID\",
    \"orgUnit\": \"$TRK_001_ID\",
    \"eventDate\": \"2026-02-04\",
    \"status\": \"COMPLETED\",
    \"dataValues\": [
      {
        \"dataElement\": \"$ROUTE_ID_DE\",
        \"value\": \"ROUTE-TRK001-001\"
      },
      {
        \"dataElement\": \"$ROUTE_DETAILS_DE\",
        \"value\": $(echo "$ROUTE_JSON" | jq -R .)
      },
      {
        \"dataElement\": \"$VEHICLE_TYPE_DE\",
        \"value\": \"TRUCK\"
      },
      {
        \"dataElement\": \"$TOTAL_DISTANCE_DE\",
        \"value\": \"12500\"
      }
    ]
  }" | jq '.'
