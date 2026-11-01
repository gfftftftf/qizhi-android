package com.qizhi.rest.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("qizhi_session", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _current = MutableStateFlow(read())
    val current: StateFlow<GuardSession?> = _current.asStateFlow()

    val session: GuardSession? get() = _current.value

    @Synchronized
    fun set(session: GuardSession) {
        prefs.edit().putString(KEY_SESSION, gson.toJson(session)).apply()
        _current.value = session
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_SESSION).apply()
        _current.value = null
    }

    private fun read(): GuardSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { gson.fromJson(raw, GuardSession::class.java) }.getOrNull()
            ?.takeIf { it.endsAt > it.startedAt && it.phase != Phase.IDLE }
    }

    private companion object {
        const val KEY_SESSION = "session_json"
    }
}

class EnvRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("qizhi_env", Context.MODE_PRIVATE)

    fun snapshot(): EnvSnapshot = EnvSnapshot(
        dndSaved = prefs.getBoolean(KEY_DND_SAVED, false),
        dndPrevious = prefs.getString(KEY_DND_PREV, "0") ?: "0",
        graySaved = prefs.getBoolean(KEY_GRAY_SAVED, false),
        grayEnabledPrevious = prefs.getString(KEY_GRAY_ENABLED_PREV, "0") ?: "0",
        grayModePrevious = prefs.getString(KEY_GRAY_MODE_PREV, "0") ?: "0",
    )

    fun saveDnd(previous: String) {
        prefs.edit().putBoolean(KEY_DND_SAVED, true).putString(KEY_DND_PREV, previous).apply()
    }

    fun saveGray(enabled: String, mode: String) {
        prefs.edit()
            .putBoolean(KEY_GRAY_SAVED, true)
            .putString(KEY_GRAY_ENABLED_PREV, enabled)
            .putString(KEY_GRAY_MODE_PREV, mode)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_DND_SAVED = "dnd_saved"
        const val KEY_DND_PREV = "dnd_prev"
        const val KEY_GRAY_SAVED = "gray_saved"
        const val KEY_GRAY_ENABLED_PREV = "gray_enabled_prev"
        const val KEY_GRAY_MODE_PREV = "gray_mode_prev"
    }
}
