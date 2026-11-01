package com.qizhi.rest.privilege

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import com.qizhi.rest.guard.QizhiAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ShellResult(
    val code: Int,
    val stdout: String = "",
    val stderr: String = "",
) {
    val ok: Boolean get() = code == 0
    val display: String
        get() = listOf(stdout, stderr).filter { it.isNotBlank() }.joinToString("\n").trim()
}

data class PrivilegeStatus(
    val installed: Boolean = false,
    val binderAlive: Boolean = false,
    val permissionGranted: Boolean = false,
    val uid: Int = -1,
    val rootVerified: Boolean = false,
    val rootAvailable: Boolean = false,
    val checked: Boolean = false,
) {
    val shizukuReady: Boolean get() = installed && binderAlive && permissionGranted
    val canExecute: Boolean get() = shizukuReady || rootAvailable
}

data class GrantResult(
    val title: String,
    val success: Boolean,
    val detail: String? = null,
)

class PrivilegedEngine(private val context: Context) {

    private val appContext = context.applicationContext
    private val _status = MutableStateFlow(PrivilegeStatus())
    val status: StateFlow<PrivilegeStatus> = _status.asStateFlow()

    private var rootVerified = false

    init {
        registerShizukuListeners()
        refresh()
    }

    private fun registerShizukuListeners() {
        runCatching {
            Shizuku.addBinderReceivedListenerSticky { refresh() }
        }
        runCatching {
            Shizuku.addBinderDeadListener { refresh() }
        }
        runCatching {
            Shizuku.addRequestPermissionResultListener(
                Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() },
            )
        }
    }

    fun refresh() {
        val installed = PermissionTools.hasShizukuInstalled(appContext)
        var binderAlive = false
        var granted = false
        var uid = -1
        if (installed) {
            binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
            if (binderAlive) {
                granted = runCatching {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }.getOrDefault(false)
                uid = runCatching { Shizuku.getUid() }.getOrDefault(-1)
            }
        }
        val currentRoot = _status.value.rootVerified
        _status.value = PrivilegeStatus(
            installed = installed,
            binderAlive = binderAlive,
            permissionGranted = granted,
            uid = uid,
            rootVerified = currentRoot,
            rootAvailable = rootVerified,
            checked = true,
        )
    }

    fun requestPermission(): Boolean {
        val current = _status.value
        if (!current.installed || !current.binderAlive) return false
        return runCatching {
            Shizuku.requestPermission(REQUEST_CODE)
            true
        }.getOrDefault(false)
    }

    suspend fun verifyRoot(): Boolean = withContext(Dispatchers.IO) {
        val result = runCatching { execRoot("id") }.getOrNull()
        val ok = result != null && result.ok && result.stdout.contains("uid=0")
        rootVerified = ok
        val base = _status.value
        _status.value = base.copy(rootVerified = ok, rootAvailable = ok)
        ok
    }

    suspend fun exec(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ShellResult =
        withContext(Dispatchers.IO) {
            val status = _status.value
            if (status.shizukuReady) {
                val result = runCatching { execShizuku(command, timeoutMs) }
                    .getOrElse {
                        ShellResult(-1, stderr = it.message ?: it.javaClass.simpleName)
                    }
                if (result.code != CODE_BINDER_DEAD) return@withContext result
                refresh()
            }
            if (_status.value.rootAvailable) {
                return@withContext runCatching { execRoot(command, timeoutMs) }
                    .getOrElse { ShellResult(-1, stderr = it.message.orEmpty()) }
            }
            ShellResult(
                code = CODE_NO_PRIVILEGE,
                stderr = "尚未连接 Shizuku 或 Root。请先在「设置 → 高级权限」中完成授权。",
            )
        }

    fun canExecute(): Boolean = _status.value.canExecute

    private fun execShizuku(command: String, timeoutMs: Long): ShellResult {
        if (!Shizuku.pingBinder()) return ShellResult(CODE_BINDER_DEAD, stderr = "Shizuku 服务已断开")
        val service = IShizukuService.Stub.asInterface(Shizuku.getBinder())
            ?: return ShellResult(CODE_BINDER_DEAD, stderr = "无法获取 Shizuku 服务")
        val remote = runCatching {
            service.newProcess(arrayOf("sh", "-c", command), null, null)
        }.getOrElse {
            if (it is SecurityException) {
                return ShellResult(CODE_NO_PRIVILEGE, stderr = "Shizuku 权限未授予")
            }
            return ShellResult(-1, stderr = it.message ?: "启动远程进程失败")
        }

        val stdoutStream = ParcelFileDescriptor.AutoCloseInputStream(remote.inputStream)
        val stderrStream = ParcelFileDescriptor.AutoCloseInputStream(remote.errorStream)
        val pool = Executors.newFixedThreadPool(2)
        val stdoutFuture = pool.submit<String> { readLimited(stdoutStream) }
        val stderrFuture = pool.submit<String> { readLimited(stderrStream) }

        val started = SystemClock.elapsedRealtime()
        var timedOut = false
        try {
            while (true) {
                val alive = runCatching { remote.alive() }.getOrDefault(false)
                if (!alive) break
                if (SystemClock.elapsedRealtime() - started > timeoutMs) {
                    timedOut = true
                    runCatching { remote.destroy() }
                    break
                }
                Thread.sleep(30)
            }
            val code = runCatching { remote.exitValue() }.getOrDefault(-1)
            val stdout = runCatching { stdoutFuture.get(1, TimeUnit.SECONDS) }.getOrDefault("")
            val stderr = runCatching { stderrFuture.get(1, TimeUnit.SECONDS) }.getOrDefault("")
            return ShellResult(
                code = if (timedOut) -1 else code,
                stdout = stdout,
                stderr = if (timedOut) "命令超时" else stderr,
            )
        } finally {
            runCatching { stdoutStream.close() }
            runCatching { stderrStream.close() }
            pool.shutdownNow()
        }
    }

    private fun execRoot(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ShellResult {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(false)
            .start()
        val pool = Executors.newFixedThreadPool(2)
        val outFuture = pool.submit<String> { readLimited(process.inputStream) }
        val errFuture = pool.submit<String> { readLimited(process.errorStream) }
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        val code = runCatching { process.exitValue() }.getOrDefault(-1)
        val stdout = runCatching { outFuture.get(1, TimeUnit.SECONDS) }.getOrDefault("")
        val stderr = runCatching { errFuture.get(1, TimeUnit.SECONDS) }.getOrDefault("")
        pool.shutdownNow()
        return ShellResult(
            code = if (finished) code else -1,
            stdout = stdout,
            stderr = if (finished) stderr else "Root 命令超时",
        )
    }

    private fun readLimited(input: InputStream): String {
        val buffer = ByteArray(4096)
        val output = ByteArrayOutputStream()
        var written = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (written < MAX_OUTPUT) {
                val take = minOf(read, MAX_OUTPUT - written)
                output.write(buffer, 0, take)
                written += take
            }
        }
        return output.toString(Charsets.UTF_8.name()).trim()
    }

    suspend fun grantAll(): List<GrantResult> {
        refresh()
        if (!_status.value.canExecute) {
            return listOf(
                GrantResult(
                    "连接 Shizuku",
                    false,
                    "Shizuku 服务未运行或未授权。请先打开 Shizuku 并允许栖止访问。",
                ),
            )
        }
        val pkg = appContext.packageName
        val results = mutableListOf<GrantResult>()

        results += runCatching {
            val r = exec("pm grant $pkg android.permission.POST_NOTIFICATIONS")
            GrantResult("通知权限", r.ok, r.display.takeIf { !r.ok })
        }.getOrElse { GrantResult("通知权限", false, it.message) }

        results += grantStep("使用情况访问", "appops set $pkg GET_USAGE_STATS allow") {
            PermissionTools.hasUsageAccess(appContext)
        }

        results += grantStep("悬浮窗", "appops set $pkg SYSTEM_ALERT_WINDOW allow") {
            PermissionTools.hasOverlay(appContext)
        }

        results += grantStep("精确闹钟", "appops set $pkg SCHEDULE_EXACT_ALARM allow") {
            PermissionTools.canScheduleExactAlarms(appContext)
        }

        results += grantStep("电池优化白名单", "cmd deviceidle whitelist +$pkg") {
            PermissionTools.isBatteryUnrestricted(appContext)
        }

        results += runCatching {
            val component = ComponentName(
                appContext,
                QizhiAccessibilityService::class.java,
            ).flattenToShortString()
            val current = exec("settings get secure enabled_accessibility_services")
                .stdout.trim()
                .let { if (it == "null") "" else it }
            val existing = current.split(':').filter { it.isNotBlank() }
            var changed = false
            if (existing.none { ComponentName.unflattenFromString(it) == ComponentName(appContext, QizhiAccessibilityService::class.java) }) {
                val merged = (existing + component).joinToString(":")
                val put = exec(
                    "settings put secure enabled_accessibility_services ${shellQuote(merged)}",
                )
                changed = put.ok
            }
            val enabled = exec("settings put secure accessibility_enabled 1")
            val ok = (changed || existing.isNotEmpty()) && enabled.ok
            GrantResult(
                "无障碍守护",
                ok,
                if (ok) "可在系统设置中关闭" else enabled.display.ifBlank { null },
            )
        }.getOrElse { GrantResult("无障碍守护", false, it.message) }

        results += runCatching {
            val r = exec("cmd deviceidle whitelist +$pkg")
            GrantResult("后台稳定运行", r.ok, r.display.takeIf { !r.ok })
        }.getOrElse { GrantResult("后台稳定运行", false, it.message) }

        refresh()
        return results
    }

    private suspend fun grantStep(
        title: String,
        command: String,
        verify: () -> Boolean,
    ): GrantResult {
        val result = runCatching { exec(command) }
            .getOrElse { return GrantResult(title, false, it.message) }
        // appops 在不同 ROM 上可能要求 android: 前缀，失败时再试一次。
        if (!result.ok) {
            val alternate = command
                .replace("GET_USAGE_STATS", "android:get_usage_stats")
                .replace("SCHEDULE_EXACT_ALARM", "android:schedule_exact_alarm")
            if (alternate != command) exec(alternate)
        }
        val verified = runCatching { verify() }.getOrDefault(false)
        return GrantResult(
            title = title,
            success = verified || result.ok,
            detail = result.display.takeIf { !result.ok && !verified },
        )
    }

    suspend fun forceStop(packageName: String) {
        if (!isSafePackageName(packageName)) return
        exec("am force-stop --user current $packageName", 8_000)
    }

    suspend fun goHome() {
        exec("input keyevent 3", 5_000)
    }

    suspend fun sleepScreen() {
        val result = exec("input keyevent 223", 5_000)
        if (!result.ok) exec("input keyevent 26", 5_000)
    }

    suspend fun getGlobal(key: String): String =
        exec("settings get global ${shellQuote(key)}", 5_000).stdout.trim()

    suspend fun putGlobal(key: String, value: String): ShellResult =
        exec("settings put global ${shellQuote(key)} ${shellQuote(value)}", 5_000)

    suspend fun getSecure(key: String): String =
        exec("settings get secure ${shellQuote(key)}", 5_000).stdout.trim()

    suspend fun putSecure(key: String, value: String): ShellResult =
        exec("settings put secure ${shellQuote(key)} ${shellQuote(value)}", 5_000)

    suspend fun foregroundPackage(): String? {
        val output = exec("dumpsys activity activities", 8_000).stdout
        val patterns = listOf(
            Regex("topResumedActivity=ActivityRecord\\{[^}]*?\\s+u\\d+\\s+([A-Za-z0-9._]+)/"),
            Regex("mResumedActivity: ActivityRecord\\{[^}]*?\\s+u\\d+\\s+([A-Za-z0-9._]+)/"),
            Regex("ResumedActivity: ActivityRecord\\{[^}]*?\\s+u\\d+\\s+([A-Za-z0-9._]+)/"),
        )
        for (pattern in patterns) {
            val match = pattern.find(output)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun isSafePackageName(packageName: String): Boolean =
        packageName.matches(Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+$"))

    fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    companion object {
        const val REQUEST_CODE = 4201
        private const val DEFAULT_TIMEOUT_MS = 15_000L
        private const val MAX_OUTPUT = 128 * 1024
        private const val CODE_BINDER_DEAD = -3
        private const val CODE_NO_PRIVILEGE = -2
    }
}
