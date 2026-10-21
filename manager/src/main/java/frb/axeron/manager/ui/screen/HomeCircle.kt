package frb.axeron.manager.ui.screen

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.generated.destinations.ActivateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickShellScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ToggleShortcutsScreenDestination
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
import frb.axeron.manager.ui.component.PowerDialog
import frb.axeron.manager.ui.component.rememberLoadingDialog
import frb.axeron.manager.ui.navigation.BottomBarDestination
import frb.axeron.manager.ui.util.checkNewVersion
import frb.axeron.manager.ui.util.openUpdateUrl
import frb.axeron.manager.ui.viewmodel.ActivateViewModel
import frb.axeron.manager.ui.viewmodel.PluginViewModel
import frb.axeron.manager.ui.viewmodel.PrivilegeViewModel
import frb.axeron.manager.ui.theme.HomeTokens
import frb.axeron.manager.ui.theme.LocalHomeTokens
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
    privilegeViewModel: PrivilegeViewModel,
    bannerDrawable: Drawable? = null,
    onTabSelected: (BottomBarDestination) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = AxeronSettings.getPreferences()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showPowerDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val tokens = LocalHomeTokens.current

    val status = activateViewModel.activateStatus
    val isRunning = status is ActivateViewModel.ActivateStatus.Running
    val isUpdating = status is ActivateViewModel.ActivateStatus.Updating
    val isNeedExtraStep = status is ActivateViewModel.ActivateStatus.NeedExtraStep
    val axeronInfo = activateViewModel.axeronInfo
    val isShizukuActive = activateViewModel.isShizukuActive

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
        listOf(tokens.staggerDelayBase, tokens.staggerDelayStep, tokens.staggerDelayStep, tokens.staggerDelayStep)
            .forEachIndexed { i, d ->
                launch {
                    delay(d)
                    enterAnimations[i] = true
                }
            }
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
            },
            onSoftReboot = {
                try {
                    Axeron.softReboot()
                } catch (_: Throwable) {
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = isRunning,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.padding(
                        bottom = 96.dp,
                        end = 16.dp
                    )
                ) {
                    FloatingActionButton(
                        onClick = {
                            navigator.navigate(QuickShellScreenDestination)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = stringResource(R.string.quick_shell)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacingMd)
        ) {
            item {
                AnimatedVisibility(
                    visible = enterAnimations[0],
                    enter = fadeIn(animationSpec = tween(tokens.motionDurationMedium, delayMillis = 0, easing = tokens.motionEasingDecelerate)) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                    ),
                    exit = fadeOut(animationSpec = tween(tokens.motionDurationShort, easing = tokens.motionEasingAccelerate))
                ) {
                    HeroSection(
                        status = status,
                        axeronInfo = axeronInfo,
                        isShizukuActive = isShizukuActive,
                        onPowerClick = { showPowerDialog = true },
                        onActivateClick = { navigator.navigate(ActivateScreenDestination) },
                        bannerDrawable = bannerDrawable,
                        tokens = tokens
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = enterAnimations[1],
                    enter = fadeIn(animationSpec = tween(tokens.motionDurationMedium, delayMillis = tokens.staggerDelayStep.toInt(), easing = tokens.motionEasingDecelerate)) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                    )
                ) {
                    QuickActionsRow(
                        enabled = isRunning,
                        onTogglesClick = {
                            navigator.navigate(ToggleShortcutsScreenDestination)
                        },
                        onReigniteClick = {
                            scope.launch {
                                val success = loadingDialog.withLoading {
                                    AxeronPluginService.igniteSuspendService()
                                }
                                if (success) pluginViewModel.fetchModuleList()
                            }
                        },
                        tokens = tokens
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(tokens.spacingMd)
                ) {
                    AnimatedVisibility(
                        visible = enterAnimations[2],
                        enter = fadeIn(animationSpec = tween(tokens.motionDurationMedium, delayMillis = (tokens.staggerDelayStep * 2).toInt(), easing = tokens.motionEasingDecelerate)) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                        )
                    ) {
                        MetricsGrid(
                            pluginCount = pluginViewModel.plugins.size,
                            privilegeCount = if (isShizukuActive) privilegeViewModel.privilegedCount else null,
                            isShizukuActive = isShizukuActive,
                            isRunning = isRunning,
                            onPluginClick = { onTabSelected(BottomBarDestination.Plugin) },
                            onPrivilegeClick = { onTabSelected(BottomBarDestination.Privilege) },
                            tokens = tokens
                        )
                    }

                    AnimatedVisibility(
                        visible = enterAnimations[3],
                        enter = fadeIn(animationSpec = tween(tokens.motionDurationMedium, delayMillis = (tokens.staggerDelayStep * 3).toInt(), easing = tokens.motionEasingDecelerate)) + scaleIn(
                            initialScale = 0.92f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                        )
                    ) {
                        DeviceInfoSection(
                            axeronInfo = axeronInfo,
                            tokens = tokens
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    status: ActivateViewModel.ActivateStatus,
    axeronInfo: AxeronInfo,
    isShizukuActive: Boolean,
    onPowerClick: () -> Unit,
    onActivateClick: () -> Unit,
    bannerDrawable: Drawable?,
    tokens: HomeTokens
) {
    val isRunning = status is ActivateViewModel.ActivateStatus.Running
    val isUpdating = status is ActivateViewModel.ActivateStatus.Updating
    val isNeedExtraStep = status is ActivateViewModel.ActivateStatus.NeedExtraStep

    val (statusColor, statusContainer, statusOnContainer) = when {
        isRunning -> Triple(tokens.statusRunning, tokens.statusRunningContainer, tokens.statusRunningOnContainer)
        isUpdating -> Triple(tokens.statusUpdating, tokens.statusUpdatingContainer, tokens.statusUpdatingOnContainer)
        isNeedExtraStep -> Triple(tokens.statusWarning, tokens.statusWarningContainer, tokens.statusWarningOnContainer)
        else -> Triple(tokens.statusStopped, tokens.statusStoppedContainer, tokens.statusStoppedOnContainer)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = tokens.pulseMinScale,
        targetValue = tokens.pulseMaxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(tokens.pulseDuration),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacingMd),
        contentAlignment = Alignment.Center
    ) {
        if (bannerDrawable != null) {
            Box(modifier = Modifier.matchParentSize()) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    update = { view ->
                        view.setImageDrawable(bannerDrawable)
                        (bannerDrawable as? android.graphics.drawable.Animatable)?.start()
                    },
                    onRelease = { view ->
                        (view.drawable as? android.graphics.drawable.Animatable)?.stop()
                        view.setImageDrawable(null)
                    },
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(tokens.heroBannerCornerRadius))
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(tokens.heroBannerCornerRadius))
                        .background(
                            Brush.verticalGradient(
                                0f to tokens.bannerOverlayStart,
                                0.45f to tokens.bannerOverlayMid,
                                0.65f to tokens.bannerOverlayEnd,
                                1f to tokens.heroSurface
                            )
                        )
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.heroMinHeight)
                    .clip(RoundedCornerShape(tokens.heroBannerCornerRadius))
                    .background(
                        Brush.verticalGradient(
                            0f to statusContainer.copy(alpha = 0.35f),
                            0.6f to statusContainer.copy(alpha = 0.08f),
                            1f to Color.Transparent
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = tokens.heroContentPadding, bottom = tokens.heroContentPadding, start = tokens.heroContentPadding, end = tokens.heroContentPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_axora_logo),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(tokens.spacingSm))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = tokens.heroAppNameSize,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isRunning) {
                    IconButton(
                        onClick = onPowerClick,
                        modifier = Modifier.padding(start = tokens.spacingSm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = stringResource(R.string.power_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(tokens.spacingLg))

            if (isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacingSm)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .scale(pulseScale)
                                .background(statusColor.copy(alpha = 0.3f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(statusColor, CircleShape)
                        )
                    }
                    Text(
                        text = stringResource(R.string.home_running),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = tokens.heroStatusSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = stringResource(R.string.home_meta_version, axeronInfo.getVersionCode()),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = tokens.heroMetaSize
                        ),
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(tokens.chipCornerRadius))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = tokens.spacingSm, vertical = tokens.spacingXs)
                    )
                }

                Spacer(Modifier.height(tokens.spacingSm))

                ServerMetaPill(axeronInfo = axeronInfo, tokens = tokens)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacingSm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = when {
                            isUpdating -> stringResource(R.string.updating)
                            isNeedExtraStep -> stringResource(R.string.home_need_fix)
                            else -> stringResource(R.string.home_not_running)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = tokens.heroStatusSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(Modifier.height(tokens.spacingXs))

                Text(
                    text = when {
                        isUpdating -> stringResource(R.string.server_updating_version,
                            axeronInfo.getVersionCode(),
                            AxeronApiConstant.server.VERSION_CODE)
                        isNeedExtraStep -> stringResource(R.string.home_need_fix_msg)
                        else -> stringResource(R.string.home_click_to_install)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = tokens.heroMetaSize
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isUpdating && !isNeedExtraStep) {
                    Spacer(Modifier.height(tokens.spacingMd))
                    Button(
                        onClick = onActivateClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(tokens.chipCornerRadius)
                    ) {
                        Text(text = stringResource(R.string.activate))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerMetaPill(axeronInfo: AxeronInfo, tokens: HomeTokens) {
    val metaTextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = tokens.heroMetaSize)

    FlowRow(
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.chipCornerRadius))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = tokens.spacingSm, vertical = tokens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacingSm),
        verticalArrangement = Arrangement.spacedBy(tokens.spacingXs)
    ) {
        UptimeText(axeronInfo = axeronInfo, tokens = tokens)
        Text(
            text = "|",
            style = metaTextStyle,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = stringResource(R.string.home_meta_pid, axeronInfo.serverInfo.pid),
            style = metaTextStyle,
            color = Color.White
        )
        Text(
            text = "|",
            style = metaTextStyle,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = stringResource(R.string.home_meta_uid, axeronInfo.serverInfo.uid),
            style = metaTextStyle,
            color = Color.White
        )
    }
}

@Composable
private fun UptimeText(axeronInfo: AxeronInfo, tokens: HomeTokens) {
    var time by remember { mutableLongStateOf(0L) }

    val starting = axeronInfo.serverInfo.starting

    LaunchedEffect(starting) {
        if (starting <= 0L) return@LaunchedEffect
        while (true) {
            time = SystemClock.elapsedRealtime() - starting
            delay(1000)
        }
    }

    fun formatUptime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            modifier = Modifier.size(tokens.heroMetaSize.value.dp),
            tint = Color.White
        )
        Text(
            text = if (starting <= 0L) "--:--:--" else formatUptime(time),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = tokens.heroMetaSize
            ),
            color = Color.White
        )
    }
}

@Composable
private fun QuickActionsRow(
    enabled: Boolean,
    onTogglesClick: () -> Unit,
    onReigniteClick: () -> Unit,
    tokens: HomeTokens
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacingMd),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacingSm)
    ) {
        ActionChip(
            icon = Icons.Default.LocalFireDepartment,
            label = stringResource(R.string.ignite),
            enabled = enabled,
            onClick = onReigniteClick,
            tokens = tokens,
            modifier = Modifier.weight(1f)
        )
        ActionChip(
            icon = Icons.Outlined.Tune,
            label = stringResource(R.string.quick_toggles),
            enabled = enabled,
            onClick = onTogglesClick,
            tokens = tokens,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionChip(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tokens: HomeTokens,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.5f
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(tokens.chipCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .alpha(alpha)
            .height(tokens.chipHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(tokens.chipIconSize),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricsGrid(
    pluginCount: Int,
    privilegeCount: Int?,
    isShizukuActive: Boolean,
    isRunning: Boolean,
    onPluginClick: () -> Unit,
    onPrivilegeClick: () -> Unit,
    tokens: HomeTokens
) {
    val pluginLabel = stringResource(R.string.plugin)
    val privilegeLabel = stringResource(R.string.privilege)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = tokens.spacingMd),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacingSm)
    ) {
        MetricCard(
            item = MetricItem(
                label = pluginLabel,
                count = pluginCount,
                icon = Icons.Outlined.Extension,
                enabled = isRunning,
                onClick = if (isRunning) onPluginClick else null
            ),
            tokens = tokens,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        if (isShizukuActive) {
            MetricCard(
                item = MetricItem(
                    label = privilegeLabel,
                    count = privilegeCount ?: 0,
                    icon = Icons.Outlined.Security,
                    enabled = isRunning,
                    onClick = if (isRunning) onPrivilegeClick else null
                ),
                tokens = tokens,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

private data class MetricItem(
    val label: String,
    val count: Int,
    val icon: ImageVector,
    val enabled: Boolean,
    val onClick: (() -> Unit)?
)

@Composable
private fun MetricCard(
    item: MetricItem,
    tokens: HomeTokens,
    modifier: Modifier = Modifier
) {
    val alpha = if (item.enabled) 1f else 0.5f
    val containerColor = tokens.cardSurface.copy(alpha = alpha)
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
    val shape = RoundedCornerShape(tokens.cardCornerRadius)
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor
    )
    val elevation = CardDefaults.cardElevation(
        defaultElevation = 1.dp,
        hoveredElevation = 2.dp,
        pressedElevation = 0.dp
    )

    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = tokens.cardPadding, vertical = tokens.spacingSm),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacingSm)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(tokens.cardIconSize),
                    tint = iconColor
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = tokens.cardLabelSize
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(tokens.spacingXs))
            AnimatedCounter(
                count = item.count,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = tokens.cardCountSize,
                    lineHeight = tokens.cardCountSize * 1.1f,
                    color = contentColor
                )
            )
        }
    }

    if (item.onClick != null && item.enabled) {
        Card(
            onClick = item.onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = content
        )
    }
}

@Composable
private fun DeviceInfoSection(
    axeronInfo: AxeronInfo,
    tokens: HomeTokens
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacingMd),
        colors = CardDefaults.cardColors(
            containerColor = tokens.cardSurface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(tokens.infoSectionCornerRadius),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            hoveredElevation = 1.dp,
            pressedElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.cardPadding, vertical = tokens.cardPadding)
        ) {
            Text(
                text = stringResource(R.string.axeron_environment),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = tokens.infoLabelSize,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(tokens.infoRowSpacing))

            InfoRow(
                icon = Icons.Outlined.Android,
                label = stringResource(R.string.android_version),
                value = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
                tokens = tokens
            )
            Spacer(Modifier.height(tokens.infoRowSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(tokens.infoIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(tokens.spacingMd))
                Text(
                    text = stringResource(R.string.abi_supported),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = tokens.infoValueSize
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(tokens.spacingSm))
                Column {
                    Build.SUPPORTED_ABIS.forEach { abi ->
                        Text(
                            text = abi,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = tokens.infoValueSize
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(tokens.infoRowSpacing))

            InfoRow(
                icon = Icons.Outlined.Shield,
                label = stringResource(R.string.selinux_context),
                value = axeronInfo.serverInfo.selinuxContext,
                tokens = tokens
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    tokens: HomeTokens
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacingMd)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(tokens.infoIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = tokens.infoValueSize
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = tokens.infoValueSize
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
