package com.vlamik.core.domain.usecase

import com.vlamik.core.domain.models.RocketListItemModel
import com.vlamik.core.domain.usecase.filtering.FilterConstants
import com.vlamik.core.domain.usecase.filtering.FilterValue
import com.vlamik.core.domain.usecase.filtering.RangeFilter
import com.vlamik.core.domain.usecase.filtering.YearFilter

/**
 * UseCase to apply active filters to a list of rockets.
 * This logic operates purely on domain models.
 */
class ApplyRocketsFiltersUseCase {
    /**
     * Filters a list of rockets based on the selected filter values.
     *
     * @param rockets The list of rockets to filter.
     * @param selectedFilters A map where the key is the filter key (String)
     * and the value is a set of selected FilterValue objects for that filter.
     * @return A new list containing only the rockets that match the filter criteria.
     */
    operator fun invoke(
        rockets: List<RocketListItemModel>,
        selectedFilters: Map<String, Set<FilterValue>> // Now directly accepts FilterValue
    ): List<RocketListItemModel> {
        if (selectedFilters.isEmpty() || selectedFilters.all { it.value.isEmpty() }) {
            return rockets // If no filters are selected or all filter sets are empty, return the original list
        }

        return rockets.filter { rocket ->
            // Filtering logic based on selected FilterValue
            // All selected filters must match for a rocket to be included
            selectedFilters.all { (filterKey, selectedFilterValues) -> // Iterate over Map<String, Set<FilterValue>>
                if (selectedFilterValues.isEmpty()) {
                    true // No values selected for this filter key, so this filter type doesn't restrict the result
                } else {
                    // Check if the rocket matches at least one of the selected values for this filter key
                    when (filterKey) {
                        FilterConstants.KEY_NAME ->
                            // For KEY_NAME, we expect FilterValue.ExactMatch.
                            // Check if the rocket's name matches the value in any selected ExactMatch.
                            selectedFilterValues.filterIsInstance<FilterValue.ExactMatch>()
                                .any { it.value.equals(rocket.name, ignoreCase = true) }

                        FilterConstants.KEY_FIRST_FLIGHT ->
                            // For KEY_FIRST_FLIGHT, we expect FilterValue.YearRange.
                            // Check if the rocket's year matches any of the selected YearRange values.
                            selectedFilterValues.filterIsInstance<FilterValue.YearRange>()
                                .any { yearRange ->
                                    YearFilter.matches(rocket.firstFlight, yearRange)
                                }

                        FilterConstants.KEY_HEIGHT ->
                            // For KEY_HEIGHT, we expect FilterValue.Range.
                            // Check if the rocket's height matches any of the selected Range values.
                            selectedFilterValues.filterIsInstance<FilterValue.Range>()
                                .any { range ->
                                    RangeFilter.matches(rocket.height, range)
                                }

                        FilterConstants.KEY_DIAMETER ->
                            // For KEY_DIAMETER, we expect FilterValue.Range.
                            // Check if the rocket's diameter matches any of the selected Range values.
                            selectedFilterValues.filterIsInstance<FilterValue.Range>()
                                .any { range ->
                                    RangeFilter.matches(rocket.diameter, range)
                                }

                        FilterConstants.KEY_MASS ->
                            // For KEY_MASS, we expect FilterValue.Range (for Double).
                            // Check if the rocket's mass matches any of the selected Range values.
                            selectedFilterValues.filterIsInstance<FilterValue.Range>()
                                .any { range ->
                                    RangeFilter.matches(rocket.mass.toDouble(), range)
                                }

                        else -> true // Unknown filter key, assume it matches
                    }
                }
            }
        }
    }
}