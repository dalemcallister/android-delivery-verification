package com.connexi.deliveryverification.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    val id: String,
    val username: String,
    val firstName: String,
    val surname: String,
    val email: String?,
    val phoneNumber: String?,
    val organisationUnits: List<OrgUnitDto>,
    val attributeValues: List<AttributeValueDto>? = null
) {
    fun getAttribute(code: String): String? {
        return attributeValues
            ?.find { it.attribute.code == code }
            ?.value
    }

    fun getFullName(): String = "$firstName $surname"

    fun getAssignedTruckOrgUnit(): OrgUnitDto? {
        // Return the first orgUnit (should be the truck)
        return organisationUnits.firstOrNull()
    }
}

data class OrgUnitDto(
    val id: String,
    val name: String,
    val code: String? = null
)

data class AttributeValueDto(
    val attribute: AttributeDto,
    val value: String
)

data class AttributeDto(
    val id: String,
    val code: String
)
