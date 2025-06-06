package com.vlamik.spacex

import com.vlamik.core.domain.models.RocketListItemModel
import com.vlamik.core.domain.repository.RocketsRepository
import com.vlamik.core.domain.usecase.ApplyRocketsFiltersUseCase
import com.vlamik.core.domain.usecase.ApplyRocketsSearchUseCase
import com.vlamik.core.domain.usecase.GetRocketsListUseCase
import com.vlamik.core.domain.usecase.filtering.FilterConstants
import com.vlamik.core.domain.usecase.filtering.FilterValue
import com.vlamik.spacex.features.rocketslist.RocketsListContract
import com.vlamik.spacex.features.rocketslist.RocketsListViewModel
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RocketsListViewModelUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockRocketsRepository = mockk<RocketsRepository>()
    private val getRocketsListUseCase = GetRocketsListUseCase(mockRocketsRepository)
    private val applyRocketsSearchUseCase = ApplyRocketsSearchUseCase()
    private val applyRocketsFiltersUseCase = ApplyRocketsFiltersUseCase()

    private lateinit var viewModel: RocketsListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Mock repository for initial state
        coEvery { mockRocketsRepository.getRocketsList(any()) } returns Result.success(emptyList())

        viewModel = RocketsListViewModel(
            getRocketsListUseCase,
            applyRocketsSearchUseCase,
            applyRocketsFiltersUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `data loads successfully and updates state`() = runTest {
        // Arrange
        // Now we return RocketListItemModel, not RocketDto
        val testRockets = listOf(createTestRocketListItemModel(name = "TestRocket1"))
        coEvery { mockRocketsRepository.getRocketsList(any()) } returns Result.success(testRockets)

        // Act: Explicitly process the LoadRockets intent.
        viewModel.processIntent(RocketsListContract.Intent.LoadRockets)

        // Allow all pending coroutines to complete, including the data fetching and StateFlow update.
        advanceUntilIdle()

        // Assert
        val currentState = viewModel.uiState.first() // Get the latest emitted state
        assertFalse(currentState.isLoading)
        assertEquals(1, currentState.rockets.size)
        assertEquals("TestRocket1", currentState.rockets.first().name)
        assertTrue(currentState.availableFilters.isNotEmpty())
    }

    @Test
    fun `error during load sets error state`() = runTest {
        // Arrange
        val errorMessage = "Failed to load data!"
        coEvery { mockRocketsRepository.getRocketsList(any()) } returns Result.failure(
            RuntimeException(
                errorMessage
            )
        )

        // Act: Let the initial load attempt complete (which will fail)
        advanceUntilIdle()

        // Assert
        val currentState = viewModel.uiState.first()
        assertFalse(currentState.isLoading)
        assertTrue(currentState.rockets.isEmpty())
    }

    @Test
    fun `search query filters rockets by name`() = runTest {
        // Arrange
        // Using createTestRocketListItemModel
        val rockets = listOf(
            createTestRocketListItemModel(name = "Falcon 9"),
            createTestRocketListItemModel(name = "Starship")
        )
        coEvery { mockRocketsRepository.getRocketsList(any()) } returns Result.success(rockets)
        viewModel.processIntent(RocketsListContract.Intent.LoadRockets)
        advanceUntilIdle() // Ensure rockets are loaded first

        // Act: Search for "Falcon"
        viewModel.processIntent(RocketsListContract.Intent.SearchQueryChanged("Falcon"))
        advanceUntilIdle()

        // Assert
        val currentState = viewModel.uiState.first()
        assertEquals("Falcon", currentState.searchQuery)
        assertEquals(1, currentState.filteredRockets.size)
        assertEquals("Falcon 9", currentState.filteredRockets.first().name)
    }

    @Test
    fun `filter chip toggles correctly`() = runTest {
        // Arrange
        val rockets = listOf(
            // Important: firstFlight must be in "dd.MM.yyyy" format for RocketListItemModel
            createTestRocketListItemModel(name = "Old Rocket", firstFlight = "01.01.2000"),
            createTestRocketListItemModel(name = "New Rocket", firstFlight = "01.01.2020")
        )
        coEvery { mockRocketsRepository.getRocketsList(any()) } returns Result.success(rockets)
        viewModel.processIntent(RocketsListContract.Intent.LoadRockets)
        advanceUntilIdle() // Ensure rockets and filters are loaded

        assertEquals(2, viewModel.uiState.first().filteredRockets.size) // All rockets initially

        // Find the "First Flight" filter
        val firstFlightFilters = viewModel.uiState.first().availableFilters
            .first { it.key == FilterConstants.KEY_FIRST_FLIGHT }.values
        // Find a filter that would select "Old Rocket" (e.g., 'before a certain year')
        // We need to use the getFirstFlightDateFormat function for consistent date format
        val oldRocketFilter = firstFlightFilters.filterIsInstance<FilterValue.YearRange>()
            .first { it.startYear == null && it.endYear != null } // This finds the "Before {Year}" filter type

        // Act 1: Select the filter
        viewModel.processIntent(
            RocketsListContract.Intent.FilterChipToggled(
                FilterConstants.KEY_FIRST_FLIGHT,
                oldRocketFilter,
                true
            )
        )
        advanceUntilIdle()

        // Assert 1: Only "Old Rocket" remains
        val stateAfterSelect = viewModel.uiState.first()
        assertEquals(1, stateAfterSelect.filteredRockets.size)
        assertEquals("Old Rocket", stateAfterSelect.filteredRockets.first().name)
        assertTrue(
            stateAfterSelect.activeFilters.selectedFilters[FilterConstants.KEY_FIRST_FLIGHT]?.contains(
                oldRocketFilter
            ) == true
        )

        // Act 2: Deselect the filter
        viewModel.processIntent(
            RocketsListContract.Intent.FilterChipToggled(
                FilterConstants.KEY_FIRST_FLIGHT,
                oldRocketFilter,
                false
            )
        )
        advanceUntilIdle()

        // Assert 2: Both rocket models are back
        val stateAfterDeselect = viewModel.uiState.first()
        assertEquals(2, stateAfterDeselect.filteredRockets.size)
        assertTrue(stateAfterDeselect.activeFilters.selectedFilters[FilterConstants.KEY_FIRST_FLIGHT]?.isEmpty() == true)
    }

    private fun createTestRocketListItemModel(
        id: String = "rocket_id",
        name: String = "Test Rocket",
        // Important: firstFlight must be in "dd.MM.yyyy" format for RocketListItemModel
        firstFlight: String = "24.03.2006",
        height: Double = 33.5,
        diameter: Double = 3.66,
        mass: Int = 301460
    ): RocketListItemModel {
        return RocketListItemModel(
            id = id,
            name = name,
            firstFlight = firstFlight,
            height = height,
            diameter = diameter,
            mass = mass
        )
    }
}