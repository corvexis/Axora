package frb.axeron.manager.ui.screen

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.SearchAppBar
import frb.axeron.manager.ui.util.LocalBottomBarHidden
import frb.axeron.manager.ui.viewmodel.DisableAppsViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DisableAppsScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val viewModel = viewModelGlobal.disableAppsViewModel
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val bottomBarHidden = LocalBottomBarHidden.current

    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (currIndex, currOffset) ->
                val isScrollingDown = currIndex > lastIndex ||
                        (currIndex == lastIndex && currOffset > lastOffset + 4)
                val isScrollingUp = currIndex < lastIndex ||
                        (currIndex == lastIndex && currOffset < lastOffset - 4)

                when {
                    isScrollingDown && !bottomBarHidden.value -> bottomBarHidden.value = true
                    isScrollingUp && bottomBarHidden.value -> bottomBarHidden.value = false
                }
                lastIndex = currIndex
                lastOffset = currOffset
            }
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.apps),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                searchText = viewModel.search,
                onSearchTextChange = { viewModel.search = it },
                onClearClick = { viewModel.search = "" },
                scrollBehavior = scrollBehavior,
                action = {
                    IconButton(
                        onClick = { viewModel.rescan() },
                        enabled = !viewModel.loading
                    ) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppFilterChips(viewModel)
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    viewModel.loading && viewModel.filteredEntries.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    viewModel.filteredEntries.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.apps_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 24.dp)
                        )
                    }

                    else -> {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            items(
                                viewModel.filteredEntries,
                                key = { it.packageName }
                            ) { app ->
                                AppListItem(
                                    app = app,
                                    enabled = viewModel.busyPackage != app.packageName,
                                    onToggle = { checked -> viewModel.toggle(app, checked) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppFilterChips(viewModel: DisableAppsViewModel) {
    val chips = listOf(
        DisableAppsViewModel.AppFilter.ALL to stringResource(R.string.apps_filter_all),
        DisableAppsViewModel.AppFilter.DISABLED to stringResource(R.string.apps_filter_disabled),
        DisableAppsViewModel.AppFilter.ENABLED to stringResource(R.string.apps_filter_enabled),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (filter, label) ->
            FilterChip(
                selected = viewModel.filter == filter,
                onClick = { viewModel.filter = filter },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun AppListItem(
    app: DisableAppsViewModel.AppEntry,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val disabledLabel = stringResource(R.string.apps_filter_disabled)
    ListItem(
        modifier = Modifier.padding(6.dp),
        headlineContent = {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        supportingContent = {
            Text(
                text = if (app.disabled) "${app.packageName} · $disabledLabel" else app.packageName,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            AppIcon(
                packageName = app.packageName,
                label = app.label,
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
            )
        },
        trailingContent = {
            Switch(
                checked = app.disabled,
                onCheckedChange = onToggle,
                enabled = enabled
            )
        }
    )
}

@Composable
private fun AppIcon(packageName: String, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icon by produceState<Drawable?>(null as Drawable?, packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (e: Throwable) {
                null
            }
        }
    }
    if (icon != null) {
        Image(
            bitmap = icon!!.toBitmap().asImageBitmap(),
            contentDescription = label,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Android, label)
        }
    }
}