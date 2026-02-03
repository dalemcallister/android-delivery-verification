package com.connexi.deliveryverification.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EventDto(
    @SerializedName("event")
    val event: String? = null,
    @SerializedName("program")
    val program: String,
    @SerializedName("programStage")
    val programStage: String,
    @SerializedName("orgUnit")
    val orgUnit: String,
    @SerializedName("eventDate")
    val eventDate: String,
    @SerializedName("status")
    val status: String = "COMPLETED",
    @SerializedName("coordinate")
    val coordinate: CoordinateDto? = null,
    @SerializedName("dataValues")
    val dataValues: List<DataValueDto>
)

data class DataValueDto(
    @SerializedName("dataElement")
    val dataElement: String,
    @SerializedName("value")
    val value: String
)

data class CoordinateDto(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double
)

data class EventsResponse(
    @SerializedName("httpStatus")
    val httpStatus: String? = null,
    @SerializedName("httpStatusCode")
    val httpStatusCode: Int? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("response")
    val response: EventImportResponse? = null
)

data class EventImportResponse(
    @SerializedName("imported")
    val imported: Int = 0,
    @SerializedName("updated")
    val updated: Int = 0,
    @SerializedName("deleted")
    val deleted: Int = 0,
    @SerializedName("ignored")
    val ignored: Int = 0,
    @SerializedName("importSummaries")
    val importSummaries: List<ImportSummary>? = null
)

data class ImportSummary(
    @SerializedName("status")
    val status: String,
    @SerializedName("reference")
    val reference: String? = null,
    @SerializedName("description")
    val description: String? = null
)
