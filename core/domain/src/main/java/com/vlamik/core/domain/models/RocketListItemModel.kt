package com.vlamik.core.domain.models

import kotlinx.serialization.Serializable


const val datePattern = "dd.MM.yyyy"

@Serializable
data class RocketListItemModel(
    val id: String,
    val name: String,
    val firstFlight: String,
    val height: Double,
    val diameter: Double,
    val mass: Int,
)