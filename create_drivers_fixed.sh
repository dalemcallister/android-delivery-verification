#!/bin/bash
# Create driver users with correct role ID

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"
ROLE_ID="gczXxpLc9RY"

# Load truck IDs
source fleet_ids.txt

echo "Creating driver users..."

# Driver 1
echo "Creating driver001..."
curl -s -X POST "$DHIS2_URL/api/users" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "firstName": "John",
    "surname": "Doe",
    "email": "driver001@connexi.com",
    "phoneNumber": "+254712345001",
    "username": "driver001",
    "userCredentials": {
      "username": "driver001",
      "password": "Driver123",
      "userRoles": [{"id": "'"$ROLE_ID"'"}]
    },
    "organisationUnits": [{"id": "'"$TRK_001_ID"'"}],
    "dataViewOrganisationUnits": [{"id": "'"$TRK_001_ID"'"}],
    "teiSearchOrganisationUnits": [{"id": "'"$TRK_001_ID"'"}]
  }' | jq -r 'if .response.uid then "✅ driver001: " + .response.uid else "❌ Failed" end'

# Driver 2
echo "Creating driver002..."
curl -s -X POST "$DHIS2_URL/api/users" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "firstName": "Jane",
    "surname": "Smith",
    "email": "driver002@connexi.com",
    "phoneNumber": "+254712345002",
    "username": "driver002",
    "userCredentials": {
      "username": "driver002",
      "password": "Driver123",
      "userRoles": [{"id": "'"$ROLE_ID"'"}]
    },
    "organisationUnits": [{"id": "'"$TRK_002_ID"'"}],
    "dataViewOrganisationUnits": [{"id": "'"$TRK_002_ID"'"}],
    "teiSearchOrganisationUnits": [{"id": "'"$TRK_002_ID"'"}]
  }' | jq -r 'if .response.uid then "✅ driver002: " + .response.uid else "❌ Failed" end'

# Driver 3
echo "Creating driver003..."
curl -s -X POST "$DHIS2_URL/api/users" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "firstName": "Mike",
    "surname": "Johnson",
    "email": "driver003@connexi.com",
    "phoneNumber": "+254712345003",
    "username": "driver003",
    "userCredentials": {
      "username": "driver003",
      "password": "Driver123",
      "userRoles": [{"id": "'"$ROLE_ID"'"}]
    },
    "organisationUnits": [{"id": "'"$TRK_003_ID"'"}],
    "dataViewOrganisationUnits": [{"id": "'"$TRK_003_ID"'"}],
    "teiSearchOrganisationUnits": [{"id": "'"$TRK_003_ID"'"}]
  }' | jq -r 'if .response.uid then "✅ driver003: " + .response.uid else "❌ Failed" end'

echo ""
echo "Testing logins..."
curl -s "http://localhost:8080/api/me" -u "driver001:Driver123" | jq -r 'if .id then "✅ driver001 login works" else "❌ driver001 login failed" end'
curl -s "http://localhost:8080/api/me" -u "driver002:Driver123" | jq -r 'if .id then "✅ driver002 login works" else "❌ driver002 login failed" end'
curl -s "http://localhost:8080/api/me" -u "driver003:Driver123" | jq -r 'if .id then "✅ driver003 login works" else "❌ driver003 login failed" end'

echo ""
echo "Login credentials:"
echo "  driver001 / Driver123 (TRK-001)"
echo "  driver002 / Driver123 (TRK-002)"
echo "  driver003 / Driver123 (TRK-003)"
