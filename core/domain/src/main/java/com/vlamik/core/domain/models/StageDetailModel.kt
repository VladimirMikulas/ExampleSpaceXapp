package com.vlamik.core.domain.models

data class StageDetailModel(
    val reusable: Boolean,
    val engines: Int,
    val fuelAmountTons: Double,
    val burnTimeSEC: Int,
)