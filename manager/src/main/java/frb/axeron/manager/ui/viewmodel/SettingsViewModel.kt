package frb.axeron.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.core.AxeronSettings
import frb.axeron.manager.ui.theme.basePrimaryDefault
import frb.axeron.manager.ui.theme.toHexString
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
//    private val prefs = AxeronSettings.getPreferences()

    val themeOptions = listOf("Follow System", "Dark Theme", "Light Theme")

    var isAutoRestartEnabled by mutableStateOf(
        AxeronSettings.getEnableAutoRestart()
    )
        private set

    var isIgniteWhenRelogEnabled by mutableStateOf(
        AxeronSettings.getEnableIgniteRelog()
    )
        private set

    var isActivateOnBootEnabled by mutableStateOf(
        AxeronSettings.getStartOnBoot()
    )
        private set

    var isTcpModeEnabled by mutableStateOf(
        AxeronSettings.getTcpMode()
    )
        private set

    var tcpPortInt: Int? by mutableStateOf(
        AxeronSettings.getTcpPort()
    )
        private set

    var isDynamicColorEnabled by mutableStateOf(
        AxeronSettings.getEnableDynamicColor()
    )
        private set

    var getAppThemeId by mutableIntStateOf(
        AxeronSettings.getAppThemeId()
    )
        private set

    var isDeveloperModeEnabled by mutableStateOf(
        AxeronSettings.getEnableDeveloperOptions()
    )
        private set

    var isWebDebuggingEnabled by mutableStateOf(
        AxeronSettings.getEnableWebDebugging()
    )
        private set

    var isSystemFontEnabled by mutableStateOf(
        AxeronSettings.getSystemFont()
    )
        private set

    var fontChoice by mutableStateOf(
        AxeronSettings.getFontChoice()
    )
        private set

    // fungsi toggle / set manual

    fun setAutoRestart(enabled: Boolean) {
        viewModelScope.launch {
            isAutoRestartEnabled = enabled
            AxeronSettings.setEnableAutoRestart(enabled)
        }
    }

    fun setIgniteWhenRelog(enabled: Boolean) {
        viewModelScope.launch {
            isIgniteWhenRelogEnabled = enabled
            AxeronSettings.setEnableIgniteRelog(enabled)
        }
    }

    fun setActivateOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            isActivateOnBootEnabled = enabled
            AxeronSettings.setStartOnBoot(enabled)
        }
    }

    fun setTcpMode(enabled: Boolean) {
        viewModelScope.launch {
            isTcpModeEnabled = enabled
            AxeronSettings.setTcpMode(enabled)
        }
    }

    fun setTcpPort(port: Int?) {
        viewModelScope.launch {
            tcpPortInt = port
            AxeronSettings.setTcpPort(port)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            isDynamicColorEnabled = enabled
            AxeronSettings.setEnableDynamicColor(enabled)
        }
    }

    fun setAppTheme(themeId: Int) {
        viewModelScope.launch {
            getAppThemeId = themeId
            AxeronSettings.setAppThemeId(themeId)
        }
    }

    fun setDeveloperOptions(enabled: Boolean) {
        viewModelScope.launch {
            isDeveloperModeEnabled = enabled
            AxeronSettings.setEnableDeveloperOptions(enabled)
        }
    }

    fun setWebDebugging(enabled: Boolean) {
        viewModelScope.launch {
            isWebDebuggingEnabled = enabled
            AxeronSettings.setEnableWebDebugging(enabled)
        }
    }

    fun setSystemFont(enabled: Boolean) {
        viewModelScope.launch {
            isSystemFontEnabled = enabled
            AxeronSettings.setSystemFont(enabled)
        }
    }

    fun updateFontChoice(choice: String) {
        viewModelScope.launch {
            fontChoice = choice
            AxeronSettings.setFontChoice(choice)
        }
    }

    var customPrimaryColorHex by mutableStateOf(
        AxeronSettings.getCustomPrimaryColor() ?: basePrimaryDefault.toHexString()
    )
        private set

    fun setCustomPrimaryColor(hex: String) {
        viewModelScope.launch {
            customPrimaryColorHex = hex
            AxeronSettings.setPrimaryColor(hex)
        }
    }

    fun removeCustomPrimaryColor() {
        viewModelScope.launch {
            customPrimaryColorHex = basePrimaryDefault.toHexString()
            AxeronSettings.removePrimaryColor()
        }
    }

    var customSecondaryColorHex by mutableStateOf(
        AxeronSettings.getCustomSecondaryColor()
    )
        private set

    fun setCustomSecondaryColor(hex: String) {
        viewModelScope.launch {
            customSecondaryColorHex = hex
            AxeronSettings.setSecondaryColor(hex)
        }
    }

    fun removeCustomSecondaryColor() {
        viewModelScope.launch {
            customSecondaryColorHex = null
            AxeronSettings.removeSecondaryColor()
        }
    }

    var customTertiaryColorHex by mutableStateOf(
        AxeronSettings.getCustomTertiaryColor()
    )
        private set

    fun setCustomTertiaryColor(hex: String) {
        viewModelScope.launch {
            customTertiaryColorHex = hex
            AxeronSettings.setTertiaryColor(hex)
        }
    }

    fun removeCustomTertiaryColor() {
        viewModelScope.launch {
            customTertiaryColorHex = null
            AxeronSettings.removeTertiaryColor()
        }
    }

    var isAmoledEnabled by mutableStateOf(
        AxeronSettings.getAmoledMode()
    )
        private set

    fun setAmoled(enabled: Boolean) {
        viewModelScope.launch {
            isAmoledEnabled = enabled
            AxeronSettings.setAmoledMode(enabled)
        }
    }

    var cornerStyle by mutableIntStateOf(
        AxeronSettings.getCornerStyle()
    )
        private set

    fun updateCornerStyle(style: Int) {
        viewModelScope.launch {
            cornerStyle = style
            AxeronSettings.setCornerStyle(style)
        }
    }

    var accentIntensity by mutableStateOf(
        AxeronSettings.getAccentIntensity()
    )
        private set

    fun updateAccentIntensity(intensity: Float) {
        viewModelScope.launch {
            accentIntensity = intensity
            AxeronSettings.setAccentIntensity(intensity)
        }
    }

    var bottomBarScale by mutableStateOf(
        AxeronSettings.getBottomBarScale()
    )
        private set

    fun updateBottomBarScale(scale: Float) {
        viewModelScope.launch {
            bottomBarScale = scale
            AxeronSettings.setBottomBarScale(scale)
        }
    }

    var bannerImagePath by mutableStateOf(
        AxeronSettings.getHomeBanner()
    )
        private set

    fun setBannerImage(path: String) {
        viewModelScope.launch {
            bannerImagePath = path
            AxeronSettings.setHomeBanner(path)
        }
    }

    fun removeBannerImage() {
        viewModelScope.launch {
            bannerImagePath = null
            AxeronSettings.removeHomeBanner()
        }
    }

}