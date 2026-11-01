package com.qizhi.rest.privilege

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.qizhi.rest.guard.QizhiAccessibilityService

data class PermissionState(
    val notifications: Boolean = false,
    val usageAccess: Boolean = false,
    val overlay: Boolean = false,
    val batteryUnrestricted: Boolean = false,
    val exactAlarm: Boolean = false,
    val accessibility: Boolean = false,
    val dndAccess: Boolean = false,
) {
    val readyCount: Int
        get() = listOf(
            notifications,
            usageAccess,
            overlay,
            batteryUnrestricted,
            exactAlarm,
            accessibility,
        ).count { it }

    val totalCount: Int get() = 6

    val allReady: Boolean get() = readyCount == totalCount
}

object PermissionTools {

    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val SHIZUKU_URL = "https://shizuku.rikka.app/download/"

    fun snapshot(context: Context): PermissionState = PermissionState(
        notifications = hasNotifications(context),
        usageAccess = hasUsageAccess(context),
        overlay = hasOverlay(context),
        batteryUnrestricted = isBatteryUnrestricted(context),
        exactAlarm = canScheduleExactAlarms(context),
        accessibility = isAccessibilityEnabled(context),
        dndAccess = hasDndAccess(context),
    )

    fun hasNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = ComponentName(context, QizhiAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        if (enabled.split(':').any {
                val component = ComponentName.unflattenFromString(it)
                component?.packageName == expected.packageName &&
                    component.className == expected.className
            }
        ) return true

        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                it.resolveInfo.serviceInfo.packageName == context.packageName &&
                    it.resolveInfo.serviceInfo.name == expected.className
            }
    }

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(android.app.NotificationManager::class.java) ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    fun hasShizukuInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    }.getOrDefault(false)

    fun openUsageAccess(context: Context) {
        launch(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openOverlay(context: Context) {
        launch(
            context,
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }

    fun openBatterySettings(context: Context) {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
        if (!launch(context, direct)) {
            launch(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    fun openExactAlarm(context: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            val direct = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}"),
            )
            if (!launch(context, direct)) {
                launch(context, Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    fun openAccessibility(context: Context) {
        launch(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun openDndAccess(context: Context) {
        launch(context, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        if (!launch(context, intent)) {
            launch(
                context,
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    fun openShizuku(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launch(context, launch)) return
        }
        openUrl(context, SHIZUKU_URL)
    }

    fun openShizukuDownload(context: Context) = openUrl(context, SHIZUKU_URL)

    fun openUrl(context: Context, url: String) {
        launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun openSelfDetails(context: Context) {
        launch(
            context,
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    }

    private fun launch(context: Context, intent: Intent): Boolean = runCatching {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
