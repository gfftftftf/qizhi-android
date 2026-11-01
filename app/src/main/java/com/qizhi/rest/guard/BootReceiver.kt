package com.qizhi.rest.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qizhi.rest.QizhiApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? QizhiApp ?: return
        val pending = goAsync()
        val scheduler = Scheduler(context)
        scheduler.scheduleAll(app.container.settings.current)

        val session = app.container.session.session
        if (session == null) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { app.container.tweaks.restore() }
                pending.finish()
            }
            return
        }
        if (session.endsAt > System.currentTimeMillis()) {
            GuardController.resume(context)
        } else {
            app.container.session.clear()
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { app.container.tweaks.restore() }
                pending.finish()
            }
            return
        }
        pending.finish()
    }
}
