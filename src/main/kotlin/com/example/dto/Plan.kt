package com.example.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.*

@Serializable
data class Plan (
    @SerialName("name") var name : String?,
    @SerialName("space") var space : Int?,
    @SerialName("private_repos") var privateRepos : Int?,
    @SerialName("collaborators") var collaborators : Int?
)
