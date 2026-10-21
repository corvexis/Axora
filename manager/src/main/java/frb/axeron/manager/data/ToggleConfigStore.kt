package frb.axeron.manager.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ToggleConfigStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<ToggleConfig> {
        val raw = prefs.getString(KEY_CONFIGS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        ToggleConfig(
                            id = item.getString("id"),
                            type = SettingsRepository.SettingType.valueOf(
                                item.getString("type")
                            ),
                            key = item.getString("key"),
                            label = item.optString("label"),
                            offValue = item.getString("offValue"),
                            onValue = item.getString("onValue")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(configs: List<ToggleConfig>) {
        val array = JSONArray()
        configs.forEach { config ->
            array.put(
                JSONObject().apply {
                    put("id", config.id)
                    put("type", config.type.name)
                    put("key", config.key)
                    put("label", config.label)
                    put("offValue", config.offValue)
                    put("onValue", config.onValue)
                }
            )
        }
        prefs.edit().putString(KEY_CONFIGS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "toggle_shortcuts"
        const val KEY_CONFIGS = "configs"
    }
}
