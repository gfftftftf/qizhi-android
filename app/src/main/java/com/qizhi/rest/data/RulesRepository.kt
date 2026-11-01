package com.qizhi.rest.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RulesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("qizhi_rules", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<Map<String, AppRule>>() {}.type

    private val _rules = MutableStateFlow(read())
    val rules: StateFlow<Map<String, AppRule>> = _rules.asStateFlow()

    val current: Map<String, AppRule> get() = _rules.value

    fun ruleFor(packageName: String): AppRule? = _rules.value[packageName]

    @Synchronized
    fun setRule(rule: AppRule) {
        val next = _rules.value.toMutableMap()
        if (rule.isRestricted) next[rule.packageName] = rule else next.remove(rule.packageName)
        prefs.edit().putString(KEY_RULES, gson.toJson(next)).apply()
        _rules.value = next
    }

    @Synchronized
    fun toggleField(packageName: String, field: RuleField, enabled: Boolean) {
        val base = _rules.value[packageName] ?: AppRule(packageName)
        val next = when (field) {
            RuleField.FOCUS -> base.copy(blockFocus = enabled)
            RuleField.REST -> base.copy(blockRest = enabled)
            RuleField.SLEEP -> base.copy(blockSleep = enabled)
        }
        setRule(next)
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_RULES).apply()
        _rules.value = emptyMap()
    }

    private fun read(): Map<String, AppRule> {
        val raw = prefs.getString(KEY_RULES, null) ?: return emptyMap()
        return runCatching { gson.fromJson<Map<String, AppRule>>(raw, type) ?: emptyMap() }
            .getOrDefault(emptyMap())
            .filterValues { it.isRestricted }
    }

    enum class RuleField { FOCUS, REST, SLEEP }

    private companion object {
        const val KEY_RULES = "rules_json"
    }
}
