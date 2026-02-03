package com.connexi.deliveryverification.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrganisationUnitDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("coordinates")
    val coordinates: String? = null,
    @SerializedName("geometry")
    val geometry: GeometryDto? = null
)

data class GeometryDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("coordinates")
    val coordinates: List<Double>
)

data class OrganisationUnitsResponse(
    @SerializedName("organisationUnits")
    val organisationUnits: List<OrganisationUnitDto>
)
