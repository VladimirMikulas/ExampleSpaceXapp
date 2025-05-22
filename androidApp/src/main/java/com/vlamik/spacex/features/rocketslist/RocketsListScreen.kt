import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vlamik.core.commons.AppText
import com.vlamik.core.domain.models.RocketListItemModel
import com.vlamik.core.domain.usecase.filtering.FilterValue
import com.vlamik.spacex.R
import com.vlamik.spacex.common.filtering.FilterItem
import com.vlamik.spacex.common.filtering.FilterState
import com.vlamik.spacex.common.utils.preview.DeviceFormatPreview
import com.vlamik.spacex.common.utils.preview.FontScalePreview
import com.vlamik.spacex.common.utils.preview.ThemeModePreview
import com.vlamik.spacex.component.LoadingIndicator
import com.vlamik.spacex.component.appbars.SearchAppBar
import com.vlamik.spacex.component.asString
import com.vlamik.spacex.component.drawer.AppDrawer
import com.vlamik.spacex.features.rocketslist.RocketsListContract
import com.vlamik.spacex.features.rocketslist.RocketsListViewModel
import com.vlamik.spacex.navigation.NavRoutes
import com.vlamik.spacex.theme.SoftGray
import com.vlamik.spacex.theme.TemplateTheme
import kotlinx.coroutines.launch


/**
 * Main screen for displaying the list of rockets.
 * This Composable observes the ViewModel's UI state and reacts to side effects.
 *
 * @param viewModel The [RocketsListViewModel] providing UI state and handling user intents.
 * @param openDetailsClicked Lambda to navigate to rocket details, taking the rocket ID.
 * @param navigateTo Lambda to navigate to a different route using [NavRoutes].
 * @param currentRoute The currently active navigation route, used for drawer selection.
 */

@Composable
fun RocketsListScreen(
    viewModel: RocketsListViewModel,
    openDetailsClicked: (String) -> Unit,
    navigateTo: (NavRoutes) -> Unit,
    currentRoute: NavRoutes = NavRoutes.RocketsList
) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // A side-effect handler that launches a coroutine once and collects effects from the ViewModel.
    // It reacts to navigation, details opening, and drawer opening requests from the ViewModel.
    LaunchedEffect(key1 = Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RocketsListContract.Effect.OpenRocketDetails -> openDetailsClicked(effect.rocketId)
                is RocketsListContract.Effect.NavigateToRoute -> navigateTo(effect.route)
                is RocketsListContract.Effect.OpenDrawer -> {
                    scope.launch { drawerState.open() }
                }
            }
        }
    }

    // The main app drawer component that wraps the content.
    AppDrawer(
        currentRoute = currentRoute,
        onItemSelected = { route ->
            viewModel.processIntent(
                RocketsListContract.Intent.NavigateTo(
                    route
                )
            )
        },
        drawerState = drawerState
    ) {
        RocketsListContent(
            state = state,
            onIntent = viewModel::processIntent
        )
    }
}

/**
 * Displays the main content area of the Rockets List screen, including the search bar and the list itself.
 * It's responsible for showing loading, error, empty, or data states.
 *
 * @param state The current UI state from the ViewModel.
 * @param onIntent Callback to send user intents to the ViewModel.
 */

@Composable
private fun RocketsListContent(
    state: RocketsListContract.State,
    onIntent: (RocketsListContract.Intent) -> Unit
) {
    Scaffold(
        topBar = {
            SearchAppBar(
                title = stringResource(R.string.rockets), // Title of the app bar
                searchText = state.searchQuery, // Current search query
                activeFilters = state.activeFilters, // Currently applied filters
                filters = state.availableFilters, // All available filters
                onSearchTextChange = { query ->
                    onIntent(
                        RocketsListContract.Intent.SearchQueryChanged(
                            query
                        )
                    )
                },
                onFilterValueToggle = { key, value, isSelected ->
                    onIntent(RocketsListContract.Intent.FilterChipToggled(key, value, isSelected))
                },
                onMenuClick = {
                    onIntent(RocketsListContract.Intent.DrawerMenuClicked)
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(RocketsListContract.Intent.RefreshRockets) },
            ) {
                when {
                    state.isLoading -> {
                        LoadingIndicator()
                    }

                    state.error != null -> {
                        ErrorState(
                            errorMessage = state.error,
                            onRetry = { onIntent(RocketsListContract.Intent.RetryLoadRockets) }
                        )
                    }

                    else -> {
                        RocketDataContent(
                            rockets = state.filteredRockets,
                            onDetailsClicked = { rocketId ->
                                onIntent(RocketsListContract.Intent.RocketClicked(rocketId))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable to display an error message and a retry button.
 *
 * @param errorMessage The [AppText] object representing the error message.
 * @param onRetry Callback to be invoked when the retry button is clicked.
 */
@Composable
private fun ErrorState(errorMessage: AppText, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = errorMessage.asString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

/**
 * Displays either a list of rockets or an empty state message.
 *
 * @param rockets The list of [RocketListItemModel] to display.
 * @param onDetailsClicked Callback invoked when a rocket item is clicked, passing its ID.
 */
@Composable
private fun RocketDataContent(
    rockets: List<RocketListItemModel>,
    onDetailsClicked: (String) -> Unit
) {
    if (rockets.isEmpty()) {
        EmptyState()
    } else {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = SoftGray
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                itemsIndexed(rockets) { index, rocket ->
                    RocketsListItem(
                        rocket = rocket,
                        onDetailsClicked = onDetailsClicked
                    )
                    if (index < rockets.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.background,
                            thickness = 2.dp,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable to display a message when no rockets are found (e.g., after filtering/searching).
 */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_rockets_found),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Represents a single item in the rockets list.
 *
 * @param rocket The [RocketListItemModel] containing data for the rocket.
 * @param onDetailsClicked Callback invoked when the item is clicked, passing the rocket's ID.
 */
@Composable
private fun RocketsListItem(
    rocket: RocketListItemModel,
    onDetailsClicked: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClicked(rocket.id) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.rocket),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .padding(end = 16.dp),
            tint = Color.Unspecified
        )
        RocketInfo(
            rocket = rocket,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = stringResource(
                R.string.cd_navigate_to_details,
                rocket.name
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Displays the name and first flight date of a rocket.
 *
 * @param rocket The [RocketListItemModel] containing rocket information.
 * @param modifier Modifier for layout and styling.
 */
@Composable
private fun RocketInfo(rocket: RocketListItemModel, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = rocket.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${stringResource(id = R.string.label_first_flight)} ${rocket.firstFlight}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

val previewRockets = listOf(
    RocketListItemModel(
        id = "1", name = "Falcon 9", firstFlight = "2010-06-04",
        height = 70.0, diameter = 3.7, mass = 549054
    ),
    RocketListItemModel(
        id = "2", name = "Falcon Heavy", firstFlight = "2018-02-06",
        height = 70.0, diameter = 12.2, mass = 1420788
    ),
    RocketListItemModel(
        id = "3", name = "Starship", firstFlight = "2023-04-20",
        height = 120.0, diameter = 9.0, mass = 5000000
    )
)

val previewAvailableFilters = listOf(
    FilterItem(
        key = "name",
        displayName = AppText.from(R.string.filter_name),
        values = listOf(
            FilterValue.ExactMatch(displayName = AppText.dynamic("Falcon 9"), value = "Falcon 9"),
            FilterValue.ExactMatch(
                displayName = AppText.dynamic("Falcon Heavy"),
                value = "Falcon Heavy"
            ),
            FilterValue.ExactMatch(displayName = AppText.dynamic("Starship"), value = "Starship")
        )
    ),
    FilterItem(
        key = "first_flight",
        displayName = AppText.from(R.string.filter_first_flight),
        values = listOf(
            FilterValue.YearRange(displayName = AppText.dynamic("Before 2015"), endYear = 2015),
            FilterValue.YearRange(
                displayName = AppText.dynamic("2015-2020"),
                startYear = 2015,
                endYear = 2020
            ),
            FilterValue.YearRange(displayName = AppText.dynamic("After 2020"), startYear = 2020)
        )
    )
)

@ThemeModePreview
@FontScalePreview
@DeviceFormatPreview
@Composable
private fun RocketsListScreenPreview_DataLoaded() {
    TemplateTheme {
        RocketsListContent(
            state = RocketsListContract.State(
                isLoading = false,
                rockets = previewRockets,
                filteredRockets = previewRockets.take(2),
                availableFilters = previewAvailableFilters,
                searchQuery = "",
                activeFilters = FilterState(
                    selectedFilters = mapOf(
                        "name" to setOf(
                            FilterValue.ExactMatch(
                                displayName = AppText.dynamic("Falcon 9"),
                                value = "Falcon 9"
                            )
                        )
                    )
                )
            ),
            onIntent = {}
        )
    }
}

@ThemeModePreview
@FontScalePreview
@DeviceFormatPreview
@Composable
private fun RocketsListScreenPreview_Loading() {
    TemplateTheme {
        RocketsListContent(
            state = RocketsListContract.State(isLoading = true),
            onIntent = {}
        )
    }
}

@ThemeModePreview
@FontScalePreview
@DeviceFormatPreview
@Composable
private fun RocketsListScreenPreview_Error() {
    TemplateTheme {
        RocketsListContent(
            state = RocketsListContract.State(
                isLoading = false,
                error = AppText.from(R.string.data_error)
            ),
            onIntent = {}
        )
    }
}

@ThemeModePreview
@FontScalePreview
@DeviceFormatPreview
@Composable
private fun RocketsListScreenPreview_Empty() {
    TemplateTheme {
        RocketsListContent(
            state = RocketsListContract.State(
                isLoading = false,
                rockets = emptyList(),
                filteredRockets = emptyList(),
                availableFilters = previewAvailableFilters,
                searchQuery = "Non-existent rocket"
            ),
            onIntent = {}
        )
    }
}