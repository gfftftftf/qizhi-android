package com.qizhi.rest.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qizhi.rest.core.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("qizhi_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<DayRecord>>() {}.type

    private val _history = MutableStateFlow(read())
    val history: StateFlow<List<DayRecord>> = _history.asStateFlow()

    val current: List<DayRecord> get() = _history.value

    fun recordFor(date: String): DayRecord? = _history.value.firstOrNull { it.date == date }

    @Synchronized
    fun addFocus(minutes: Int, countSession: Boolean) {
        if (minutes <= 0) return
        mergeToday { it.copy(focusMinutes = it.focusMinutes + minutes, focusSessions = it.focusSessions + if (countSession) 1 else 0) }
    }

    @Synchronized
    fun addRest(minutes: Int, completed: Boolean) {
        if (minutes <= 0 && !completed) return
        mergeToday {
            it.copy(
                restMinutes = it.restMinutes + minutes.coerceAtLeast(0),
                restSessions = it.restSessions + if (completed) 1 else 0,
                skippedRests = it.skippedRests + if (completed) 0 else 1,
            )
        }
    }

    @Synchronized
    fun addSleep(minutes: Int) {
        if (minutes <= 0) return
        mergeToday { it.copy(sleepMinutes = it.sleepMinutes + minutes) }
    }

    @Synchronized
    fun addSkip() = mergeToday { it.copy(skippedRests = it.skippedRests + 1) }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
        _history.value = emptyList()
    }

    private fun mergeToday(transform: (DayRecord) -> DayRecord) {
        val today = TimeUtils.todayKey()
        val map = _history.value.associateBy { it.date }.toMutableMap()
        map[today] = transform(map[today] ?: DayRecord(date = today))
        val next = map.values.sortedBy { it.date }.takeLast(MAX_DAYS)
        prefs.edit().putString(KEY_HISTORY, gson.toJson(next)).apply()
        _history.value = next
    }

    private fun read(): List<DayRecord> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<DayRecord>>(raw, type) ?: emptyList() }
            .getOrDefault(emptyList())
            .sortedBy { it.date }
    }

    private companion object {
        const val KEY_HISTORY = "history_json"
        const val MAX_DAYS = 400
    }
}
