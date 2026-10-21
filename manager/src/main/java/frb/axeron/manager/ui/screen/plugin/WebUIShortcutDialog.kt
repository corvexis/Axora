package frb.axeron.manager.ui.screen.plugin

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.util.SquircleShape
import frb.axeron.manager.ui.webui.WebUIShortcutHelper
import frb.axeron.server.PluginInfo
import kotlinx.coroutines.launch

@Composable
fun WebUIShortcutDialog(
    plugin: PluginInfo,
    bannerData: ByteArray?,
    onDismiss: () -> Unit,
    onShortcutSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(plugin.prop.id) { mutableStateOf("") }
    var icon by remember(plugin.prop.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var iconResolved by remember(plugin.prop.id) { mutableStateOf(false) }
    var iconPicked by remember(plugin.prop.id) { mutableStateOf(false) }
    var isPinned by remember(plugin.prop.id) { mutableStateOf(false) }
    var working by remember(plugin.prop.id) { mutableStateOf(false) }

    LaunchedEffect(plugin.prop.id) {
        name = WebUIShortcutHelper.getShortcutName(context, plugin)
        isPinned = WebUIShortcutHelper.isShortcutPinned(context, plugin.prop.id)
        icon = WebUIShortcutHelper.loadConfiguredIcon(context, plugin, bannerData)
        iconResolved = true
    }

    val iconPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val picked = WebUIShortcutHelper.loadIconFromUri(context, uri)
            if (picked == null) {
                Toast.makeText(
                    context,
                    R.string.webui_shortcut_icon_failed,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                icon = picked
                iconPicked = true
            }
        }
    }

    fun confirm() {
        val trimmedName = name.trim().ifBlank { plugin.prop.name }
        scope.launch {
            working = true
            val shortcutIcon = icon

            if (iconPicked && shortcutIcon != null) {
                if (!WebUIShortcutHelper.saveCustomIconBitmap(
                        context,
                        plugin.prop.id,
                        shortcutIcon
                    )
                ) {
                    Toast.makeText(
                        context,
                        R.string.webui_shortcut_icon_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    working = false
                    return@launch
                }
            }

            WebUIShortcutHelper.saveShortcutName(context, plugin.prop.id, trimmedName)
            val result = WebUIShortcutHelper.pinOrUpdateShortcut(
                context = context,
                plugin = plugin,
                name = trimmedName,
                bannerData = bannerData,
                iconOverride = shortcutIcon
            )
            val message = when (result) {
                WebUIShortcutHelper.ShortcutResult.PINNED -> R.string.webui_shortcut_requested
                WebUIShortcutHelper.ShortcutResult.UPDATED -> R.string.webui_shortcut_updated
                WebUIShortcutHelper.ShortcutResult.UNSUPPORTED -> R.string.webui_shortcut_unsupported
                WebUIShortcutHelper.ShortcutResult.FAILED -> R.string.webui_shortcut_failed
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

            working = false
            onShortcutSaved()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(stringResource(R.string.add_web_ui_shortcut)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(76.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(SquircleShape),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            val bitmap = icon
                            when {
                                !iconResolved -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }

                                bitmap != null -> Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                else -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Home,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp)
                        ) {
                            IconButton(
                                onClick = { iconPicker.launch("image/*") },
                                enabled = !working,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.webui_edit_icon),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp),
                        label = { Text(stringResource(R.string.webui_shortcut_name)) },
                        singleLine = true,
                        enabled = !working
                    )
                }

                Spacer(Modifier.size(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (iconPicked) {
                            stringResource(R.string.webui_shortcut_icon_custom)
                        } else {
                            stringResource(R.string.webui_shortcut_icon_desc)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { confirm() },
                enabled = name.isNotBlank() && !working
            ) {
                Text(
                    stringResource(
                        if (isPinned) R.string.webui_shortcut_update
                        else R.string.webui_shortcut_add
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !working
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
