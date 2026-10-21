package frb.axeron.manager.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.R
import frb.axeron.manager.data.SettingsRepository
import frb.axeron.manager.data.ToggleConfig
import frb.axeron.manager.ui.component.ConfirmResult
import frb.axeron.manager.ui.component.rememberConfirmDialog
import frb.axeron.manager.ui.viewmodel.ToggleViewModel
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ToggleShortcutsScreen(
    navigator: DestinationsNavigator,
    viewModelGlobal: ViewModelGlobal
) {
    val toggleViewModel = viewModelGlobal.toggleViewModel
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ToggleConfig?>(null) }

    LaunchedEffect(Unit) {
        toggleViewModel.refreshAll()
    }

    LaunchedEffect(toggleViewModel.failedLabel) {
        val label = toggleViewModel.failedLabel ?: return@LaunchedEffect
        Toast.makeText(
            context,
            context.getString(R.string.toggle_failed, label),
            Toast.LENGTH_SHORT
        ).show()
        toggleViewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.toggle_shortcuts),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Filled.Add, stringResource(R.string.add_toggle))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_toggle))
            }
        }
    ) { padding ->
        if (toggleViewModel.configs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.toggle_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = toggleViewModel.configs,
                    key = { it.id }
                ) { config ->
                    ToggleCard(
                        config = config,
                        currentValue = toggleViewModel.values[config.id],
                        busy = config.id in toggleViewModel.busyIds,
                        onToggle = { toggleViewModel.toggle(config, it) },
                        onEdit = { editing = config },
                        onDelete = { toggleViewModel.delete(config) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        ToggleConfigSheet(
            toggleViewModel = toggleViewModel,
            onDismiss = { showAdd = false },
            allowTypeSelection = true
        )
    }

    editing?.let { config ->
        ToggleConfigSheet(
            toggleViewModel = toggleViewModel,
            initial = config,
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun ToggleCard(
    config: ToggleConfig,
    currentValue: String?,
    busy: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOn = currentValue != null && currentValue == config.onValue
    var menuExpanded by remember(config.id) { mutableStateOf(false) }
    val confirmDialog = rememberConfirmDialog()
    val scope = rememberCoroutineScope()
    val deleteTitle = stringResource(R.string.toggle_delete_title)
    val deleteMessage = stringResource(R.string.toggle_delete_message)
    val removeLabel = stringResource(R.string.remove)
    val cancelLabel = stringResource(R.string.cancel)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .toggleable(
                value = isOn,
                enabled = !busy,
                role = Role.Switch,
                onValueChange = onToggle
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.displayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${stringResource(config.type.stringId)} · ${config.key}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.toggle_current_value,
                        currentValue ?: stringResource(R.string.toggle_not_set)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Switch(
                    checked = isOn,
                    onCheckedChange = null,
                    enabled = !busy
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.toggle_edit)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    val result = confirmDialog.awaitConfirm(
                                        title = deleteTitle,
                                        content = deleteMessage,
                                        confirm = removeLabel,
                                        dismiss = cancelLabel
                                    )
                                    if (result == ConfirmResult.Confirmed) onDelete()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToggleConfigSheet(
    toggleViewModel: ToggleViewModel,
    onDismiss: () -> Unit,
    initial: ToggleConfig? = null,
    initialType: SettingsRepository.SettingType = SettingsRepository.SettingType.GLOBAL,
    initialKey: String = "",
    initialOffValue: String = "",
    allowTypeSelection: Boolean = false
) {
    var selectedType by remember(initial?.id, initialType) {
        mutableStateOf(initial?.type ?: initialType)
    }
    var key by remember(initial?.id, initialKey) {
        mutableStateOf(initial?.key ?: initialKey)
    }
    var label by remember(initial?.id) {
        mutableStateOf(initial?.label ?: "")
    }
    var offValue by remember(initial?.id, initialOffValue) {
        mutableStateOf(initial?.offValue ?: initialOffValue)
    }
    var onValue by remember(initial?.id) {
        mutableStateOf(initial?.onValue ?: "")
    }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var availableKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingKeys by remember { mutableStateOf(false) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedType) {
        loadingKeys = true
        availableKeys = toggleViewModel.loadKeys(selectedType)
        loadingKeys = false
    }

    val filteredKeys = remember(availableKeys, key) {
        val query = key.trim()
        availableKeys
            .asSequence()
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .take(30)
            .toList()
    }

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(
                    if (initial == null) R.string.add_toggle else R.string.toggle_edit
                ),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(16.dp))

            if (allowTypeSelection) {
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(selectedType.stringId),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Icon(Icons.Outlined.KeyboardArrowDown, null)
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        SettingsRepository.SettingType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(stringResource(type.stringId)) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = key,
                onValueChange = {
                    key = it
                    errorRes = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.toggle_property_key)) },
                singleLine = true
            )

            if (loadingKeys) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.toggle_loading_keys),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredKeys.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.toggle_key_suggestions),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredKeys.forEach { suggestion ->
                        AssistChip(
                            onClick = {
                                key = suggestion
                                errorRes = null
                            },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.toggle_label)) },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = offValue,
                    onValueChange = {
                        offValue = it
                        errorRes = null
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.toggle_off_value)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = onValue,
                    onValueChange = {
                        onValue = it
                        errorRes = null
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.toggle_on_value)) },
                    singleLine = true
                )
            }

            errorRes?.let { message ->
                Text(
                    text = stringResource(message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val trimmedKey = key.trim()
                        when {
                            trimmedKey.isEmpty() -> {
                                errorRes = R.string.toggle_invalid_key
                            }
                            offValue == onValue -> {
                                errorRes = R.string.toggle_values_same
                            }
                            else -> {
                                val config = ToggleConfig(
                                    id = initial?.id ?: UUID.randomUUID().toString(),
                                    type = selectedType,
                                    key = trimmedKey,
                                    label = label.trim(),
                                    offValue = offValue,
                                    onValue = onValue
                                )
                                if (toggleViewModel.upsert(config)) {
                                    onDismiss()
                                } else {
                                    errorRes = R.string.toggle_duplicate
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
