package com.qizhi.rest

import android.app.Application
import com.qizhi.rest.data.EnvRepository
import com.qizhi.rest.data.HistoryRepository
import com.qizhi.rest.data.RulesRepository
import com.qizhi.rest.data.SessionRepository
import com.qizhi.rest.data.SettingsRepository
import com.qizhi.rest.data.UsageRepository
import com.qizhi.rest.guard.GuardNotifications
import com.qizhi.rest.guard.Scheduler
import com.qizhi.rest.guard.SystemTweaks
import com.qizhi.rest.privilege.PrivilegedEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Application) {
    val settings = SettingsRepository(context)
    val rules = RulesRepository(context)
    val history = HistoryRepository(context)
    val session = SessionRepository(context)
    val env = EnvRepository(context)
    val usage = UsageRepository(context)
    val privileged = PrivilegedEngine(context)
    val tweaks = SystemTweaks(privileged, env)
    val scheduler = Scheduler(context)
}

class QizhiApp : Application() {

    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        GuardNotifications.createChannels(this)
        container.scheduler.scheduleAll(container.settings.current)
        if (container.session.session == null) {
            appScope.launch { runCatching { container.tweaks.restore() } }
        }
    }
}
