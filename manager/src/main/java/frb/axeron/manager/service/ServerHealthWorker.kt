package frb.axeron.manager.service

import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.topjohnwu.superuser.Shell
import frb.axeron.api.Axeron
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.adb.AdbStarter

class ServerHealthWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "ServerHealthWorker"
    }

    override suspend fun doWork(): Result {
        if (!AxeronSettings.getEnableAutoRestart()) return Result.success()

        if (Axeron.pingBinder()) return Result.success()

        if (!AxeronSettings.getWasRunning()) return Result.success()

        Log.w(TAG, "Server is dead — attempting auto-restart from WorkManager")

        when (AxeronSettings.getLastLaunchMode()) {
            AxeronSettings.LaunchMethod.ROOT -> tryRootRestart()
            AxeronSettings.LaunchMethod.ADB -> tryAdbRestart()
        }

        return Result.success()
    }

    private fun tryRootRestart() {
        runCatching {
            if (Shell.getShell().isRoot) {
                Shell.cmd(Starter.internalCommand).exec()
                Log.i(TAG, "Auto-restart via root: command sent")
            } else {
                Log.w(TAG, "Auto-restart via root: no root access")
            }
        }.onFailure {
            Log.e(TAG, "Auto-restart via root failed", it)
        }
    }

    private suspend fun tryAdbRestart() {
        val tcpPort = AxeronSettings.getTcpPort()
        if (tcpPort > 0) {
            runCatching {
                AdbStarter.startAdbClient(applicationContext, tcpPort) {}
                Log.i(TAG, "Auto-restart via ADB TCP: command sent to port $tcpPort")
            }.onFailure {
                Log.e(TAG, "Auto-restart via ADB failed", it)
            }
        } else {
            Log.w(TAG, "Auto-restart via ADB: no TCP port configured")
        }
    }
}
