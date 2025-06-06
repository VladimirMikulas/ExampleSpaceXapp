package com.vlamik.core.data.models

import com.vlamik.core.domain.models.RocketDetailModel
import com.vlamik.core.domain.models.RocketListItemModel
import com.vlamik.core.domain.models.StageDetailModel
import com.vlamik.core.domain.models.datePattern
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Serializable
data class RocketDto(
    val height: Dimensions? = null,
    val diameter: Dimensions? = null,
    val mass: Mass? = null,

    @SerialName("first_stage")
    val firstStage: FirstStage,

    @SerialName("second_stage")
    val secondStage: SecondStage,

    val engines: Engines? = null,

    @SerialName("landing_legs")
    val landingLegs: LandingLegs? = null,

    @SerialName("payload_weights")
    val payloadWeights: List<PayloadWeight>? = null,

    @SerialName("flickr_images")
    val flickrImages: List<String>? = null,

    val name: String? = null,
    val type: String? = null,
    val active: Boolean? = null,
    val stages: Long? = null,
    val boosters: Long? = null,

    @SerialName("cost_per_launch")
    val costPerLaunch: Long? = null,

    @SerialName("success_rate_pct")
    val successRatePct: Long? = null,

    @SerialName("first_flight")
    val firstFlight: String? = null,

    val country: String? = null,
    val company: String? = null,
    val wikipedia: String? = null,
    val description: String? = null,
    val id: String? = null
)

@Serializable
data class Dimensions(
    val meters: Double? = null,
    val feet: Double? = null
)

@Serializable
data class Engines(
    val isp: ISP? = null,

    @SerialName("thrust_sea_level")
    val thrustSeaLevel: Thrust? = null,

    @SerialName("thrust_vacuum")
    val thrustVacuum: Thrust? = null,

    val number: Long? = null,
    val type: String? = null,
    val version: String? = null,
    val layout: String? = null,

    @SerialName("engine_loss_max")
    val engineLossMax: Long? = null,

    @SerialName("propellant_1")
    val propellant1: String? = null,

    @SerialName("propellant_2")
    val propellant2: String? = null,

    @SerialName("thrust_to_weight")
    val thrustToWeight: Double? = null
)

@Serializable
data class ISP(
    @SerialName("sea_level")
    val seaLevel: Long? = null,

    val vacuum: Long? = null
)

@Serializable
data class Thrust(
    val kN: Long? = null,
    val lbf: Long? = null
)

@Serializable
data class FirstStage(
    @SerialName("thrust_sea_level")
    val thrustSeaLevel: Thrust? = null,

    @SerialName("thrust_vacuum")
    val thrustVacuum: Thrust? = null,

    val reusable: Boolean? = null,
    val engines: Long? = null,

    @SerialName("fuel_amount_tons")
    val fuelAmountTons: Double? = null,

    @SerialName("burn_time_sec")
    val burnTimeSEC: Long? = null
)

@Serializable
data class LandingLegs(
    val number: Long? = null,
    val material: String? = null
)

@Serializable
data class Mass(
    val kg: Double? = null,
    val lb: Double? = null
)

@Serializable
data class PayloadWeight(
    val id: String? = null,
    val name: String? = null,
    val kg: Long? = null,
    val lb: Long? = null
)

@Serializable
data class SecondStage(
    val thrust: Thrust? = null,
    val payloads: Payloads? = null,
    val reusable: Boolean? = null,
    val engines: Long? = null,

    @SerialName("fuel_amount_tons")
    val fuelAmountTons: Double? = null,

    @SerialName("burn_time_sec")
    val burnTimeSEC: Long? = null
)

@Serializable
data class Payloads(
    @SerialName("composite_fairing")
    val compositeFairing: CompositeFairing? = null,

    @SerialName("option_1")
    val option1: String? = null
)

@Serializable
data class CompositeFairing(
    val height: Dimensions? = null,
    val diameter: Dimensions? = null
)


fun RocketDto.toRocketDetailModel(): RocketDetailModel = RocketDetailModel(
    name = name.orEmpty(),
    description = description.orEmpty(),
    height = height?.meters ?: -1.0,
    diameter = diameter?.meters ?: -1.0,
    mass = mass?.kg?.toInt() ?: -1,
    firstStage = firstStage.toStageDetailModel(),
    secondStage = secondStage.toStageDetailModel(),
    images = flickrImages.orEmpty()
)


fun RocketDto.toRocketListItemModel(): RocketListItemModel = RocketListItemModel(
    id = id.orEmpty(),
    name = name.orEmpty(),
    firstFlight = getFirstFlightDateFormat(firstFlight.orEmpty()),
    height = height?.meters ?: -1.0,
    diameter = diameter?.meters ?: -1.0,
    mass = mass?.kg?.toInt() ?: -1
)

fun getFirstFlightDateFormat(date: String): String {
    val formatter = DateTimeFormatter.ofPattern(datePattern)
    return LocalDate.parse(date).format(formatter)
}


fun FirstStage.toStageDetailModel(): StageDetailModel = StageDetailModel(
    reusable = reusable ?: false,
    engines = engines?.toInt() ?: -1,
    fuelAmountTons = fuelAmountTons ?: -1.0,
    burnTimeSEC = burnTimeSEC?.toInt() ?: -1
)

fun SecondStage.toStageDetailModel(): StageDetailModel = StageDetailModel(
    reusable = reusable ?: false,
    engines = engines?.toInt() ?: -1,
    fuelAmountTons = fuelAmountTons ?: -1.0,
    burnTimeSEC = burnTimeSEC?.toInt() ?: -1
)
