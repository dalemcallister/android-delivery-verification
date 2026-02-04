#!/bin/bash

echo "=== Checking DHIS2 Data ==="
echo ""

echo "1. Events (Verifications):"
curl -s "http://192.168.88.9:8080/api/events?paging=false" -u admin:district | jq '.events | length'
echo ""

echo "2. Recent Events Details:"
curl -s "http://192.168.88.9:8080/api/events?paging=false&pageSize=5" -u admin:district | jq '.events[] | {event: .event, orgUnit: .orgUnit, eventDate: .eventDate, status: .status}'
echo ""

echo "3. Programs:"
curl -s "http://192.168.88.9:8080/api/programs?paging=false&fields=id,name" -u admin:district | jq '.programs[] | {id: .id, name: .name}'
echo ""

echo "=== End ==="
