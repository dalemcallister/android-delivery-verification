#!/bin/bash
# Fix driver001 configuration

echo "Updating driver001 configuration..."

# Get current user data
curl -s "http://localhost:8080/api/users/m5ymjFBd8My" -u "admin:district" > /tmp/driver001.json

# Update orgUnits
jq '.organisationUnits = [{"id": "uMce0FFzxd0"}] | .dataViewOrganisationUnits = [{"id": "uMce0FFzxd0"}] | .teiSearchOrganisationUnits = [{"id": "uMce0FFzxd0"}]' /tmp/driver001.json > /tmp/driver001_updated.json

# Save back
curl -s -X PUT "http://localhost:8080/api/users/m5ymjFBd8My" \
  -H "Content-Type: application/json" \
  -u "admin:district" \
  -d @/tmp/driver001_updated.json | jq '.status'

echo ""
echo "Testing route access..."
source dhis2_config.txt
curl -s "http://localhost:8080/api/events?program=$PROGRAM_ID&paging=false" -u "driver001:Driver123" | jq '{totalRoutes: .events | length}'
