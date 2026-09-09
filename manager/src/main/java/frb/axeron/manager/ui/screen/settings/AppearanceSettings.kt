package frb.axeron.manager.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import frb.axeron.manager.R
import frb.axeron.manager.ui.component.ClickableItem
import frb.axeron.manager.ui.component.RadioItem
import frb.axeron.manager.ui.component.SettingsCategory
import frb.axeron.manager.ui.component.SwitchItem
import frb.axeron.manager.ui.theme.CORNER_STYLE_DEFAULT
import frb.axeron.manager.ui.theme.CORNER_STYLE_EXTRA_ROUNDED
import frb.axeron.manager.ui.theme.CORNER_STYLE_ROUNDED
import frb.axeron.manager.ui.theme.CORNER_STYLE_SQUARED
import frb.axeron.manager.ui.theme.hexToColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    searchText: String,
    autoThemeEnabled: Boolean,
    onAutoThemeChange: (Boolean) -> Unit,
    darkModeEnabled: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    amoledEnabled: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    cornerStyle: Int,
    onCornerStyleChange: (Int) -> Unit,
    accentIntensity: Float,
    onAccentIntensityChange: (Float) -> Unit,
    bottomBarScale: Float,
    onBottomBarScaleChange: (Float) -> Unit,
    secondaryColorHex: String?,
    onSecondaryPaletteClick: () -> Unit,
    tertiaryColorHex: String?,
    onTertiaryPaletteClick: () -> Unit,
    bannerImagePath: String?,
    onBannerClick: () -> Unit,
    onBannerRemove: () -> Unit,
    systemFontEnabled: Boolean,
    onSystemFontChange: (Boolean) -> Unit,
    fontChoice: String,
    onFontChoiceChange: (String) -> Unit,
    currentLanguageDisplay: String?,
    onLanguageClick: () -> Unit,
    onPaletteClick: () -> Unit,
    onPresetSelected: (String) -> Unit,
    currentColorHex: String
) {
    val categoryTitle = stringResource(R.string.settings_category_appearance)
    val matchCategory = shouldShow(searchText, categoryTitle)

    val languageTitle = stringResource(R.string.settings_language)
    val showLanguage = matchCategory || shouldShow(searchText, languageTitle, currentLanguageDisplay ?: "")

    val autoThemeTitle = stringResource(R.string.auto_theme)
    val autoThemeSummary = stringResource(R.string.auto_theme_desc)
    val showAutoTheme = matchCategory || shouldShow(searchText, autoThemeTitle, autoThemeSummary)

    val darkThemeTitle = stringResource(R.string.dark_theme)
    val showDarkMode = !autoThemeEnabled && (matchCategory || shouldShow(searchText, darkThemeTitle))

    val dynamicColorTitle = stringResource(R.string.dynamic_color)
    val dynamicColorSummary = stringResource(R.string.dynamic_color_desc)
    val showDynamicColor = matchCategory || shouldShow(searchText, dynamicColorTitle, dynamicColorSummary)

    val colorPaletteTitle = stringResource(R.string.color_palette)
    val colorPaletteSummary = stringResource(R.string.customize_color_palette)
    val showColorPalette = !dynamicColorEnabled && (matchCategory || shouldShow(searchText, colorPaletteTitle, colorPaletteSummary))

    val amoledTitle = stringResource(R.string.amoled_mode)
    val amoledSummary = stringResource(R.string.amoled_mode_desc)
    val showAmoled = !dynamicColorEnabled && darkModeEnabled && (matchCategory || shouldShow(searchText, amoledTitle, amoledSummary))

    val cornerTitle = stringResource(R.string.corner_style)
    val showCorner = matchCategory || shouldShow(searchText, cornerTitle)

    val intensityTitle = stringResource(R.string.accent_intensity)
    val intensitySummary = stringResource(R.string.accent_intensity_desc)
    val showIntensity = !dynamicColorEnabled && (matchCategory || shouldShow(searchText, intensityTitle, intensitySummary))

    val bottomBarTitle = stringResource(R.string.bottom_bar_size)
    val bottomBarSummary = stringResource(R.string.bottom_bar_size_desc)
    val showBottomBar = matchCategory || shouldShow(searchText, bottomBarTitle, bottomBarSummary)

    val secondaryTitle = stringResource(R.string.secondary_color)
    val showSecondary = !dynamicColorEnabled && (matchCategory || shouldShow(searchText, secondaryTitle))

    val tertiaryTitle = stringResource(R.string.tertiary_color)
    val showTertiary = !dynamicColorEnabled && (matchCategory || shouldShow(searchText, tertiaryTitle))

    val bannerTitle = stringResource(R.string.home_banner)
    val bannerSummary = stringResource(R.string.home_banner_desc)
    val showBanner = matchCategory || shouldShow(searchText, bannerTitle, bannerSummary)

    val systemFontTitle = stringResource(R.string.system_font)
    val systemFontSummary = stringResource(R.string.system_font_desc)
    val showSystemFont = matchCategory || shouldShow(searchText, systemFontTitle, systemFontSummary)

    val showCategory = showLanguage || showAutoTheme || showDarkMode || showDynamicColor || showColorPalette ||
        showAmoled || showCorner || showIntensity || showBottomBar || showSecondary || showTertiary || showBanner || showSystemFont

    if (showCategory) {
        SettingsCategory(
            icon = Icons.Filled.Palette,
            title = categoryTitle
        ) {
            if (showLanguage) {
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(languageTitle) },
                    supportingContent = {
                        Text(
                            text = currentLanguageDisplay ?: stringResource(R.string.system_default),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    modifier = Modifier
                        .clickable(enabled = true, onClick = onLanguageClick)
                        .padding(horizontal = 8.dp)
                )
            }
            
            if (showAutoTheme) {
                SwitchItem(
                    icon = Icons.Filled.DarkMode,
                    title = autoThemeTitle,
                    summary = autoThemeSummary,
                    checked = autoThemeEnabled,
                    onCheckedChange = onAutoThemeChange
                )
            }
            
            if (showDarkMode) {
                SwitchItem(
                    icon = Icons.Filled.DarkMode,
                    title = darkThemeTitle,
                    summary = null,
                    checked = darkModeEnabled,
                    onCheckedChange = onDarkModeChange
                )
            }
            
            if (showDynamicColor) {
                SwitchItem(
                    icon = Icons.Filled.Palette,
                    title = dynamicColorTitle,
                    summary = dynamicColorSummary,
                    checked = dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChange
                )
            }

            if (showSystemFont) {
                SwitchItem(
                    icon = Icons.Filled.FontDownload,
                    title = systemFontTitle,
                    summary = systemFontSummary,
                    checked = systemFontEnabled,
                    onCheckedChange = onSystemFontChange
                )

                if (!systemFontEnabled) {
                    val fontOptions = listOf(
                        "jetbrains_mono" to R.string.font_jetbrains_mono,
                        "caskaydia_mono" to R.string.font_caskaydia_mono,
                        "overpass" to R.string.font_overpass,
                        "firacode" to R.string.font_firacode,
                        "hurmit" to R.string.font_hurmit,
                        "heavydata" to R.string.font_heavydata,
                        "droid_sans_mono" to R.string.font_droid_sans_mono,
                    )

                    val selectedLabel = fontOptions.firstOrNull { it.first == fontChoice }?.let { stringResource(it.second) }
                        ?: stringResource(fontOptions[0].second)
                    var showFontPicker by remember { mutableStateOf(false) }

                    ListItem(
                        headlineContent = { Text(selectedLabel) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clickable { showFontPicker = true }
                            .padding(horizontal = 8.dp)
                    )

                    if (showFontPicker) {
                        ModalBottomSheet(
                            onDismissRequest = { showFontPicker = false },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.select_font),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                                )
                                fontOptions.forEach { (key, labelRes) ->
                                    ListItem(
                                        headlineContent = { Text(stringResource(labelRes)) },
                                        leadingContent = {
                                            RadioButton(
                                                selected = key == fontChoice,
                                                onClick = {
                                                    onFontChoiceChange(key)
                                                    showFontPicker = false
                                                }
                                            )
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier
                                            .clickable {
                                                onFontChoiceChange(key)
                                                showFontPicker = false
                                            }
                                            .padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showColorPalette) {
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(colorPaletteTitle) },
                    supportingContent = {
                        Text(
                            text = colorPaletteSummary,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.FormatColorFill,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        ColorSwatch(hexToColor(currentColorHex))
                    },
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    modifier = Modifier
                        .clickable(enabled = true, onClick = onPaletteClick)
                        .padding(horizontal = 8.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.color_presets),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ColorPresetItem("#00E5FF", R.string.preset_electric_cyan, currentColorHex, onPresetSelected)
                    ColorPresetItem("#FFB487", R.string.preset_original_orange, currentColorHex, onPresetSelected)
                    ColorPresetItem("#39FF14", R.string.preset_neon_green, currentColorHex, onPresetSelected)
                    ColorPresetItem("#FF1493", R.string.preset_hot_pink, currentColorHex, onPresetSelected)
                    ColorPresetItem("#BB86FC", R.string.preset_royal_purple, currentColorHex, onPresetSelected)
                    ColorPresetItem("#03DAC6", R.string.preset_sky_blue, currentColorHex, onPresetSelected)
                    ColorPresetItem("#CF6679", R.string.preset_crimson_red, currentColorHex, onPresetSelected)
                    ColorPresetItem("#FF6D00", R.string.preset_sunset_orange, currentColorHex, onPresetSelected)
                }
            }

            if (showSecondary) {
                ColorStripeRow(
                    icon = Icons.Filled.FormatColorFill,
                    title = secondaryTitle,
                    colorHex = secondaryColorHex,
                    onPaletteClick = onSecondaryPaletteClick
                )
            }

            if (showTertiary) {
                ColorStripeRow(
                    icon = Icons.Filled.FormatColorFill,
                    title = tertiaryTitle,
                    colorHex = tertiaryColorHex,
                    onPaletteClick = onTertiaryPaletteClick
                )
            }

            if (showAmoled) {
                SwitchItem(
                    icon = Icons.Filled.DarkMode,
                    title = amoledTitle,
                    summary = amoledSummary,
                    checked = amoledEnabled,
                    onCheckedChange = onAmoledChange
                )
            }

            if (showCorner) {
                val cornerOptions = listOf(
                    CORNER_STYLE_DEFAULT to R.string.corner_style_default,
                    CORNER_STYLE_SQUARED to R.string.corner_style_squared,
                    CORNER_STYLE_ROUNDED to R.string.corner_style_rounded,
                    CORNER_STYLE_EXTRA_ROUNDED to R.string.corner_style_extra_rounded,
                )
                val cornerLabel = cornerOptions.firstOrNull { it.first == cornerStyle }
                    ?.let { stringResource(it.second) }
                    ?: stringResource(R.string.corner_style_default)
                var showCornerPicker by remember { mutableStateOf(false) }

                ClickableItem(
                    icon = Icons.Filled.CropSquare,
                    title = cornerTitle,
                    summary = cornerLabel,
                    onClick = { showCornerPicker = true }
                )

                if (showCornerPicker) {
                    ModalBottomSheet(
                        onDismissRequest = { showCornerPicker = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.select_corner_style),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                            cornerOptions.forEach { (style, labelRes) ->
                                RadioItem(
                                    title = stringResource(labelRes),
                                    selected = style == cornerStyle,
                                    onClick = {
                                        onCornerStyleChange(style)
                                        showCornerPicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showIntensity) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(intensityTitle) },
                        supportingContent = {
                            Text(
                                text = intensitySummary,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.outline
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Brightness6,
                                contentDescription = null,
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = androidx.compose.material3.ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Slider(
                        value = accentIntensity,
                        onValueChange = onAccentIntensityChange,
                        valueRange = 0.5f..1.5f,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = String.format("%.2fx", accentIntensity),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            if (showBottomBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.ListItem(
                        headlineContent = { Text(bottomBarTitle) },
                        supportingContent = {
                            Text(
                                text = bottomBarSummary,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.outline
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Filled.Expand,
                                contentDescription = null,
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = androidx.compose.material3.ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Slider(
                        value = bottomBarScale,
                        onValueChange = onBottomBarScaleChange,
                        valueRange = 0.5f..1.5f,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = String.format("%.2fx", bottomBarScale),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            if (showBanner) {
                ListItem(
                    headlineContent = { Text(bannerTitle) },
                    supportingContent = {
                        Text(
                            text = if (bannerImagePath == null) {
                                bannerSummary
                            } else {
                                stringResource(R.string.home_banner_set)
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        if (bannerImagePath != null) {
                            IconButton(onClick = onBannerRemove) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable(enabled = true, onClick = onBannerClick)
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorStripeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    colorHex: String?,
    onPaletteClick: () -> Unit
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = if (colorHex != null) colorHex else stringResource(R.string.secondary_tertiary_not_set),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.outline
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            ColorSwatch(hexToColor(colorHex ?: "#00E5FF"))
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        modifier = Modifier
            .clickable(enabled = true, onClick = onPaletteClick)
            .padding(horizontal = 8.dp)
    )
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

@Composable
fun ColorPresetItem(
    hexColor: String,
    labelRes: Int,
    currentColorHex: String,
    onPresetSelected: (String) -> Unit
) {
    val color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hexColor))
    val isSelected = currentColorHex.equals(hexColor, ignoreCase = true)

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onPresetSelected(hexColor) }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(labelRes),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}