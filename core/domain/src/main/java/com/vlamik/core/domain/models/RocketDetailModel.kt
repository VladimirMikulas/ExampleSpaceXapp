package com.vlamik.core.domain.models

data class RocketDetailModel(
    val name: String,
    val description: String,
    val height: Double,
    val diameter: Double,
    val mass: Int,
    val firstStage: StageDetailModel,
    val secondStage: StageDetailModel,
    val images: List<String>

)