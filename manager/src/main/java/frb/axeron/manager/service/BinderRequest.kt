package frb.axeron.manager.service

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import frb.axeron.api.Axeron
import frb.axeron.server.ServerConstants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Asks the detached server to hand us its binder.
 *
 * The server normally pushes the binder at startup and whenever its process/uid observers fire.
 * Those observers are best-effort: after a force-stop the app process is recreated under the
 * same uid, and if the platform does not deliver the callbacks the push never happens. The app
 * then shows "Need to Activate" even though a healthy server is holding its binder, and the
 * only escape is a manual re-activate.
 *
 * This is the deterministic counterpart - the server registers a receiver for
 * [ServerConstants.REQUEST_BINDER_DIRECT] and transacts straight back into [replyTarget].
 * It is a belt-and-braces path; the server-side dedup pruning in BinderSender is the real fix.
 */
object BinderRequest {

    private const val TAG = "BinderRequest"

    /**
     * Transaction code the server answers on. Matches the contract used by ShellLoader.
     */
    private const val CODE_ON_BINDER = 1

    private const val BINDER_RETRY_INTERVAL_MS = 500L
    private const val DEFAULT_BINDER_TIMEOUT_MS = 5_000L

    /**
     * Target for the server's reply. Created once and reused so we never leak a Binder.
     *
     * Deliberately not a lambda/SAM: the reply arrives on a binder thread via transact, and we
     * hand the result straight to [Axeron.onBinderReceived], which schedules the sticky
     * listeners and fans out to the UI.
     */
    private val replyTarget = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code != CODE_ON_BINDER) return super.onTransact(code, data, reply, flags)

            val binder = data.readStrongBinder()
            val sourceDir = data.readString()

            if (binder == null || sourceDir == null) {
                Log.w(TAG, "server replied with an empty binder")
                return true
            }

            val packageName = this@BinderRequest.context?.packageName
            if (packageName == null) {
                Log.w(TAG, "discarding a binder reply with no known package name")
                return true
            }

            // Must run before the listeners fire, since they read from Axeron.
            Axeron.onBinderReceived(binder, packageName)
            Log.i(TAG, "received binder on request from server")
            return true
        }
    }

    private var context: Context? = null

    /**
     * Sends one request. Safe to call repeatedly; a reply for a request we no longer care
     * about is harmless because [Axeron.onBinderReceived] is a no-op when the binder is
     * already live.
     */
    fun request(context: Context) {
        this.context = context.applicationContext

        try {
            val bundle = Bundle().apply {
                putBinder("binder", replyTarget)
            }

            val intent = Intent(ServerConstants.REQUEST_BINDER_DIRECT)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .putExtra("data", bundle)

            context.applicationContext.sendBroadcast(intent)
            Log.i(TAG, "requested binder from server")
        } catch (t: Throwable) {
            Log.w(TAG, "binder request failed", t)
        }
    }

    suspend fun awaitBinder(
        context: Context,
        timeoutMs: Long = DEFAULT_BINDER_TIMEOUT_MS
    ): Boolean {
        if (Axeron.pingBinder()) return true

        val received = CompletableDeferred<Unit>()
        val listener = object : Axeron.OnBinderReceivedListener {
            override fun onBinderReceived() {
                if (Axeron.pingBinder()) received.complete(Unit)
            }
        }

        Axeron.addBinderReceivedListenerSticky(listener)
        try {
            val connected = withTimeoutOrNull(timeoutMs) {
                while (!Axeron.pingBinder()) {
                    request(context)
                    withTimeoutOrNull(BINDER_RETRY_INTERVAL_MS) {
                        received.await()
                    }
                    if (!Axeron.pingBinder() && received.isCompleted) {
                        delay(BINDER_RETRY_INTERVAL_MS)
                    }
                }
                true
            }
            return connected == true || Axeron.pingBinder()
        } finally {
            Axeron.removeBinderReceivedListener(listener)
        }
    }
}
