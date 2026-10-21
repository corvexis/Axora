package frb.axeron.manager.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.manager.data.SettingsRepository
import frb.axeron.manager.data.ToggleConfig
import frb.axeron.manager.data.ToggleConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ToggleViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ToggleConfigStore(application)
    private val repository = SettingsRepository(application.contentResolver)

    var configs by mutableStateOf(store.load())
        private set

    var values by mutableStateOf<Map<String, String?>>(emptyMap())
        private set

    var busyIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var failedLabel by mutableStateOf<String?>(null)
        private set

    init {
        refreshAll()
    }

    fun refreshAll() {
        val snapshot = configs
        if (snapshot.isEmpty()) {
            values = emptyMap()
            return
        }
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                snapshot.associate { config ->
                    config.id to repository.getValue(config.type, config.key)
                }
            }
            values = values + loaded.filterKeys { it !in busyIds }
        }
    }

    suspend fun loadKeys(type: SettingsRepository.SettingType): List<String> =
        withContext(Dispatchers.IO) {
            repository.getAll(type).keys.sorted()
        }

    fun upsert(config: ToggleConfig): Boolean {
        val duplicate = configs.any {
            it.id != config.id && it.type == config.type && it.key == config.key
        }
        if (duplicate) return false

        configs = if (configs.any { it.id == config.id }) {
            configs.map { if (it.id == config.id) config else it }
        } else {
            configs + config
        }
        store.save(configs)
        refreshValue(config)
        return true
    }

    fun delete(config: ToggleConfig) {
        configs = configs.filterNot { it.id == config.id }
        values = values - config.id
        store.save(configs)
    }

    fun toggle(config: ToggleConfig, enabled: Boolean) {
        if (config.id in busyIds) return

        val targetValue = if (enabled) config.onValue else config.offValue
        busyIds = busyIds + config.id

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                repository.putValue(config.type, config.key, targetValue)
            }
            if (success) {
                values = values + (config.id to targetValue)
            } else {
                failedLabel = config.displayLabel
            }
            busyIds = busyIds - config.id
        }
    }

    fun clearError() {
        failedLabel = null
    }

    private fun refreshValue(config: ToggleConfig) {
        viewModelScope.launch {
            val value = withContext(Dispatchers.IO) {
                repository.getValue(config.type, config.key)
            }
            values = values + (config.id to value)
        }
    }
}
