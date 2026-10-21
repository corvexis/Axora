package frb.axeron.manager.ui.webui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import coil.Coil
import coil.request.ImageRequest
import frb.axeron.api.Axeron
import frb.axeron.manager.R
import frb.axeron.manager.ui.util.squirclePath
import frb.axeron.manager.ui.util.toBitmapSafely
import frb.axeron.server.PluginInfo
import frb.axeron.shared.AxeronApiConstant
import frb.axeron.shared.PathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object WebUIShortcutHelper {

    private const val ICON_DIR = "webui_shortcut_icons"
    private const val PREFS_NAME = "webui_shortcut_settings"
    private const val NAME_KEY_PREFIX = "name_"
    private const val SHORTCUT_ID_PREFIX = "webui_"
    private const val MIN_ICON_SIZE = 96
    private const val MAX_ICON_SIZE = 384
    private const val BANNER_ICON_ZOOM = 1.15f
    private val BANNER_ICON_BACKGROUND = Color.rgb(13, 13, 43)

    enum class ShortcutResult {
        PINNED,
        UPDATED,
        UNSUPPORTED,
        FAILED
    }

    fun hasCustomIcon(context: Context, pluginId: String): Boolean =
        iconFile(context, pluginId).isFile

    fun deleteCustomIcon(context: Context, pluginId: String) {
        iconFile(context, pluginId).delete()
    }

    fun deleteShortcutName(context: Context, pluginId: String) {
        prefs(context).edit().remove(NAME_KEY_PREFIX + pluginId).apply()
    }

    fun clearShortcutData(context: Context, pluginId: String) {
        deleteCustomIcon(context, pluginId)
        deleteShortcutName(context, pluginId)
    }

    fun savedShortcutName(context: Context, pluginId: String): String? =
        prefs(context).getString(NAME_KEY_PREFIX + pluginId, null)

    fun saveShortcutName(context: Context, pluginId: String, name: String) {
        prefs(context).edit().putString(NAME_KEY_PREFIX + pluginId, name).apply()
    }

    suspend fun getShortcutName(context: Context, plugin: PluginInfo): String =
        withContext(Dispatchers.IO) {
            val pinned = pinnedShortcuts(context)
            val id = resolveShortcutId(pinned, plugin.prop.id)
            savedShortcutName(context, plugin.prop.id)
                ?: pinned.firstOrNull { it.id == id }?.shortLabel?.toString()?.takeIf { it.isNotBlank() }
                ?: plugin.prop.name
        }

    suspend fun isShortcutPinned(context: Context, pluginId: String): Boolean =
        withContext(Dispatchers.IO) {
            val pinned = pinnedShortcuts(context)
            val id = resolveShortcutId(pinned, pluginId)
            pinned.any { it.id == id }
        }

    suspend fun loadConfiguredIcon(
        context: Context,
        plugin: PluginInfo,
        bannerData: ByteArray? = null
    ): Bitmap? {
        return loadCustomIcon(context, plugin.prop.id)
            ?: loadBannerIcon(context, plugin, bannerData)
            ?: loadDefaultIcon(context)
    }

    suspend fun loadCustomIcon(context: Context, pluginId: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val file = iconFile(context, pluginId)
            if (!file.isFile) return@withContext null
            runCatching {
                BitmapFactory.decodeFile(file.absolutePath)?.let {
                    createSquircleCover(it, iconTargetSize(context))
                }
            }.getOrNull()
        }

    suspend fun loadIconFromUri(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            val target = iconTargetSize(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .size(target)
                .build()

            val drawable = runCatching { Coil.imageLoader(context).execute(request).drawable }
                .getOrNull() ?: return@withContext null
            createSquircleCover(drawable.toBitmapSafely(target), target)
        }

    suspend fun saveCustomIconFromUri(
        context: Context,
        pluginId: String,
        uri: Uri
    ): Bitmap? {
        val icon = loadIconFromUri(context, uri) ?: return null
        return if (saveCustomIconBitmap(context, pluginId, icon)) icon else null
    }

    suspend fun saveCustomIconBitmap(
        context: Context,
        pluginId: String,
        bitmap: Bitmap
    ): Boolean = withContext(Dispatchers.IO) {
        writeBitmap(iconFile(context, pluginId), bitmap)
    }

    suspend fun pinOrUpdateShortcut(
        context: Context,
        plugin: PluginInfo,
        name: String,
        bannerData: ByteArray? = null,
        iconOverride: Bitmap? = null
    ): ShortcutResult = withContext(Dispatchers.IO) {
        val pinned = pinnedShortcuts(context)
        val id = resolveShortcutId(pinned, plugin.prop.id)
        val icon = iconOverride ?: loadConfiguredIcon(context, plugin, bannerData)
        val shortcut = buildShortcut(context, plugin, id, name, icon)

        if (pinned.any { it.id == id }) {
            return@withContext if (runCatching {
                    ShortcutManagerCompat.updateShortcuts(context, listOf(shortcut))
                }.getOrDefault(false)
            ) ShortcutResult.UPDATED else ShortcutResult.FAILED
        }

        if (!runCatching {
                ShortcutManagerCompat.isRequestPinShortcutSupported(context)
            }.getOrDefault(false)
        ) {
            return@withContext ShortcutResult.UNSUPPORTED
        }

        return@withContext if (runCatching {
                ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
            }.getOrDefault(false)
        ) ShortcutResult.PINNED else ShortcutResult.FAILED
    }

    suspend fun updatePinnedShortcut(
        context: Context,
        plugin: PluginInfo,
        bannerData: ByteArray? = null,
        name: String? = null
    ): ShortcutResult? = withContext(Dispatchers.IO) {
        val pinned = pinnedShortcuts(context)
        val id = resolveShortcutId(pinned, plugin.prop.id)
        val existing = pinned.firstOrNull { it.id == id } ?: return@withContext null

        val resolvedName = name
            ?: savedShortcutName(context, plugin.prop.id)
            ?: existing.shortLabel?.toString()?.takeIf { it.isNotBlank() }
            ?: plugin.prop.name
        val icon = loadConfiguredIcon(context, plugin, bannerData)

        val shortcut = buildShortcut(context, plugin, id, resolvedName, icon)
        return@withContext if (runCatching {
                ShortcutManagerCompat.updateShortcuts(context, listOf(shortcut))
            }.getOrDefault(false)
        ) ShortcutResult.UPDATED else ShortcutResult.FAILED
    }

    private fun buildShortcut(
        context: Context,
        plugin: PluginInfo,
        id: String,
        name: String,
        icon: Bitmap?
    ): ShortcutInfoCompat {
        val iconCompat = icon?.let { IconCompat.createWithBitmap(it) }
            ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)

        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(name)
            .setLongLabel(name)
            .setIcon(iconCompat)
            .setIntent(
                Intent(context, WebUIActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("id", plugin.prop.id)
                }
            )
            .build()
    }

    private suspend fun loadBannerIcon(
        context: Context,
        plugin: PluginInfo,
        bannerData: ByteArray?
    ): Bitmap? {
        val banner = plugin.prop.banner.trim()
        if (banner.isEmpty()) return null

        val target = iconTargetSize(context)
        val data: Any = when {
            bannerData != null && bannerData.isNotEmpty() -> bannerData
            banner.startsWith("http://", true) || banner.startsWith("https://", true) -> banner
            else -> withContext(Dispatchers.IO) {
                runCatching {
                    val isRoot = Axeron.getAxeronInfo().isRoot()
                    val parent = PathHelper.getWorkingPath(
                        isRoot,
                        AxeronApiConstant.folder.PARENT_PLUGIN
                    )
                    val bannerFile = File(File(parent, plugin.prop.id), banner)
                    Axeron.newFileService()
                        .setFileInputStream(bannerFile.absolutePath)
                        ?.use { it.readBytes() }
                }.getOrNull()
            } ?: return null
        }

        val request = ImageRequest.Builder(context)
            .data(data)
            .allowHardware(false)
            .size(target)
            .build()

        val drawable = runCatching { Coil.imageLoader(context).execute(request).drawable }
            .getOrNull() ?: return null
        return createBannerIcon(drawable.toBitmapSafely(target), target)
    }

    private fun createSquircleCover(source: Bitmap, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        drawCover(canvas, source, size, size, paint)
        applySquircleMask(canvas, size)
        return output
    }

    private fun createBannerIcon(source: Bitmap, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(BANNER_ICON_BACKGROUND)
        drawCover(canvas, source, size, size, paint, BANNER_ICON_ZOOM)
        applySquircleMask(canvas, size)
        return output
    }

    private suspend fun loadDefaultIcon(context: Context): Bitmap? =
        withContext(Dispatchers.IO) {
            val target = iconTargetSize(context)
            runCatching {
                val drawable = context.getDrawable(R.mipmap.ic_launcher)
                    ?: return@runCatching null
                createSquircleCover(drawable.toBitmapSafely(target), target)
            }.getOrNull()
        }

    private fun applySquircleMask(canvas: Canvas, size: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawPath(squirclePath(size), paint)
        paint.xfermode = null
    }

    private fun drawCover(
        canvas: Canvas,
        source: Bitmap,
        width: Int,
        height: Int,
        paint: Paint,
        zoom: Float = 1f
    ) {
        val scale = max(
            width.toFloat() / source.width,
            height.toFloat() / source.height
        ) * zoom
        val drawnWidth = source.width * scale
        val drawnHeight = source.height * scale
        val left = (width - drawnWidth) / 2f
        val top = (height - drawnHeight) / 2f
        canvas.drawBitmap(
            source,
            null,
            RectF(left, top, left + drawnWidth, top + drawnHeight),
            paint
        )
    }

    private fun writeBitmap(file: File, bitmap: Bitmap): Boolean {
        return runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            val compressed = FileOutputStream(temp).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (!compressed) {
                temp.delete()
                return@runCatching false
            }
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
            true
        }.getOrDefault(false)
    }

    private fun iconTargetSize(context: Context): Int {
        val width = runCatching { ShortcutManagerCompat.getIconMaxWidth(context) }
            .getOrDefault(192)
        val height = runCatching { ShortcutManagerCompat.getIconMaxHeight(context) }
            .getOrDefault(192)
        return min(width, height).coerceIn(MIN_ICON_SIZE, MAX_ICON_SIZE)
    }

    private fun pinnedShortcuts(context: Context): List<ShortcutInfoCompat> =
        runCatching {
            ShortcutManagerCompat.getShortcuts(
                context,
                ShortcutManagerCompat.FLAG_MATCH_PINNED
            )
        }.getOrDefault(emptyList())

    private fun resolveShortcutId(
        pinned: List<ShortcutInfoCompat>,
        pluginId: String
    ): String {
        if (pinned.any { it.id == pluginId }) return pluginId
        return SHORTCUT_ID_PREFIX + pluginId
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun iconFile(context: Context, pluginId: String): File {
        val safeName = pluginId
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(80)
            .ifBlank { "plugin" }
        val unique = Integer.toHexString(pluginId.hashCode())
        return File(
            File(context.filesDir, ICON_DIR),
            "${safeName}_$unique.png"
        )
    }
}
