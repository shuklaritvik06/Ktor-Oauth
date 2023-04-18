package com.example.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleUser(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("given_name") val givenName: String,
    @SerialName("family_name") val familyName: String,
    @SerialName("picture") val picture: String,
    @SerialName("locale") val locale: String
)