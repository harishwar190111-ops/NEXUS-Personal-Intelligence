package com.nexus.personalintelligence.tv.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val activityName: String,
    val category: String,
)

/** [isBanner] = a 16:9 TV banner; otherwise a square launcher icon. */
data class AppIcon(val bitmap: ImageBitmap, val isBanner: Boolean)

sealed interface LaunchResult {
    data object Started : LaunchResult
    data class Failed(val reason: String) : LaunchResult
}

/**
 * Real installed-app discovery through PackageManager. Nothing is hard-coded:
 * an app appears only if the system reports a launcher entry for it.
 */
class AppRepository(private val context: Context) {
    private val pm = context.packageManager
    private val icons = LruCache<String, AppIcon>(160)

    suspend fun discover(): List<AppEntry> = withContext(Dispatchers.IO) {
        val own = context.packageName
        val found = LinkedHashMap<String, AppEntry>()
        val categories = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (category in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            val results = try {
                pm.queryIntentActivities(query, 0)
            } catch (e: Exception) {
                emptyList()
            }
            for (ri in results) {
                val info = ri.activityInfo ?: continue
                val pkg = info.packageName
                if (pkg == own || found.containsKey(pkg)) continue
                val label = try {
                    ri.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkg
                }
                val appInfo = info.applicationInfo
                val isSystem = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val category = CategoryLogic.labelFor(appInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED, isSystem)
                found[pkg] = AppEntry(pkg, label.ifBlank { pkg }, info.name, category)
            }
        }
        found.values.sortedBy { it.label.lowercase() }
    }

    suspend fun loadIcon(entry: AppEntry): AppIcon? = withContext(Dispatchers.IO) {
        icons.get(entry.packageName)?.let { return@withContext it }
        val component = ComponentName(entry.packageName, entry.activityName)
        val banner: Drawable? = try {
            pm.getActivityBanner(component)
        } catch (e: Exception) {
            null
        }
        val icon: AppIcon? = try {
            if (banner != null) {
                AppIcon(banner.toBitmap(320, 180, Bitmap.Config.ARGB_8888).asImageBitmap(), true)
            } else {
                AppIcon(pm.getActivityIcon(component).toBitmap(192, 192, Bitmap.Config.ARGB_8888).asImageBitmap(), false)
            }
        } catch (e: Throwable) {
            null
        }
        if (icon != null) icons.put(entry.packageName, icon)
        icon
    }

    /** Uses supported launch intents only. Success means the system accepted the intent, nothing more. */
    fun launch(packageName: String): LaunchResult {
        val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
            ?: return LaunchResult.Failed("this app has no launch entry")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            LaunchResult.Started
        } catch (e: ActivityNotFoundException) {
            LaunchResult.Failed("it could not be found")
        } catch (e: SecurityException) {
            LaunchResult.Failed("the system blocked it")
        } catch (e: Exception) {
            LaunchResult.Failed("an unexpected error occurred")
        }
    }

    /** Opens the Android system settings screen, if this device provides one. */
    fun openSystemSettings(): LaunchResult {
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            LaunchResult.Started
        } catch (e: Exception) {
            LaunchResult.Failed("system settings could not be opened")
        }
    }

    // ---- Launcher mode (optional, off by default) ----
    private val homeAlias = ComponentName(context.packageName, "com.nexus.personalintelligence.tv.HomeAlias")

    /** True only if the user switched on the HOME entry point. Off = the original launcher is untouched. */
    fun launcherModeEnabled(): Boolean = try {
        pm.getComponentEnabledSetting(homeAlias) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } catch (e: Exception) {
        false
    }

    fun setLauncherMode(enabled: Boolean): Boolean = try {
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(homeAlias, state, PackageManager.DONT_KILL_APP)
        true
    } catch (e: Exception) {
        false
    }

    // ---- Websites ----
    fun openUrl(url: String): LaunchResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(pm) == null) return LaunchResult.Failed("no browser app is installed on this TV")
        return try {
            context.startActivity(intent)
            LaunchResult.Started
        } catch (e: ActivityNotFoundException) {
            LaunchResult.Failed("no browser app could open it")
        } catch (e: Exception) {
            LaunchResult.Failed("the system could not open it")
        }
    }

    /** Names of apps that can open https links. Empty = no browser installed. */
    fun browserNames(): List<String> = try {
        val query = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        pm.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY).map { it.loadLabel(pm).toString() }.distinct()
    } catch (e: Exception) {
        emptyList()
    }

    // ---- Custom background image ----
    /** Copies a user-chosen image into app storage, downscaled. Returns null if it could not be read. */
    suspend fun importImage(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 1400) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@withContext null
            val out = File(context.filesDir, "background_${System.currentTimeMillis()}.jpg")
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bmp.recycle()
            out
        } catch (e: Exception) {
            null
        }
    }
}
