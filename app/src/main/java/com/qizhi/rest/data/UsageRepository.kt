package com.qizhi.rest.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.qizhi.rest.privilege.PermissionTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
)

class UsageRepository(private val context: Context) {

    private val appContext = context.applicationContext
    private val usageStatsManager =
        appContext.getSystemService(UsageStatsManager::class.java)

    @Volatile
    private var cachedUsage: Map<String, Long> = emptyMap()

    @Volatile
    private var cacheAt: Long = 0L

    @Volatile
    private var launchableCache: List<InstalledAppInfo>? = null

    @Volatile
    private var systemCache: Map<String, Boolean> = emptyMap()

    fun hasAccess(): Boolean = PermissionTools.hasUsageAccess(appContext)

    suspend fun usageToday(force: Boolean = false): Map<String, Long> = withContext(Dispatchers.IO) {
        if (!force && System.currentTimeMillis() - cacheAt < CACHE_MS) {
            return@withContext cachedUsage
        }
        if (!hasAccess() || usageStatsManager == null) {
            cachedUsage = emptyMap()
            cacheAt = System.currentTimeMillis()
            return@withContext emptyMap()
        }
        val now = System.currentTimeMillis()
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val stats = runCatching {
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
        }.getOrDefault(emptyList())
        val map = stats
            .filter { it.lastTimeUsed > 0 }
            .groupBy { it.packageName }
            .mapValues { entry ->
                entry.value.maxOf { it.totalTimeInForeground }
            }
            .filterValues { it > 0 }
        cachedUsage = map
        cacheAt = System.currentTimeMillis()
        return@withContext map
    }

    fun usageMinutesFor(packageName: String): Long {
        val millis = cachedUsage[packageName] ?: 0L
        return millis / 60_000L
    }

    suspend fun foregroundPackage(): String? = withContext(Dispatchers.IO) {
        if (!hasAccess() || usageStatsManager == null) return@withContext null
        val now = System.currentTimeMillis()
        val events = runCatching {
            usageStatsManager.queryEvents(now - 120_000, now)
        }.getOrNull() ?: return@withContext null
        val event = UsageEvents.Event()
        var current: String? = null
        while (events.hasNextEvent()) {
            if (!events.getNextEvent(event)) break
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> current = pkg
                UsageEvents.Event.MOVE_TO_BACKGROUND -> if (current == pkg) current = null
            }
        }
        current
    }

    suspend fun loadLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        launchableCache?.let { return@withContext it }
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())
        val seen = mutableSetOf<String>()
        val apps = mutableListOf<InstalledAppInfo>()
        for (info in resolved) {
            val pkg = info.activityInfo.packageName
            if (pkg == appContext.packageName || !seen.add(pkg)) continue
            val appInfo = runCatching {
                pm.getApplicationInfo(pkg, 0)
            }.getOrNull() ?: continue
            val label = runCatching {
                pm.getApplicationLabel(appInfo).toString()
            }.getOrDefault(pkg)
            val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
            val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            apps += InstalledAppInfo(pkg, label, icon, isSystem)
            systemCache = systemCache + (pkg to isSystem)
        }
        val sorted = apps.sortedWith(
            compareBy<InstalledAppInfo> { it.isSystem }
                .thenBy { it.label.lowercase() },
        )
        launchableCache = sorted
        sorted
    }

    fun labelFor(packageName: String): String = runCatching {
        val pm = appContext.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    fun isSystemApp(packageName: String): Boolean {
        systemCache[packageName]?.let { return it }
        val value = runCatching {
            val pm = appContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            info.flags and ApplicationInfo.FLAG_SYSTEM != 0
        }.getOrDefault(false)
        systemCache = systemCache + (packageName to value)
        return value
    }

    fun isLaunchable(packageName: String): Boolean {
        launchableCache?.let { list -> return list.any { it.packageName == packageName } }
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
        return runCatching {
            pm.queryIntentActivities(intent, 0).isNotEmpty()
        }.getOrDefault(false)
    }

    fun launcherPackages(): Set<String> {
        val pm = appContext.packageManager
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val result = runCatching {
            pm.queryIntentActivities(home, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())
        return result.map { it.activityInfo.packageName }.toSet()
    }

    fun imePackages(): Set<String> {
        val imm = appContext.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
            ?: return emptySet()
        return runCatching {
            imm.enabledInputMethodList.map { it.packageName }.toSet() +
                imm.inputMethodList.map { it.packageName }.toSet()
        }.getOrDefault(emptySet())
    }

    fun refreshApps() {
        launchableCache = null
    }

    private companion object {
        const val CACHE_MS = 15_000L
    }
}
