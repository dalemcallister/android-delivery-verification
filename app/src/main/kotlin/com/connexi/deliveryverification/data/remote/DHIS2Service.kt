package com.connexi.deliveryverification.data.remote

import com.connexi.deliveryverification.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface DHIS2Service {

    @GET("api/system/info")
    suspend fun getSystemInfo(): Response<SystemInfoDto>

    @GET("api/programs")
    suspend fun getPrograms(
        @Query("fields") fields: String = "id,name,programType,programStages[id,name,programStageDataElements[dataElement[id,name,valueType]]]",
        @Query("paging") paging: Boolean = false
    ): Response<ProgramsResponse>

    @GET("api/programs/{programId}")
    suspend fun getProgram(
        @Path("programId") programId: String,
        @Query("fields") fields: String = "id,name,programType,programStages[id,name]"
    ): Response<ProgramDto>

    @GET("api/organisationUnits")
    suspend fun getOrganisationUnits(
        @Query("fields") fields: String = "id,name,coordinates,geometry",
        @Query("paging") paging: Boolean = false
    ): Response<OrganisationUnitsResponse>

    @GET("api/events")
    suspend fun getEvents(
        @Query("program") programId: String,
        @Query("orgUnit") orgUnitId: String? = null,
        @Query("ouMode") ouMode: String = "ACCESSIBLE",
        @Query("fields") fields: String = "*",
        @Query("paging") paging: Boolean = false
    ): Response<EventsListResponse>

    @POST("api/events")
    suspend fun createEvent(
        @Body event: EventDto
    ): Response<EventsResponse>

    @PUT("api/events/{eventId}")
    suspend fun updateEvent(
        @Path("eventId") eventId: String,
        @Body event: EventDto
    ): Response<EventsResponse>

    @Headers("Content-Type: application/json")
    @POST("api/events")
    suspend fun createEvents(
        @Body events: EventsWrapper
    ): Response<EventsResponse>
}

data class EventsListResponse(
    @SerializedName("events")
    val events: List<EventDto>
)

data class EventsWrapper(
    @SerializedName("events")
    val events: List<EventDto>
)
