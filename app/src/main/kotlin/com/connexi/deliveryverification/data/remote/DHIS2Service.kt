package com.connexi.deliveryverification.data.remote

import com.connexi.deliveryverification.data.remote.dto.*
import com.google.gson.annotations.SerializedName
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

    @GET("api/dataValueSets")
    suspend fun getDataValueSets(
        @Query("dataElement") dataElements: String,
        @Query("period") period: String,
        @Query("orgUnit") orgUnit: String? = null,
        @Query("orgUnitMode") orgUnitMode: String? = null,
        @Query("children") children: Boolean = true
    ): Response<DataValueSetsResponse>
}

data class EventsListResponse(
    @SerializedName("events")
    val events: List<EventDto>
)

data class EventsWrapper(
    @SerializedName("events")
    val events: List<EventDto>
)

data class DataValueSetsResponse(
    @SerializedName("dataValues")
    val dataValues: List<DataValue>
)

data class DataValue(
    @SerializedName("dataElement")
    val dataElement: String,
    @SerializedName("period")
    val period: String,
    @SerializedName("orgUnit")
    val orgUnit: String,
    @SerializedName("categoryOptionCombo")
    val categoryOptionCombo: String? = null,
    @SerializedName("attributeOptionCombo")
    val attributeOptionCombo: String? = null,
    @SerializedName("value")
    val value: String,
    @SerializedName("storedBy")
    val storedBy: String? = null,
    @SerializedName("created")
    val created: String? = null,
    @SerializedName("lastUpdated")
    val lastUpdated: String? = null,
    @SerializedName("comment")
    val comment: String? = null,
    @SerializedName("followup")
    val followup: Boolean? = null
)
