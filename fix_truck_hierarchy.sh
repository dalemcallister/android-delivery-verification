#!/bin/bash
# fix_truck_hierarchy.sh
# Moves trucks to be direct children of Nigeria (Level 1)

DHIS2_URL="http://localhost:8080"
AUTH="admin:district"

echo "================================================"
echo "Fixing Truck Organization Hierarchy"
echo "================================================"
echo ""

# Level 1 orgUnit (Nigeria)
NIGERIA_ID="su0tIhYDDMo"

echo "Moving trucks to be direct children of Nigeria..."
echo ""

# Load existing truck IDs
if [ -f fleet_ids.txt ]; then
    source fleet_ids.txt
else
    echo "❌ fleet_ids.txt not found"
    exit 1
fi

# Array of truck IDs and names
declare -a TRUCKS=(
    "$TRK_001_ID:TRK-001:Truck TRK-001 (3-ton)"
    "$TRK_002_ID:TRK-002:Van TRK-002"
    "$TRK_003_ID:TRK-003:Motorcycle TRK-003"
)

for truck_data in "${TRUCKS[@]}"; do
    IFS=':' read -r TRUCK_ID CODE NAME <<< "$truck_data"

    if [ -z "$TRUCK_ID" ]; then
        echo "⚠️  Skipping $CODE - ID not found"
        continue
    fi

    echo "Updating $CODE ($TRUCK_ID)..."

    # Update the truck's parent to be Nigeria
    RESPONSE=$(curl -s -X PUT "$DHIS2_URL/api/organisationUnits/$TRUCK_ID" \
        -H "Content-Type: application/json" \
        -u "$AUTH" \
        -d '{
            "name": "'"$NAME"'",
            "shortName": "'"$CODE"'",
            "code": "'"$CODE"'",
            "openingDate": "2026-01-01",
            "parent": {"id": "'"$NIGERIA_ID"'"}
        }')

    STATUS=$(echo "$RESPONSE" | jq -r '.status // "success"')

    if [ "$STATUS" = "success" ] || [ "$STATUS" = "OK" ]; then
        echo "✅ Moved $CODE to Level 2 (under Nigeria)"
    else
        echo "❌ Failed to move $CODE"
        echo "$RESPONSE" | jq
    fi
    echo ""
done

echo "================================================"
echo "Verifying new hierarchy..."
echo "================================================"
echo ""

for truck_data in "${TRUCKS[@]}"; do
    IFS=':' read -r TRUCK_ID CODE NAME <<< "$truck_data"

    if [ -z "$TRUCK_ID" ]; then
        continue
    fi

    TRUCK_INFO=$(curl -s "$DHIS2_URL/api/organisationUnits/$TRUCK_ID?fields=id,name,code,level,parent[id,name]" -u "$AUTH")
    LEVEL=$(echo "$TRUCK_INFO" | jq -r '.level')
    PARENT_NAME=$(echo "$TRUCK_INFO" | jq -r '.parent.name')

    echo "$CODE: Level $LEVEL, Parent: $PARENT_NAME"
done

echo ""
echo "================================================"
echo "Truck hierarchy updated!"
echo "================================================"
echo ""
echo "Next step: Assign driver users to trucks"
echo "Run: ./create_driver_users.sh"
