package frb.axeron.manager.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import frb.axeron.manager.ui.component.SettingsCategory
import frb.axeron.manager.ui.component.SwitchItem

@Composable
fun AppearanceSettings(
    searchText: String,
    autoThemeEnabled: Boolean,
    onAutoThemeChange: (Boolean) -> Unit,
    darkModeEnabled: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
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

    val systemFontTitle = stringResource(R.string.system_font)
    val systemFontSummary = stringResource(R.string.system_font_desc)
    val showSystemFont = matchCategory || shouldShow(searchText, systemFontTitle, systemFontSummary)

    val showCategory = showLanguage || showAutoTheme || showDarkMode || showDynamicColor || showColorPalette || showSystemFont
    
    if (showCategory) {
        SettingsCategory(
            icon = Icons.Filled.Palette,
            title = categoryTitle,
            isSearching = searchText.isNotEmpty()
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

                    var expanded by remember { mutableStateOf(false) }
                    val selectedLabel = fontOptions.firstOrNull { it.first == fontChoice }?.let { stringResource(it.second) }
                        ?: stringResource(fontOptions[0].second)

                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            fontOptions.forEach { (key, labelRes) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(labelRes)) },
                                    onClick = {
                                        onFontChoiceChange(key)
                                        expanded = false
                                    }
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expanded = !expanded }
                        )
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

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    ColorPresetItem("#00E5FF", R.string.preset_electric_cyan, currentColorHex, onPresetSelected)
                    ColorPresetItem("#FFB487", R.string.preset_original_orange, currentColorHex, onPresetSelected)
                    ColorPresetItem("#39FF14", R.string.preset_neon_green, currentColorHex, onPresetSelected)
                }

            }
        }
    }
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onPresetSelected(hexColor) }
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
