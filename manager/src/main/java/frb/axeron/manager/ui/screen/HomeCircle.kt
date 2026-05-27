package frb.axeron.manager.ui.screen

import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.AxeronInfo
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.api.AxeronPluginService
import frb.axeron.api.core.Starter
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.AnimatedCounter
import frb.axeron.manager.ui.component.ModeLabelText
import frb.axeron.manager.ui.component.PowerDialog
import frb.axeron.manager.ui.component.TonalCard
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.navigation.BottomBarDestination
import frb.axeron.manager.ui.util.checkNewVersion
import frb.axeron.manager.ui.util.openUpdateUrl
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCircleScreen(
    navigator: DestinationsNavigator,
    activateViewModel: ActivateViewModel,
    pluginViewModel: PluginViewModel,
    privilegeViewModel: PrivilegeViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = AxeronSettings.getPreferences()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showPowerDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    val status = activateViewModel.activateStatus
    val isRunning = status is ActivateViewModel.ActivateStatus.Running
    val axeronInfo = activateViewModel.axeronInfo

    val enterAnimations = remember { mutableStateListOf(false, false, false, false) }

    LaunchedEffect(Unit) {
        if (prefs.getBoolean("auto_update_check", true)) {
            withContext(Dispatchers.IO) {
                delay(2000)
                val hasUpdate = checkNewVersion()
                if (hasUpdate) showUpdateDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        activateViewModel.setRestartContext(context)

        delay(80)
        enterAnimations[0] = true
        delay(100)
        enterAnimations[1] = true
        delay(100)
        enterAnimations[2] = true
        delay(100)
        enterAnimations[3] = true
    }

    if (showUpdateDialog) {
        frb.axeron.manager.ui.component.UpdateDialog(
            onDismiss = { showUpdateDialog = false },
            onUpdate = { showUpdateDialog = false; openUpdateUrl(context) }
        )
    }

    if (showPowerDialog) {
        PowerDialog(
            onDismiss = { showPowerDialog = false },
            onReignite = {
                scope.launch {
                    val success = loadingDialog.withLoading {
                        AxeronPluginService.igniteSuspendService()
                    }
                    if (success) pluginViewModel.fetchModuleList()
                }
            },
            onShutdown = {
                activateViewModel.markIntentionalStop()
                Axeron.destroy()
            },
            onRestart = {
                activateViewModel.markIntentionalStop()
                Axeron.newProcess(
                    AxeronCommandSession.getQuickCmd(Starter.internalCommand, true, false),
                    null, null
                )
            }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            AxoraHeroHeader(
                status = status,
                axeronInfo = axeronInfo,
                isShizukuActive = activateViewModel.isShizukuActive,
                onPowerClick = { showPowerDialog = true },
                onActivateClick = { navigator.navigate(ActivateScreenDestination) },
                visible = enterAnimations[0]
            )

            if (isRunning) {
                LaunchedEffect(Unit) { pluginViewModel.fetchModuleList() }

                QuickActionsRow(
                    visible = enterAnimations[1],
                    onTerminalClick = { navigator.navigate(QuickShellScreenDestination) },
                    onReigniteClick = {
                        scope.launch {
                            val success = loadingDialog.withLoading {
                                AxeronPluginService.igniteSuspendService()
                            }
                            if (success) pluginViewModel.fetchModuleList()
                        }
                    }
                )

                AnimatedStatCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    label = stringResource(
                        if (pluginViewModel.plugins.size <= 1) R.string.plugin
                        else R.string.plugin_plural
                    ),
                    count = pluginViewModel.plugins.size,
                    icon = Icons.Outlined.Extension,
                    onClick = {
                        navigator.navigate(BottomBarDestination.Plugin.direction) {
                            popUpTo(NavGraphs.root) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    visible = enterAnimations[1]
                )

                Spacer(Modifier.height(8.dp))

                if (activateViewModel.isShizukuActive) {
                    AnimatedStatCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        label = stringResource(
                            if (privilegeViewModel.privilegedCount <= 1) R.string.privilege
                            else R.string.privilege_plural
                        ),
                        count = privilegeViewModel.privilegedCount,
                        icon = Icons.Outlined.Security,
                        enabled = true,
                        onClick = {
                            navigator.navigate(BottomBarDestination.Privilege.direction) {
                                popUpTo(NavGraphs.root) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        visible = enterAnimations[1]
                    )
                }
            } else {
                AnimatedStatCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    label = stringResource(R.string.plugin_plural),
                    count = 0,
                    icon = Icons.Outlined.Extension,
                    enabled = false,
                    visible = enterAnimations[1]
                )
            }

            Spacer(Modifier.height(16.dp))

            DeviceInfoSection(
                axeronInfo = axeronInfo,
                visible = enterAnimations[2]
            )

            Spacer(Modifier.height(12.dp))

            LearnMoreSection(visible = enterAnimations[3])

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AxoraHeroHeader(
    status: ActivateViewModel.ActivateStatus,
    axeronInfo: AxeronInfo,
    isShizukuActive: Boolean,
    onPowerClick: () -> Unit,
    onActivateClick: () -> Unit,
    visible: Boolean
) {
    val isRunning = status is ActivateViewModel.ActivateStatus.Running
    val isUpdating = status is ActivateViewModel.ActivateStatus.Updating
    val isNeedExtraStep = status is ActivateViewModel.ActivateStatus.NeedExtraStep

    val heroColor = when {
        isRunning -> MaterialTheme.colorScheme.secondaryContainer
        isUpdating -> MaterialTheme.colorScheme.primaryContainer
        isNeedExtraStep -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }

    val dotColor = when {
        isRunning -> MaterialTheme.colorScheme.primary
        isUpdating -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            animationSpec = tween(400),
            initialOffsetY = { -it / 3 }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to heroColor.copy(alpha = 0.35f),
                        0.6f to heroColor.copy(alpha = 0.08f),
                        1f to Color.Transparent
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_thunderbolt),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isRunning) {
                        IconButton(onClick = onPowerClick) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Power",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isRunning) {
                    val infiniteTransition = rememberInfiniteTransition(label = "statusGlow")
                    val glowScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glowScale"
                    )
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.7f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glowAlpha"
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .scale(glowScale)
                                    .background(dotColor.copy(alpha = glowAlpha), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(dotColor, CircleShape)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.home_running),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(10.dp))
                        ModeLabelText(
                            label = axeronInfo.serverInfo.getMode().label,
                            containerColor = MaterialTheme.colorScheme.primary,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Version: ${axeronInfo.getVersionCode()}  |  PID: ${axeronInfo.serverInfo.pid}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    UptimeText(axeronInfo = axeronInfo)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(dotColor, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = when {
                                isUpdating -> stringResource(R.string.updating)
                                isNeedExtraStep -> stringResource(R.string.home_need_fix)
                                else -> stringResource(R.string.home_not_running)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = when {
                            isUpdating -> stringResource(R.string.server_updating_version,
                                axeronInfo.getVersionCode(),
                                AxeronApiConstant.server.VERSION_CODE)
                            isNeedExtraStep -> stringResource(R.string.home_need_fix_msg)
                            else -> stringResource(R.string.home_click_to_install)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isUpdating && !isNeedExtraStep) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onActivateClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(R.string.activate))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UptimeText(axeronInfo: AxeronInfo) {
    var time by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            time = SystemClock.elapsedRealtime() - axeronInfo.serverInfo.starting
            delay(1000)
        }
    }

    fun formatUptime(millis: Long): String {
        val totalSeconds = millis / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val dayPart = if (days > 0) "${days}d " else ""
        return "T+${dayPart}%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    Text(
        text = formatUptime(time),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun QuickActionsRow(
    visible: Boolean,
    onTerminalClick: () -> Unit,
    onReigniteClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(350)) + slideInVertically(
            animationSpec = tween(350),
            initialOffsetY = { it / 2 }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionChip(
                icon = Icons.Filled.Terminal,
                label = stringResource(R.string.quick_shell),
                onClick = onTerminalClick
            )
            ActionChip(
                icon = Icons.Default.LocalFireDepartment,
                label = stringResource(R.string.ignite),
                onClick = onReigniteClick
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        content = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    )
}

@Composable
private fun AnimatedStatCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            animationSpec = tween(400),
            initialOffsetY = { it / 2 }
        )
    ) {
        val alpha = if (enabled) 1f else 0.5f
        TonalCard(
            modifier = modifier.then(
                if (onClick != null && enabled) Modifier.clip(RoundedCornerShape(20.dp)).clickable { onClick() }
                else Modifier
            ),
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = alpha)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                AnimatedCounter(
                    count = count,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoSection(
    axeronInfo: AxeronInfo,
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            animationSpec = tween(400),
            initialOffsetY = { it / 3 }
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.axeron_environment),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(16.dp))

                InfoRow(
                    icon = Icons.Outlined.Android,
                    label = stringResource(R.string.android_version),
                    value = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.abi_supported),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Build.SUPPORTED_ABIS.forEach { abi ->
                            Text(
                                text = abi,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                InfoRow(
                    icon = Icons.Outlined.Shield,
                    label = stringResource(R.string.selinux_context),
                    value = axeronInfo.serverInfo.selinuxContext
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LearnMoreSection(visible: Boolean) {
    val uriHandler = LocalUriHandler.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            animationSpec = tween(400),
            initialOffsetY = { it / 3 }
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { uriHandler.openUri("https://fahrez182.github.io/AxManager") },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.learn_more),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.learn_more_msg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
