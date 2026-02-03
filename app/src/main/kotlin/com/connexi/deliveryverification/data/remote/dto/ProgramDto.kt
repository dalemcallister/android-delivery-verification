package com.connexi.deliveryverification.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProgramDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("programType")
    val programType: String,
    @SerializedName("programStages")
    val programStages: List<ProgramStageDto>? = null
)

data class ProgramStageDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("programStageDataElements")
    val programStageDataElements: List<ProgramStageDataElementDto>? = null
)

data class ProgramStageDataElementDto(
    @SerializedName("dataElement")
    val dataElement: DataElementDto
)

data class DataElementDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("valueType")
    val valueType: String
)

data class ProgramsResponse(
    @SerializedName("programs")
    val programs: List<ProgramDto>
)
