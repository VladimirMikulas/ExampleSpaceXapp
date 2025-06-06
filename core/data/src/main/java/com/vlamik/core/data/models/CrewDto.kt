package com.vlamik.core.data.models

import com.vlamik.core.domain.models.CrewListItemModel
import kotlinx.serialization.Serializable


@Serializable
data class CrewDto(
    val name: String? = null,
    val agency: String? = null,
    val image: String? = null,
    val wikipedia: String? = null,
    val launches: List<String>? = null,
    val status: String? = null,
    val id: String? = null
)


fun CrewDto.toCrewListItemModel(): CrewListItemModel = CrewListItemModel(
    name = name.orEmpty(),
    agency = agency.orEmpty(),
    wikipedia = wikipedia.orEmpty(),
    status = status.orEmpty(),
)
