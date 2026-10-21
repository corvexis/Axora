package frb.axeron.manager.data

import java.util.UUID

data class ToggleConfig(
    val id: String = UUID.randomUUID().toString(),
    val type: SettingsRepository.SettingType,
    val key: String,
    val label: String,
    val offValue: String,
    val onValue: String
) {
    val displayLabel: String
        get() = label.ifBlank { key }
}
