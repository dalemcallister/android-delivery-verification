package com.connexi.deliveryverification.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SystemInfoDto(
    @SerializedName("version")
    val version: String,
    @SerializedName("revision")
    val revision: String? = null,
    @SerializedName("systemName")
    val systemName: String? = null,
    @SerializedName("contextPath")
    val contextPath: String? = null
)
