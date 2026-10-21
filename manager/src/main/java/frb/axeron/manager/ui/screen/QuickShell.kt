package frb.axeron.manager.ui.screen

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DoNotTouch
import androidx.compose.material.icons.outlined.Input
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fox2code.androidansi.ktx.parseAsAnsiAnnotatedString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AxSnackBarHost
import frb.axeron.manager.ui.component.CheckBoxText
import frb.axeron.manager.ui.component.KeyEventBlocker
import frb.axeron.manager.ui.component.SettingsItem
import frb.axeron.manager.ui.component.SettingsItemExpanded
import frb.axeron.manager.ui.util.LocalBottomBarHidden
import frb.axeron.manager.ui.util.LocalSnackbarHost
import frb.axeron.manager.ui.util.PrefsEnumHelper
import frb.axeron.manager.ui.viewmodel.QuickShellViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun QuickShellScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    val viewModel = viewModelGlobal.quickShellViewModel
    val running = viewModel.isRunning

    val listState = rememberLazyListState()

    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    // FAB visibility
    val bottomBarHidden = LocalBottomBarHidden.current
    var fabVisible by remember { mutableStateOf(true) }

    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose {
            view.keepScreenOn = false
        }
    }

    LaunchedEffect(listState, viewModel.isRunning) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (!viewModel.isRunning) {  // hanya update FAB kalau sedang tidak running
                    val isScrollingDown = index > previousIndex ||
                        (index == previousIndex && offset > previousScrollOffset + 4)
                    val isScrollingUp = index < previousIndex ||
                        (index == previousIndex && offset < previousScrollOffset - 4)

                    if (isScrollingDown) {
                        fabVisible = false
                        if (!bottomBarHidden.value) bottomBarHidden.value = true
                    } else if (isScrollingUp) {
                        fabVisible = true
                        if (bottomBarHidden.value) bottomBarHidden.value = false
                    } else if (index == 0 && offset == 0 && bottomBarHidden.value) {
                        bottomBarHidden.value = false
                    }
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }

    val scope = rememberCoroutineScope()
    val snackBarHost = LocalSnackbarHost.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    var actionsExpanded by rememberSaveable { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchMatchLines by remember { mutableStateOf<List<Int>>(emptyList()) }
    var searchMatchIndex by remember { mutableIntStateOf(-1) }
    var filterRevision by remember { mutableIntStateOf(0) }

    val visibleTypes = remember(context, filterRevision) {
        QuickShellViewModel.OutputType.entries
            .filter {
                PrefsEnumHelper<QuickShellViewModel.OutputType>("output_")
                    .loadState(context, it, true)
            }
            .toSet()
    }

    fun scrollToLogLine(logIndex: Int) {
        val target = (logIndex + 1).coerceIn(0, viewModel.logs.size)
        scope.launch { listState.animateScrollToItem(target) }
    }

    suspend fun scanSearch(jumpToFirst: Boolean) {
        val query = searchQuery
        if (!searchActive || query.isBlank()) {
            searchMatchLines = emptyList()
            searchMatchIndex = -1
            return
        }

        val visible = visibleTypes
        val snapshot = viewModel.logs.toList()
        val matches = withContext(Dispatchers.Default) {
            snapshot.mapIndexedNotNull { index, line ->
                if (line.type !in visible) return@mapIndexedNotNull null
                val plain = line.output.parseAsAnsiAnnotatedString().text
                if (plain.contains(query, ignoreCase = true)) index else null
            }
        }

        val currentLine = searchMatchLines.getOrNull(searchMatchIndex)
        searchMatchLines = matches
        searchMatchIndex = when {
            matches.isEmpty() -> -1
            jumpToFirst -> 0
            currentLine != null -> matches.indexOf(currentLine).takeIf { it >= 0 }
                ?: searchMatchIndex.coerceIn(0, matches.lastIndex)

            else -> searchMatchIndex.coerceIn(0, matches.lastIndex)
        }

        if (jumpToFirst && searchMatchIndex >= 0) {
            scrollToLogLine(matches[searchMatchIndex])
        }
    }

    fun closeSearch() {
        searchActive = false
        searchQuery = ""
        searchMatchLines = emptyList()
        searchMatchIndex = -1
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun openSearch() {
        actionsExpanded = true
        searchActive = true
    }

    fun cycleSearch(delta: Int) {
        if (searchMatchLines.isEmpty()) {
            scope.launch { scanSearch(jumpToFirst = true) }
            return
        }
        val next = (searchMatchIndex + delta + searchMatchLines.size) % searchMatchLines.size
        searchMatchIndex = next
        scrollToLogLine(searchMatchLines[next])
    }

    LaunchedEffect(searchActive) {
        if (searchActive) {
            delay(100)
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(searchActive, searchQuery, filterRevision) {
        if (!searchActive || searchQuery.isBlank()) {
            searchMatchLines = emptyList()
            searchMatchIndex = -1
            return@LaunchedEffect
        }
        searchMatchLines = emptyList()
        searchMatchIndex = -1
        delay(300)
        scanSearch(jumpToFirst = true)
    }

    LaunchedEffect(viewModel.logs.size, searchActive, filterRevision) {
        if (!searchActive || searchQuery.isBlank()) return@LaunchedEffect
        delay(300)
        scanSearch(jumpToFirst = false)
    }

    LaunchedEffect(viewModel.logs.size, searchActive) {
        if (searchActive || viewModel.logs.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
        val nearBottom = info.totalItemsCount == 0 || lastVisible >= info.totalItemsCount - 3
        if (nearBottom) {
            listState.scrollToItem(viewModel.logs.size)
        }
    }

    BackHandler(enabled = searchActive) {
        closeSearch()
    }

    var showExtraDialog by remember { mutableStateOf(false) }
    var showSavedCommands by remember { mutableStateOf(false) }

    ExtraSettings(
        showExtraDialog,
        viewModel,
    ) {
        showExtraDialog = false
        filterRevision++
    }

    if (showSavedCommands) {
        SavedCommandsSheet(
            viewModel = viewModel,
            onDismiss = { showSavedCommands = false },
            onLoadCommand = {
                showSavedCommands = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            placeholder = {
                                Text(stringResource(R.string.quickshell_search_hint))
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    scope.launch { scanSearch(jumpToFirst = true) }
                                    keyboardController?.hide()
                                }
                            )
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.quick_shell),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                actions = {
                    if (searchActive) {
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = stringResource(
                                    R.string.quickshell_search_matches,
                                    if (searchMatchIndex >= 0) searchMatchIndex + 1 else 0,
                                    searchMatchLines.size
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = { cycleSearch(-1) },
                            enabled = searchMatchLines.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.quickshell_search_previous)
                            )
                        }
                        IconButton(
                            onClick = { cycleSearch(1) },
                            enabled = searchMatchLines.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.quickshell_search_next)
                            )
                        }
                        IconButton(onClick = { closeSearch() }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.quickshell_search_close)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                showSavedCommands = true
                            }
                        ) {
                            Icon(Icons.Outlined.Bookmark, contentDescription = "Saved commands")
                        }
                        IconButton(
                            onClick = {
                                viewModel.stop()
                            },
                            enabled = running,
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                        }
                        IconButton(
                            onClick = {
                                viewModel.clearLogs()
                                actionsExpanded = false
                                closeSearch()
                            },
                            enabled = viewModel.logs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.ClearAll, contentDescription = null)
                        }
                        IconButton(
                            onClick = {
                                showExtraDialog = true
                            }
                        ) {
                            Icon(Icons.Outlined.MoreVert, null)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = viewModel.logs.isNotEmpty() && (fabVisible || actionsExpanded),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            ) {
                Box(
                    modifier = Modifier.padding(
                        bottom = if (bottomBarHidden.value) 26.dp else 96.dp,
                        end = 16.dp
                    ),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Surface(
                        modifier = Modifier
                            .width(56.dp)
                            .animateContentSize(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            AnimatedVisibility(
                                visible = actionsExpanded,
                                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                            ) {
                                Column {
                                    IconButton(onClick = { openSearch() }) {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = stringResource(R.string.quickshell_search_logs)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                saveLogsToDownload(
                                                    context,
                                                    viewModel.logs,
                                                    snackBarHost
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.Save,
                                            contentDescription = stringResource(R.string.save)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (actionsExpanded) {
                                        actionsExpanded = false
                                        closeSearch()
                                    } else {
                                        actionsExpanded = true
                                    }
                                }
                            ) {
                                Icon(
                                    if (actionsExpanded) {
                                        Icons.Outlined.KeyboardArrowDown
                                    } else {
                                        Icons.Outlined.KeyboardArrowUp
                                    },
                                    contentDescription = stringResource(
                                        if (actionsExpanded) {
                                            R.string.quickshell_actions_collapse
                                        } else {
                                            R.string.quickshell_actions_expand
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { AxSnackBarHost(hostState = snackBarHost) }
    ) { paddingValues ->
        var keyboardVisible by remember { mutableStateOf(false) }
        var commandFieldFocused by remember { mutableStateOf(false) }

        KeyEventBlocker {
            val prefs = PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_")
            when (it.key) {
                Key.VolumeUp -> prefs.loadState(
                    context,
                    QuickShellViewModel.KeyEventType.VOLUME_UP,
                    true
                )

                Key.VolumeDown -> prefs.loadState(
                    context,
                    QuickShellViewModel.KeyEventType.VOLUME_DOWN,
                    true
                )

                else -> false
            }
        }

        KeyboardVisibilityListener(
            onKeyboardState = { visible ->
                keyboardVisible = visible
                if (!visible) {
                    focusManager.clearFocus()
                }
            }
        )

        Box(
            Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {


            val hScroll = rememberScrollState()

            SelectionContainer(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .horizontalScroll(hScroll)
                ) {
                    LazyColumn(
                        state = listState,
                    ) {
                        item {
                            Spacer(modifier = Modifier.size(70.dp))
                        }
                        items(viewModel.logs) { line ->
                            if (line.type !in visibleTypes) return@items
                            val lineColor = line.type.color ?: MaterialTheme.colorScheme.onSurfaceVariant
                            val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
                            val text = remember(
                                line.output,
                                searchActive,
                                searchQuery,
                                highlightColor
                            ) {
                                val parsed = line.output.parseAsAnsiAnnotatedString()
                                if (!searchActive || searchQuery.isBlank()) {
                                    parsed
                                } else {
                                    parsed.highlightMatches(searchQuery, highlightColor)
                                }
                            }
                            BasicText(
                                text = text,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    lineHeight = MaterialTheme.typography.labelSmall.fontSize,
                                    lineHeightStyle = LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both
                                    ),
                                    color = lineColor,
                                    fontFamily = FontFamily.Monospace
                                ),
                                softWrap = false,
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            @SuppressLint("ConfigurationScreenWidthHeight")
            val screen = LocalConfiguration.current.screenHeightDp
            val paddingHeight = (screen * 0.52).dp


            ElevatedCard(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.elevatedCardColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
//                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .run {
                        if (keyboardVisible && commandFieldFocused) {
                            padding(bottom = paddingHeight)
                        } else {
                            padding(bottom = 0.dp)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    TextField(
                        value = viewModel.commandText,
                        onValueChange = {
                            viewModel.setCommand(it)
                        },
                        label = {
                            Text(viewModel.execMode)
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = MaterialTheme.typography.bodyLarge.fontSize,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            ),
                            fontFamily = FontFamily.Monospace
                        ),
                        maxLines = if (keyboardVisible && commandFieldFocused) Int.MAX_VALUE else 1,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,   // garis saat fokus
                            unfocusedIndicatorColor = Color.Transparent, // garis saat tidak fokus
                            disabledIndicatorColor = Color.Transparent,   // garis saat disabled
                            focusedContainerColor = Color.Transparent,   // ⬅ ini penting
                            unfocusedContainerColor = Color.Transparent, // ⬅ ini juga
                            disabledContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { commandFieldFocused = it.isFocused }
                            .animateContentSize(
                                animationSpec = tween(
                                    durationMillis = 250,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                    )

                    var justSaved by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentCommand()
                            justSaved = true
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 56.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Bookmark,
                            contentDescription = "Save",
                            modifier = Modifier.size(28.dp),
                            tint = if (justSaved) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LaunchedEffect(justSaved) {
                        if (justSaved) {
                            kotlinx.coroutines.delay(1500)
                            justSaved = false
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.runShell()
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp)
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_exec),
                            contentDescription = stringResource(R.string.exec),
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun KeyboardVisibilityListener(
    onKeyboardState: (visible: Boolean) -> Unit
) {
    val insets = WindowInsets.ime
    val imeHeight = insets.getBottom(LocalDensity.current)

    val imeVisible = imeHeight > 0
    val prevImeVisible = remember { mutableStateOf(imeVisible) }

    LaunchedEffect(imeVisible) {
        if (imeVisible != prevImeVisible.value) {
            onKeyboardState(imeVisible)
            prevImeVisible.value = imeVisible
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraSettings(
    showDialog: Boolean,
    quickShellViewModel: QuickShellViewModel,
    onDismissRequest: () -> Unit
) {
    if (showDialog) {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onDismissRequest = onDismissRequest
        ) {
            val context: Context = LocalContext.current
            val outputOption = QuickShellViewModel.OutputType.entries
            val keyEventOption = QuickShellViewModel.KeyEventType.entries

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val checkedOutputStates = remember {
                        mutableStateMapOf<QuickShellViewModel.OutputType, Boolean>().apply {
                            outputOption.forEach {
                                put(
                                    it,
                                    PrefsEnumHelper<QuickShellViewModel.OutputType>("output_")
                                        .loadState(context, it, true)
                                )
                            }
                        }
                    }

                    SettingsItemExpanded(
                        label = stringResource(R.string.output_filter),
                        description = stringResource(R.string.filter_output_desc),
                        iconVector = Icons.Outlined.Output
                    ) { _, expanded ->
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                outputOption.forEach { type ->
                                    val isChecked = checkedOutputStates[type] ?: false
                                    CheckBoxText(
                                        label = stringResource(type.labelId),
                                        checked = isChecked
                                    ) {
                                        checkedOutputStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.OutputType>("output_")
                                            .saveState(context, type, it)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val checkedSaveStates = remember {
                        mutableStateMapOf<QuickShellViewModel.OutputType, Boolean>().apply {
                            outputOption.forEach {
                                put(
                                    it,
                                    PrefsEnumHelper<QuickShellViewModel.OutputType>("save_")
                                        .loadState(context, it, true)
                                )
                            }
                        }
                    }

                    SettingsItemExpanded(
                        label = stringResource(R.string.save_log_filter),
                        description = stringResource(R.string.filter_save_log_desc),
                        iconVector = Icons.Outlined.Save,
                    ) { _, expanded ->
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                outputOption.forEach { type ->
                                    val isChecked = checkedSaveStates[type] ?: false
                                    CheckBoxText(
                                        label = stringResource(type.labelId),
                                        checked = isChecked
                                    ) {
                                        checkedSaveStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.OutputType>("save_")
                                            .saveState(context, type, it)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val blockedKeyEventStates = remember {
                        mutableStateMapOf<QuickShellViewModel.KeyEventType, Boolean>().apply {
                            keyEventOption.forEach {
                                put(
                                    it,
                                    PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_")
                                        .loadState(context, it, true)
                                )
                            }
                        }
                    }

                    SettingsItemExpanded(
                        label = stringResource(R.string.block_key_event),
                        description = stringResource(R.string.block_key_event_desc),
                        iconVector = Icons.Outlined.DoNotTouch,
                    ) { _, expanded ->
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                keyEventOption.forEach { type ->
                                    val isChecked = blockedKeyEventStates[type] ?: false
                                    CheckBoxText(
                                        label = stringResource(type.labelId),
                                        checked = isChecked
                                    ) {
                                        blockedKeyEventStates[type] = it
                                        PrefsEnumHelper<QuickShellViewModel.KeyEventType>("block_")
                                            .saveState(context, type, it)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsItem(
                        iconVector = Icons.Filled.Security,
                        label = stringResource(R.string.shell_restriction),
                        description = stringResource(R.string.shell_restriction_desc),
                        checked = quickShellViewModel.isShellRestrictionEnabled,
                        onSwitchChange = { quickShellViewModel.setShellRestriction(it) }
                    )
                }

                item {
                    SettingsItem(
                        iconVector = Icons.Outlined.Bolt,
                        label = stringResource(R.string.compat_mode),
                        description = stringResource(R.string.compat_mode_desc),
                        checked = quickShellViewModel.isCompatModeEnabled,
                        onSwitchChange = { quickShellViewModel.setCompatMode(it) }
                    )
                }

                item {
                    SettingsItem(
                        iconVector = Icons.Outlined.Input,
                        label = stringResource(R.string.clear_command),
                        description = stringResource(R.string.clear_command_desc),
                        checked = quickShellViewModel.isClearCommandEnabled,
                        onSwitchChange = { quickShellViewModel.setClearCommand(it) }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedCommandsSheet(
    viewModel: QuickShellViewModel,
    onDismiss: () -> Unit,
    onLoadCommand: () -> Unit
) {
    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Saved Commands",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            if (viewModel.savedCommands.isEmpty()) {
                Text(
                    text = "No saved commands",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    itemsIndexed(viewModel.savedCommands) { index, cmd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadSavedCommand(index)
                                    onLoadCommand()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cmd,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { viewModel.removeSavedCommand(index) }
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun AnnotatedString.highlightMatches(
    query: String,
    background: Color
): AnnotatedString {
    if (query.isEmpty()) return this
    val source = text
    val ranges = mutableListOf<IntRange>()
    var start = 0
    while (start < source.length) {
        val index = source.indexOf(query, start, ignoreCase = true)
        if (index < 0) break
        ranges += index until (index + query.length)
        start = index + query.length.coerceAtLeast(1)
    }
    if (ranges.isEmpty()) return this

    return buildAnnotatedString {
        append(this@highlightMatches)
        ranges.forEach { range ->
            addStyle(
                SpanStyle(background = background),
                range.first,
                range.last + 1
            )
        }
    }
}

suspend fun saveLogsToDownload(
    context: Context,
    logs: List<QuickShellViewModel.Output>,
    snackbar: SnackbarHostState
) {
    val logSaved = context.getString(R.string.log_saved_to)
    val logFailed = context.getString(R.string.failed_to_save_log)
    if (logs.isEmpty()) return
    val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    val date = format.format(Date())

    val baseDir = PathHelper.getPath(AxeronApiConstant.folder.PARENT_LOG)
    if (!baseDir.exists()) {
        baseDir.mkdirs()
    }

    val file = File(baseDir, "QuickShell_log_${date}.log")

    try {
        val store = PrefsEnumHelper<QuickShellViewModel.OutputType>("save_")
        val saveTypes = QuickShellViewModel.OutputType.entries
            .filter { store.loadState(context, it, true) }
            .toSet()
        val fos =
            Axeron.newFileService().getStreamSession(file.absolutePath, true, false).outputStream
        val writer = fos.bufferedWriter()
        logs.forEach { line ->
            if (line.type !in saveTypes) return@forEach
            writer.write("${line.output}\n")
        }
        writer.flush()

        snackbar.showSnackbar(logSaved.format(file.absolutePath))
    } catch (e: Exception) {
        snackbar.showSnackbar(logFailed.format(e.message))
    }
}



