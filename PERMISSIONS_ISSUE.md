# DHIS2 Permissions Issue - Driver Cannot See Routes

## Problem
The testdriver user cannot see events/routes even though:
- Program has been shared with the user (data read access)
- Program stage has been shared with the user
- User is assigned to the correct organization unit (TRK-001 / uMce0FFzxd0)
- User has the necessary authorities in their role

## Root Cause
Events in the program have enrollments attached (enrollment ID: fVXoJldXrpe) even though this is an Event Program (WITHOUT_REGISTRATION). The enrollment doesn't have a proper orgUnit assignment and was created by "system-process", not a user. This enrollment acts as a permission barrier.

## What We've Tried
1. ✅ Added program to driver role - No effect
2. ✅ Shared program with driver user (r-r----- access) - No effect
3. ✅ Shared program stage with driver user - No effect
4. ✅ Made program and program stage publicly readable for data - No effect
5. ✅ Added authorities: F_PROGRAM_ENROLLMENT, F_PROGRAMSTAGE_ADD, F_UNCOMPLETED_EVENT_EDIT - No effect
6. ✅ Reset testdriver password to Test123! - Works for login but still no data access
7. ✅ Verified user is assigned to TRK-001 orgUnit - Confirmed
8. ✅ Verified user has dataViewOrganisationUnits set - Confirmed

## Test Results
- **Admin** can see 1 event for program nnYQNh2XW8m
- **testdriver** sees 0 events with same query
- **API Query**: `GET /api/events?program=nnYQNh2XW8m&orgUnit=uMce0FFzxd0&ouMode=SELECTED&paging=false`

## Recommended Solutions

### Option 1: Use Admin Account for Testing (Quick Fix)
For immediate testing of the Android app:
1. Login as `admin` / `district` in the app
2. Admin has full access to all events
3. Test the route fetching and delivery verification features
4. Deal with multi-user permissions later

### Option 2: Fix via DHIS2 Web UI (Proper Fix)
The DHIS2 Web UI has better permission configuration than the API:

1. **Grant Role Access to Program**:
   - Go to: Maintenance → Program → Route Assignment Program 1
   - Click "Sharing settings"
   - Add "Delivery Driver" role with "Can view and edit data" permission
   - Save

2. **Check Program Configuration**:
   - Verify "Program type" is "Event program"
   - Verify "Without registration" is checked
   - Assign organization units: TRK-001, TRK-002, TRK-003

3. **Create User Group** (Recommended):
   - Go to: Users → User Group
   - Create "Delivery Drivers" group
   - Add testdriver to the group
   - Share the program with this group instead of individual users

4. **Test Access**:
   - Login as testdriver in DHIS2 Web UI
   - Go to Event Capture app
   - Select "Route Assignment Program 1"
   - Select TRK-001 organization unit
   - You should see the test route

### Option 3: Recreate Program from Scratch (Nuclear Option)
If permissions are too broken:

1. Delete current program nnYQNh2XW8m via Web UI
2. Create new Event Program via Web UI:
   - Name: "Route Assignment Program 2"
   - Type: Event (WITHOUT_REGISTRATION)
   - Assign data elements during creation
   - Assign organization units during creation
   - Set sharing to public or specific user groups
3. Note new program ID
4. Update Android app RouteRepository.kt with new program ID
5. Create test routes with admin account
6. Test with testdriver

### Option 4: Use Different DHIS2 Version
DHIS2 2.41.4.2 might have specific bugs with Event Program permissions. Consider:
- Upgrading to latest 2.41.x patch
- Or use DHIS2 2.40 LTS
- Or use DHIS2 2.42 (latest)

## Current Configuration

### Program Details
- **ID**: nnYQNh2XW8m
- **Name**: Route Assignment Program 1
- **Type**: WITHOUT_REGISTRATION
- **Stage ID**: hYmuhfhaqoH
- **Public Access**: rwr----- (metadata rw, data r)

### User Details
- **Username**: testdriver
- **Password**: Test123!
- **ID**: GxTRN2Ap3xl
- **Role**: Delivery Driver (gczXxpLc9RY)
- **Org Unit**: uMce0FFzxd0 (TRK-001)

### Role Authorities
- F_PROGRAM_EVENT_ADD
- F_PROGRAM_EVENT_UPDATE
- F_TRACKED_ENTITY_INSTANCE_ADD
- F_TRACKED_ENTITY_INSTANCE_SEARCH
- F_VIEW_EVENT_ANALYTICS
- M_dhis-web-dashboard
- F_PROGRAMSTAGE_ADD
- F_PROGRAM_ENROLLMENT
- F_UNCOMPLETED_EVENT_EDIT
- F_PROGRAM_PUBLIC_ADD

## Next Steps

**Recommended Immediate Action**: Use **Option 1** (admin account) to test the Android app functionality first. Once the app is working properly, then tackle the multi-user permissions issue with **Option 2** or **Option 3**.

The core Android app functionality (route fetching, GPS verification, delivery capture) should be tested and verified before spending more time on DHIS2 permissions configuration.
