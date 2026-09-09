package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.manager.ui.util.HanziToPinyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DisableAppsViewModel(application: Application) : AndroidViewModel(application) {

    data class AppEntry(
        val packageName: String,
        val label: String,
        val disabled: Boolean,
    )

    enum class AppFilter { ALL, DISABLED, ENABLED }

    var search by mutableStateOf("")
    var filter by mutableStateOf(AppFilter.ALL)

    var entries by mutableStateOf<List<AppEntry>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var busyPackage by mutableStateOf<String?>(null)
        private set

    @Volatile
    private var scanning = false

    val filteredEntries by derivedStateOf {
        entries.filter { entry ->
            val matchesSearch = entry.label.contains(search, ignoreCase = true) ||
                    entry.packageName.contains(search, ignoreCase = true) ||
                    HanziToPinyin.getInstance().toPinyinString(entry.label)
                        .contains(search, ignoreCase = true)
            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.DISABLED -> entry.disabled
                AppFilter.ENABLED -> !entry.disabled
            }
            matchesSearch && matchesFilter
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val cached = readCacheMap()
            if (cached.isNotEmpty()) {
                entries = cached.values.toList().sortedSelf()
            } else {
                rescan()
            }
        }
    }

    fun mergeNew() {
        if (scanning) return
        viewModelScope.launch(Dispatchers.IO) {
            scanning = true
            try {
                val packages = Axeron.getPackages(0)
                val pm = getApplication<Application>().packageManager
                val map = readCacheMap()
                var changed = false
                packages.forEach { pk ->
                    val name = pk.packageName ?: return@forEach
                    if (name !in map && isThirdParty(pk)) {
                        map[name] = AppEntry(name, loadLabel(pk, pm), false)
                        changed = true
                    }
                }
                if (changed) {
                    writeCacheMap(map)
                    entries = map.values.toList().sortedSelf()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                scanning = false
            }
        }
    }

    fun rescan() {
        if (scanning) return
        viewModelScope.launch(Dispatchers.IO) {
            scanning = true
            loading = true
            try {
                val packages = Axeron.getPackages(0)
                val disabledSet = readDisabledPackages()
                val pm = getApplication<Application>().packageManager
                val cached = readCacheMap()
                val next = LinkedHashMap<String, AppEntry>()
                packages.forEach { pk ->
                    val name = pk.packageName ?: return@forEach
                    if (!isThirdParty(pk)) return@forEach
                    val label = loadLabel(pk, pm)
                    val cachedEntry = cached[name]
                    val disabled = if (disabledSet != null) name in disabledSet else cachedEntry?.disabled ?: false
                    next[name] = cachedEntry?.copy(label = label, disabled = disabled)
                        ?: AppEntry(name, label, disabled)
                }
                writeCacheMap(next)
                entries = next.values.toList().sortedSelf()
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                loading = false
                scanning = false
            }
        }
    }

    fun toggle(app: AppEntry, checked: Boolean) {
        if (checked == app.disabled || busyPackage != null) return
        busyPackage = app.packageName
        viewModelScope.launch(Dispatchers.IO) {
            val success = try {
                val cmd = if (checked) {
                    arrayOf("pm", "disable-user", "--user", "0", app.packageName)
                } else {
                    arrayOf("pm", "enable", app.packageName)
                }
                val process = Axeron.newProcess(cmd)
                process.waitFor()
                process.exitValue() == 0
            } catch (e: Throwable) {
                false
            }
            if (success) {
                val map = readCacheMap()
                map[app.packageName] = app.copy(disabled = checked)
                writeCacheMap(map)
                entries = entries.map {
                    if (it.packageName == app.packageName) it.copy(disabled = checked) else it
                }.sortedSelf()
            }
            busyPackage = null
        }
    }

    // ==== Cache (app-private, no root needed) ====

    private fun cacheFile() = File(getApplication<Application>().filesDir, "disable_apps_cache.json")

    private fun readCacheMap(): LinkedHashMap<String, AppEntry> = synchronized(cacheLock) {
        val file = cacheFile()
        if (!file.exists()) return@synchronized LinkedHashMap()
        return try {
            val arr = JSONArray(file.readText())
            LinkedHashMap<String, AppEntry>().apply {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.getString("p")
                    put(name, AppEntry(name, obj.optString("l", name), obj.optBoolean("d", false)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LinkedHashMap()
        }
    }

    private fun writeCacheMap(map: LinkedHashMap<String, AppEntry>) = synchronized(cacheLock) {
        try {
            val arr = JSONArray()
            map.values.forEach { entry ->
                arr.put(
                    JSONObject().apply {
                        put("p", entry.packageName)
                        put("l", entry.label)
                        put("d", entry.disabled)
                    }
                )
            }
            cacheFile().writeText(arr.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val cacheLock = Any()

    // ==== Helpers ====

    private fun readDisabledPackages(): Set<String>? = try {
        val process = Axeron.newProcess(arrayOf("pm", "list", "packages", "-d"))
        process.inputStream.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                line.trim().removePrefix("package:").takeIf { it.isNotEmpty() }
            }.toSet()
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }

    private fun isThirdParty(pk: PackageInfo): Boolean {
        val appInfo = pk.applicationInfo ?: return false
        return pk.packageName != getApplication<Application>().packageName &&
                appInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0
    }

    private fun loadLabel(pk: PackageInfo, pm: PackageManager): String =
        pk.applicationInfo?.loadLabel(pm)?.toString() ?: pk.packageName

    private fun List<AppEntry>.sortedSelf() = sortedWith(
        compareByDescending<AppEntry> { it.disabled }.thenBy { it.label.lowercase() }
    )
}