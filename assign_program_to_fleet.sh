#!/bin/bash
# assign_program_to_fleet.sh
# Assigns the Route Assignment Program to fleet org units

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

# Load configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/dhis2_config.txt"
source "$SCRIPT_DIR/fleet_ids.txt"

echo "================================================"
echo "Assigning Route Assignment Program to Fleet"
echo "================================================"
echo ""

# First, get the current program configuration
echo "Fetching program configuration..."
PROGRAM=$(curl -s "http://localhost:8080/api/programs/$PROGRAM_ID" -u "$AUTH")

echo "Current program name: $(echo "$PROGRAM" | jq -r '.name')"
echo ""

# Update program to include fleet org units
echo "Assigning program to fleet org units..."
echo "TRK-001: $TRK_001_ID"
echo "TRK-002: $TRK_002_ID"
echo "TRK-003: $TRK_003_ID"
echo ""

# Add all truck org units to the program
curl -s -X PUT "$DHIS2_URL/api/programs/$PROGRAM_ID" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "id": "'"$PROGRAM_ID"'",
    "name": "Route Assignment Program",
    "shortName": "Route Assignment",
    "programType": "WITHOUT_REGISTRATION",
    "organisationUnits": [
      {"id": "'"$TRK_001_ID"'"},
      {"id": "'"$TRK_002_ID"'"},
      {"id": "'"$TRK_003_ID"'"}
    ]
  }' | jq '.status // .'

echo ""
echo "✅ Program assigned to fleet org units"
echo ""

# Verify
echo "Verifying program org unit assignments..."
curl -s "$DHIS2_URL/api/programs/$PROGRAM_ID?fields=organisationUnits[id,name,code]" -u "$AUTH" | jq '.organisationUnits'

echo ""
echo "================================================"
echo "Program Assignment Complete!"
echo "================================================"
