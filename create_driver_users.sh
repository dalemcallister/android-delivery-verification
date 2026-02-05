#!/bin/bash
# create_driver_users.sh
# Creates driver user accounts in DHIS2

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

echo "================================================"
echo "Creating Driver User Accounts"
echo "================================================"
echo ""

# Load fleet IDs
if [ ! -f fleet_ids.txt ]; then
  echo "❌ fleet_ids.txt not found. Run ./setup_fleet_structure.sh first"
  exit 1
fi

source fleet_ids.txt

# Step 1: Create or find "Delivery Driver" user role
echo "Setting up Delivery Driver role..."

ROLE_RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/userRoles" \
  -H "Content-Type: application/json" \
  -u "$AUTH" \
  -d '{
    "name": "Delivery Driver",
    "description": "Role for delivery drivers using mobile app",
    "authorities": [
      "F_TRACKED_ENTITY_INSTANCE_SEARCH",
      "F_TRACKED_ENTITY_INSTANCE_ADD",
      "F_PROGRAM_EVENT_ADD",
      "F_PROGRAM_EVENT_UPDATE",
      "F_VIEW_EVENT_ANALYTICS",
      "M_dhis-web-dashboard"
    ]
  }')

DRIVER_ROLE_ID=$(echo "$ROLE_RESPONSE" | jq -r '.response.uid // empty')

if [ -z "$DRIVER_ROLE_ID" ]; then
  # Check if role already exists
  DRIVER_ROLE_ID=$(curl -s "$DHIS2_URL/api/userRoles?filter=name:eq:Delivery%20Driver&fields=id" -u "$AUTH" | jq -r '.userRoles[0].id // empty')
fi

if [ -n "$DRIVER_ROLE_ID" ]; then
  echo "✅ Delivery Driver role: $DRIVER_ROLE_ID"
else
  echo "❌ Failed to create Delivery Driver role"
  exit 1
fi
echo ""

# Step 2: Create driver user accounts
echo "Creating driver users..."
echo ""

declare -a DRIVERS=(
  "driver001:John:Doe:TRK-001:DRV-001:DL123456:+254712345001"
  "driver002:Jane:Smith:TRK-002:DRV-002:DL123457:+254712345002"
  "driver003:Mike:Johnson:TRK-003:DRV-003:DL123458:+254712345003"
)

for driver_data in "${DRIVERS[@]}"; do
  IFS=':' read -r USERNAME FIRST LAST TRUCK_CODE DRIVER_ID LICENSE PHONE <<< "$driver_data"

  # Get truck orgUnit ID from fleet_ids.txt
  # Replace dashes with underscores for bash variable compatibility
  TRUCK_VAR="${TRUCK_CODE//-/_}_ID"
  TRUCK_ID="${!TRUCK_VAR}"

  if [ -z "$TRUCK_ID" ]; then
    echo "❌ Could not find truck ID for $TRUCK_CODE"
    continue
  fi

  echo "Creating user: $USERNAME (assigned to $TRUCK_CODE)..."

  USER_RESPONSE=$(curl -s -X POST "$DHIS2_URL/api/users" \
    -H "Content-Type: application/json" \
    -u "$AUTH" \
    -d '{
      "firstName": "'"$FIRST"'",
      "surname": "'"$LAST"'",
      "email": "'"${USERNAME}@connexi.com"'",
      "phoneNumber": "'"$PHONE"'",
      "userCredentials": {
        "username": "'"$USERNAME"'",
        "password": "Driver123!",
        "userRoles": [{"id": "'"$DRIVER_ROLE_ID"'"}]
      },
      "organisationUnits": [{"id": "'"$TRUCK_ID"'"}],
      "dataViewOrganisationUnits": [{"id": "'"$TRUCK_ID"'"}],
      "teiSearchOrganisationUnits": [{"id": "'"$TRUCK_ID"'"}]
    }')

  USER_ID=$(echo "$USER_RESPONSE" | jq -r '.response.uid // empty')

  if [ -n "$USER_ID" ]; then
    echo "✅ Created $USERNAME: $USER_ID"
  else
    # Check if user already exists
    EXISTING_USER=$(curl -s "$DHIS2_URL/api/users?filter=userCredentials.username:eq:$USERNAME&fields=id" -u "$AUTH" | jq -r '.users[0].id // empty')
    if [ -n "$EXISTING_USER" ]; then
      echo "⚠️  User $USERNAME already exists: $EXISTING_USER"
    else
      ERROR_MSG=$(echo "$USER_RESPONSE" | jq -r '.message // "Unknown error"')
      echo "❌ Failed to create $USERNAME: $ERROR_MSG"
    fi
  fi
  echo ""
done

echo "================================================"
echo "Driver Users Created!"
echo "================================================"
echo ""
echo "Login Credentials:"
echo "  Username: driver001, Password: Driver123!, Truck: TRK-001"
echo "  Username: driver002, Password: Driver123!, Truck: TRK-002"
echo "  Username: driver003, Password: Driver123!, Truck: TRK-003"
echo ""
echo "Next: Update Android app to use driver credentials"
