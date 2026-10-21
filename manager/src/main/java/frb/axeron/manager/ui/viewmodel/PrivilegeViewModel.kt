package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.system.Os
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import frb.axeron.api.Axeron
import frb.axeron.manager.ui.util.HanziToPinyin
import frb.axeron.manager.ui.viewmodel.AppsViewModel
import frb.axeron.server.ServerConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrivilegeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val FLAG_ALLOWED = 1 shl 1
        private const val FLAG_DENIED = 1 shl 2
        private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED
        private const val API_PERMISSION = ServerConstants.PERMISSION
        private const val PREFS_NAME = "privilege_cache"
        private const val KEY_PRIVILEGED_COUNT = "privileged_count"
    }

    private val cachePrefs =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var loadJob: Job? = null

    private fun PackageInfo.isCapable(): Boolean =
        requestedPermissions?.contains(API_PERMISSION) == true

    var isRefreshing: Boolean by mutableStateOf(false)
        private set

    var showAllApps: Boolean by mutableStateOf(false)

    var search by mutableStateOf("")

    val privilegeList by derivedStateOf {
        privileges.values
            .asSequence()
            .filter {
                it.label.contains(search, true) ||
                        it.packageName.contains(search, true) ||
                        HanziToPinyin.getInstance()
                            .toPinyinString(it.label)
                            .contains(search, true)
            }
            .filter {
                val isSystem =
                    it.packageInfo.applicationInfo!!
                        .flags.and(ApplicationInfo.FLAG_SYSTEM) != 0

                val isSelf = it.uid == Os.getuid()

                !isSystem && !isSelf
            }
            .filter {
                showAllApps || it.packageInfo.isCapable() || it.isAdded
            }
            .sortedWith(compareByDescending<AppsViewModel.AppInfo> { it.isAdded }.thenBy { it.label })
            .toList()
            .also { isRefreshing = false }
    }


    var privileges by mutableStateOf<Map<Int, AppsViewModel.AppInfo>>(HashMap())
        private set

    var privilegedCount by mutableIntStateOf(
        cachePrefs.getInt(KEY_PRIVILEGED_COUNT, 0)
    )
        private set

    private fun publishPrivilegedCount() {
        val count = privileges.values.count { it.isAdded }
        privilegedCount = count
        cachePrefs.edit().putInt(KEY_PRIVILEGED_COUNT, count).apply()
    }

    fun granted(uid: Int): Boolean {
        return (Axeron.getFlagsForUid(uid, MASK_PERMISSION) and FLAG_ALLOWED) == FLAG_ALLOWED
    }

    fun grant(uid: Int) {
        Axeron.updateFlagsForUid(uid, MASK_PERMISSION, FLAG_ALLOWED)
        privileges = privileges.toMutableMap().apply {
            this[uid]?.let {
                this[uid] = it.copy(isAdded = true)
            }
        }
        publishPrivilegedCount()
    }

    fun revoke(uid: Int) {
        Axeron.updateFlagsForUid(uid, MASK_PERMISSION, 0)
        privileges = privileges.toMutableMap().apply {
            this[uid]?.let {
                this[uid] = it.copy(isAdded = false)
            }
        }
        publishPrivilegedCount()
    }


    private fun getApplications(): List<PackageInfo> {
        return application.packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    }

    fun loadInstalledApps(refresh: Boolean = true) {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            isRefreshing = refresh

            withContext(Dispatchers.IO) {
                try {
                    val pm = getApplication<Application>().packageManager
                    val selfUid = Os.getuid()
                    val grantedCache = HashMap<Int, Boolean>()

                    privileges = getApplications()
                        .asSequence()
                        .filter { packageInfo ->
                            val appInfo = packageInfo.applicationInfo
                            appInfo != null &&
                                    appInfo.uid != selfUid &&
                                    appInfo.flags.and(ApplicationInfo.FLAG_SYSTEM) == 0
                        }
                        .associate { packageInfo ->
                            val appInfo = packageInfo.applicationInfo!!
                            val uid = appInfo.uid
                            uid to AppsViewModel.AppInfo(
                                label = appInfo.loadLabel(pm).toString(),
                                packageInfo = packageInfo,
                                isAdded = grantedCache.getOrPut(uid) { granted(uid) }
                            )
                        }
                    publishPrivilegedCount()
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    isRefreshing = false
                }
            }
        }
    }
}