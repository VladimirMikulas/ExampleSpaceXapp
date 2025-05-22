package com.vlamik.spacex.features.rocketslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlamik.core.commons.AppText
import com.vlamik.core.commons.PublishFlow
import com.vlamik.core.domain.models.RocketListItemModel
import com.vlamik.core.domain.usecase.ApplyRocketsFiltersUseCase
import com.vlamik.core.domain.usecase.ApplyRocketsSearchUseCase
import com.vlamik.core.domain.usecase.GetRocketsListUseCase
import com.vlamik.core.domain.usecase.filtering.FilterConstants
import com.vlamik.core.domain.usecase.filtering.FilterUtils
import com.vlamik.core.domain.usecase.filtering.FilterValue
import com.vlamik.spacex.R
import com.vlamik.spacex.common.filtering.FilterItem
import com.vlamik.spacex.common.filtering.FilterState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RocketsListViewModel @Inject constructor(
    private val getRocketsListUseCase: GetRocketsListUseCase,
    private val applyRocketsSearch: ApplyRocketsSearchUseCase,
    private val applyRocketsFilters: ApplyRocketsFiltersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RocketsListContract.State())
    val uiState = _uiState.asStateFlow()

    private val _effect = PublishFlow<RocketsListContract.Effect>()
    val effect: Flow<RocketsListContract.Effect> = _effect

    init {
        processIntent(RocketsListContract.Intent.LoadRockets)
    }

    /**
     * Processes incoming [RocketsListContract.Intent]s from the UI.
     * This is the single entry point for UI events.
     */
    fun processIntent(intent: RocketsListContract.Intent) {
        viewModelScope.launch {
            when (intent) {
                is RocketsListContract.Intent.LoadRockets -> loadRockets(refresh = false)
                is RocketsListContract.Intent.RefreshRockets -> refreshRockets()
                is RocketsListContract.Intent.SearchQueryChanged -> updateSearchQuery(intent.query)
                is RocketsListContract.Intent.FilterChipToggled -> handleFilterChipToggle(
                    intent.filterKey,
                    intent.filterValue,
                    intent.isSelected
                )

                is RocketsListContract.Intent.RocketClicked -> _effect.tryEmit(
                    RocketsListContract.Effect.OpenRocketDetails(intent.rocketId)
                )

                is RocketsListContract.Intent.NavigateTo -> _effect.tryEmit(
                    RocketsListContract.Effect.NavigateToRoute(intent.route)
                )

                is RocketsListContract.Intent.DrawerMenuClicked -> _effect.tryEmit(
                    RocketsListContract.Effect.OpenDrawer
                )
                is RocketsListContract.Intent.RetryLoadRockets -> loadRockets(refresh = false)
                is RocketsListContract.Intent.ConsumeError -> _uiState.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Loads rockets, handling initial load or data refresh.
     * Updates loading states and applies current search/filters after fetching.
     */
    private suspend fun loadRockets(refresh: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = !refresh, // Only show full loading spinner for initial load
                isRefreshing = refresh, // Show refresh indicator for pull-to-refresh
                error = null
            )
        }

        getRocketsListUseCase(refresh)
            .onSuccess { rockets ->
                val availableFilters = createAvailableFilters(rockets)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        isRefreshing = false,
                        rockets = rockets, // Store original rockets
                        // Apply existing search query and active filters to the newly loaded data
                        filteredRockets = applySearchAndFilters(
                            rockets,
                            currentState.searchQuery,
                            currentState.activeFilters
                        ),
                        availableFilters = availableFilters,
                        error = null
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        // Provide a user-friendly error message
                        error = throwable.message?.let { AppText.dynamic(it) }
                            ?: AppText.from(R.string.data_error)
                    )
                }
            }
    }

    /**
     * Triggers a refresh of rockets, also resetting search and filters.
     */
    private suspend fun refreshRockets() {
        // Reset search and filters to initial state upon explicit refresh
        _uiState.update {
            it.copy(
                searchQuery = "",
                activeFilters = FilterState()
            )
        }
        loadRockets(refresh = true)
    }

    /**
     * Updates the search query and re-applies search/filters to the rocket list.
     */
    private fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                // Re-apply search and filters with the new query
                filteredRockets = applySearchAndFilters(
                    currentState.rockets,
                    query,
                    currentState.activeFilters
                )
            )
        }
    }

    /**
     * Handles the toggling of a filter chip, updating the `activeFilters` state.
     * This is where the core filter state manipulation now happens.
     */
    private fun handleFilterChipToggle(
        filterKey: String,
        filterValue: FilterValue,
        isSelected: Boolean
    ) {
        _uiState.update { currentState ->
            // Create a mutable copy of the current selected filters map
            val updatedSelectedFilters = currentState.activeFilters.selectedFilters.toMutableMap()
            // Get the current set of selected values for this specific filter key
            val currentSelectedValues =
                updatedSelectedFilters[filterKey]?.toMutableSet() ?: mutableSetOf()

            // Add or remove the clicked filter value based on its new selection state
            if (isSelected) {
                currentSelectedValues.add(filterValue)
            } else {
                currentSelectedValues.remove(filterValue)
            }
            // Update the map with the modified set of values for this filter key
            updatedSelectedFilters[filterKey] = currentSelectedValues.toSet()

            // Create a new FilterState with the updated selected filters
            val newActiveFilters =
                currentState.activeFilters.copy(selectedFilters = updatedSelectedFilters.toMap())

            // Update the UI state with the new active filters and re-apply search/filters
            currentState.copy(
                activeFilters = newActiveFilters,
                filteredRockets = applySearchAndFilters(
                    currentState.rockets,
                    currentState.searchQuery,
                    newActiveFilters // Use the newly calculated active filters for processing
                )
            )
        }
    }

    /**
     * Applies both search and filters using the domain UseCases.
     * This function now directly passes presentation `FilterValue`s to the UseCase.
     */
    private fun applySearchAndFilters(
        rockets: List<RocketListItemModel>,
        query: String,
        filters: FilterState
    ): List<RocketListItemModel> {
        val searchedRockets = applyRocketsSearch(rockets, query)

        return applyRocketsFilters(searchedRockets, filters.selectedFilters)
    }

    /**
     * Creates the list of available filters for the UI based on the provided rockets.
     */
    private fun createAvailableFilters(rockets: List<RocketListItemModel>): List<FilterItem> {
        if (rockets.isEmpty()) return emptyList()
        return listOf(
            createNameFilter(rockets),
            createFirstFlightFilter(rockets),
            createHeightFilter(rockets),
            createDiameterFilter(rockets),
            createMassFilter(rockets)
        )
    }

    /**
     * Creates the `FilterItem` for the "Name" filter.
     * Creates `FilterValue.ExactMatch` with `AppText`.
     */
    private fun createNameFilter(rockets: List<RocketListItemModel>): FilterItem {
        val names = rockets.map { it.name }.distinct().sorted()
        val filterValues = names.map { name ->
            FilterValue.ExactMatch(displayName = AppText.dynamic(name), value = name)
        }
        return FilterItem(
            key = FilterConstants.KEY_NAME,
            displayName = AppText.from(R.string.filter_name),
            values = filterValues
        )
    }

    /**
     * Creates the `FilterItem` for the "First Flight" filter.
     * Creates `FilterValue.YearRange` with `AppText` based on range info.
     */
    private fun createFirstFlightFilter(rockets: List<RocketListItemModel>): FilterItem {
        val dateStrings = rockets.map { it.firstFlight }
        val yearRangeInfo = FilterUtils.generateYearRangeInfo(dateStrings)

        val filterValues = yearRangeInfo?.let { info ->
            listOf(
                FilterValue.YearRange(
                    displayName = AppText.from(
                        R.string.filter_before,
                        (info.min + info.step).toString()
                    ),
                    endYear = info.min + info.step
                ),
                FilterValue.YearRange(
                    displayName = AppText.from(
                        R.string.filter_range,
                        (info.min + info.step).toString(),
                        (info.max - info.step).toString()
                    ),
                    startYear = info.min + info.step,
                    endYear = info.max - info.step
                ),
                FilterValue.YearRange(
                    displayName = AppText.from(
                        R.string.filter_after,
                        (info.max - info.step).toString()
                    ),
                    startYear = info.max - info.step
                )
            )
        } ?: emptyList()

        return FilterItem(
            key = FilterConstants.KEY_FIRST_FLIGHT,
            displayName = AppText.from(R.string.filter_first_flight),
            values = filterValues,
            extraParams = mapOf(FilterConstants.PARAM_UNIT to AppText.from(R.string.unit_year))
        )
    }

    /**
     * Creates the `FilterItem` for the "Height" filter.
     * Creates `FilterValue.Range` with `AppText` based on domain range info.
     */
    private fun createHeightFilter(rockets: List<RocketListItemModel>): FilterItem {
        val heights = rockets.map { it.height }
        val rangeInfo = FilterUtils.generateDoubleRangeInfo(heights)

        val filterValues = rangeInfo?.let { info ->
            listOf(
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_under,
                        "%.1f".format(info.min + info.step)
                    ),
                    end = info.min + info.step
                ),
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_range,
                        "%.1f".format(info.min + info.step),
                        "%.1f".format(info.max - info.step)
                    ),
                    start = info.min + info.step,
                    end = info.max - info.step
                ),
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_over,
                        "%.1f".format(info.max - info.step)
                    ),
                    start = info.max - info.step
                )
            )
        } ?: emptyList()

        return FilterItem(
            key = FilterConstants.KEY_HEIGHT,
            displayName = AppText.from(R.string.filter_height),
            values = filterValues,
            extraParams = mapOf(FilterConstants.PARAM_UNIT to AppText.from(R.string.unit_meters))
        )
    }

    /**
     * Creates the `FilterItem` for the "Diameter" filter.
     * Creates `FilterValue.Range` with `AppText` based on domain range info.
     */
    private fun createDiameterFilter(rockets: List<RocketListItemModel>): FilterItem {
        val diameters = rockets.map { it.diameter }
        val rangeInfo = FilterUtils.generateDoubleRangeInfo(diameters)

        val filterValues = rangeInfo?.let { info ->
            listOf(
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_under,
                        "%.1f".format(info.min + info.step)
                    ),
                    end = info.min + info.step
                ),
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_range,
                        "%.1f".format(info.min + info.step),
                        "%.1f".format(info.max - info.step)
                    ),
                    start = info.min + info.step,
                    end = info.max - info.step
                ),
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_over,
                        "%.1f".format(info.max - info.step)
                    ),
                    start = info.max - info.step
                )
            )
        } ?: emptyList()

        return FilterItem(
            key = FilterConstants.KEY_DIAMETER,
            displayName = AppText.from(R.string.filter_diameter),
            values = filterValues,
            extraParams = mapOf(FilterConstants.PARAM_UNIT to AppText.from(R.string.unit_meters))
        )
    }

    /**
     * Creates the `FilterItem` for the "Mass" filter.
     * Creates  `FilterValue.Range` with `AppText` based on domain range info.
     */
    private fun createMassFilter(rockets: List<RocketListItemModel>): FilterItem {
        val masses = rockets.map { it.mass.toDouble() }
        val rangeInfo = FilterUtils.generateDoubleRangeInfo(masses)

        val filterValues = rangeInfo?.let { info ->
            listOf(
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_under,
                        "%.1f".format(info.min + info.step)
                    ),
                    end = info.min + info.step
                ),
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_range,
                        "%.1f".format(info.min + info.step),
                        "%.1f".format(info.max - info.step)
                    ),
                    start = info.min + info.step,
                    end = info.max - info.step
                ),
                FilterValue.Range(
                    displayName = AppText.from(
                        R.string.filter_over,
                        "%.1f".format(info.max - info.step)
                    ),
                    start = info.max - info.step
                )
            )
        } ?: emptyList()

        return FilterItem(
            key = FilterConstants.KEY_MASS,
            displayName = AppText.from(R.string.filter_mass),
            values = filterValues,
            extraParams = mapOf(FilterConstants.PARAM_UNIT to AppText.from(R.string.unit_kilograms))
        )
    }
}