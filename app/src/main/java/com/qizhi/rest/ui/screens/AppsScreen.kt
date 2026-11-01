package com.qizhi.rest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qizhi.rest.data.AppRule
import com.qizhi.rest.data.InstalledAppInfo
import com.qizhi.rest.privilege.PermissionTools
import com.qizhi.rest.ui.AppUi
import com.qizhi.rest.ui.MainViewModel
import com.qizhi.rest.ui.components.AppIcon
import com.qizhi.rest.ui.components.Eyebrow
import com.qizhi.rest.ui.components.Hairline
import com.qizhi.rest.ui.components.Pill
import com.qizhi.rest.ui.components.PrimaryButton
import com.qizhi.rest.ui.components.SettingToggle
import com.qizhi.rest.ui.components.SoftCard
import com.qizhi.rest.ui.theme.LocalQizhiPalette
import com.qizhi.rest.core.TimeUtils
import kotlin.math.roundToInt

private enum class AppFilter { ALL, LIMITED, USED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val palette = LocalQizhiPalette.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val rawApps by viewModel.apps.collectAsStateWithLifecycle()
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val loading by viewModel.loadingApps.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val privilege by viewModel.privilege.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(AppFilter.ALL) }
    var selected by remember { mutableStateOf<AppUi?>(null) }

    val userIds = remember(rawApps) { rawApps.filter { !it.isSystem } }
    val apps = remember(userIds, rules, usage, query, filter) {
        val list = userIds.map { info ->
            AppUi(
                info = info,
                icon = viewModel.iconFor(info.packageName),
                rule = rules[info.packageName],
                usageMinutes = (usage[info.packageName] ?: 0L) / 60_000L,
            )
        }.filter { app ->
            (query.isBlank() ||
                app.info.label.contains(query, ignoreCase = true) ||
                app.info.packageName.contains(query, ignoreCase = true)) &&
                when (filter) {
                    AppFilter.ALL -> true
                    AppFilter.LIMITED -> app.rule?.isRestricted == true
                    AppFilter.USED -> app.usageMinutes > 0
                }
        }.sortedWith(
            compareByDescending<AppUi> { it.usageMinutes }
                .thenBy { it.info.label },
        )
        list
    }
    val restrictedCount = remember(rules) { rules.values.count { it.isRestricted } }

    LazyColumn(
        modifier = Modifier.statusBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 124.dp),
    ) {
        item {
            Eyebrow("Guard")
            Spacer(Modifier.height(6.dp))
            Text(
                text = "管控",
                style = MaterialTheme.typography.displayMedium,
                color = palette.ink,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "把容易滑走的时间收起来。规则只作用于你自己选择的应用。",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(20.dp))
        }

        item {
            SoftCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.accentSoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = palette.accentDeep,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "应用管控",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.ink,
                                )
                                Text(
                                    text = "已限制 $restrictedCount 个应用",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.muted,
                                )
                            }
                        }
                        Switch(
                            checked = settings.enforcementEnabled,
                            onCheckedChange = { checked ->
                                viewModel.updateSettings { it.copy(enforcementEnabled = checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.background,
                                checkedTrackColor = palette.accent,
                                uncheckedThumbColor = palette.muted,
                                uncheckedTrackColor = palette.surfaceHigh,
                            ),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Hairline()
                    SettingToggle(
                        title = "全天执行每日限额",
                        subtitle = "借助无障碍服务，在专注、休息与睡眠之外也执行限额。",
                        checked = settings.alwaysGuardLimits,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(alwaysGuardLimits = checked) }
                        },
                    )
                    SettingToggle(
                        title = "达到限额后强制停止",
                        subtitle = "需要 Shizuku 或 Root。关闭时只返回桌面，不结束应用进程。",
                        checked = settings.forceStopBlocked,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings { it.copy(forceStopBlocked = checked) }
                        },
                    )
                    if (!permissions.accessibility) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.surfaceHigh.copy(alpha = 0.55f))
                                .clickable { PermissionTools.openAccessibility(context) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessibilityNew,
                                contentDescription = null,
                                tint = palette.glow,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = "未开启无障碍：应用切换时的即时阻断会慢一些。去开启",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (!permissions.usageAccess) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.surfaceHigh.copy(alpha = 0.55f))
                                .clickable { PermissionTools.openUsageAccess(context) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Insights,
                                contentDescription = null,
                                tint = palette.glow,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = "未开启使用情况访问：暂时无法统计今日用量。去开启",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.muted,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        item {
            SearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    text = "全部",
                    selected = filter == AppFilter.ALL,
                    onClick = { filter = AppFilter.ALL },
                )
                Pill(
                    text = "已限制",
                    selected = filter == AppFilter.LIMITED,
                    onClick = { filter = AppFilter.LIMITED },
                )
                Pill(
                    text = "今日使用",
                    selected = filter == AppFilter.USED,
                    onClick = { filter = AppFilter.USED },
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = palette.accent, strokeWidth = 2.dp)
                }
            }
        } else if (apps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (query.isNotBlank()) "没有找到匹配的应用" else "暂无可管控的应用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.muted,
                    )
                }
            }
        } else {
            items(apps, key = { it.info.packageName }) { app ->
                AppRow(
                    app = app,
                    onOpen = { selected = app },
                    onToggle = { enabled ->
                        viewModel.setRule(
                            if (enabled) {
                                (app.rule ?: AppRule(app.info.packageName)).copy(
                                    blockRest = true,
                                    blockSleep = true,
                                )
                            } else {
                                AppRule(app.info.packageName)
                            },
                        )
                    },
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            Text(
                text = "系统应用不参与管控。每日限额在守护时段内即时生效；开启无障碍后，全天限额也会被即时执行。",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted.copy(alpha = 0.82f),
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (privilege.shizukuReady) "Shizuku 已连接：强制停止可用。" else "Shizuku 未连接：仅执行返回桌面，不强制停止应用。",
                style = MaterialTheme.typography.labelSmall,
                color = palette.muted.copy(alpha = 0.82f),
            )
        }
    }

    selected?.let { app ->
        val current = app.info
        RuleSheet(
            app = current,
            icon = viewModel.iconFor(current.packageName),
            usageMinutes = app.usageMinutes,
            rule = rules[current.packageName],
            onDismiss = { selected = null },
            onSave = { rule ->
                viewModel.setRule(rule)
                selected = null
            },
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalQizhiPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = palette.muted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(9.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.ink),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) {
                        Text(
                            text = "搜索应用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.muted.copy(alpha = 0.75f),
                        )
                    }
                    inner()
                }
            },
        )
        if (value.isNotBlank()) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "清空搜索",
                tint = palette.muted,
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onValueChange("") },
            )
        }
    }
}

@Composable
private fun AppRow(
    app: AppUi,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val palette = LocalQizhiPalette.current
    val rule = app.rule
    val restricted = rule?.isRestricted == true
    val limit = rule?.dailyLimitMinutes ?: 0
    val progress = if (limit > 0) {
        (app.usageMinutes.toFloat() / limit).coerceIn(0f, 1f)
    } else {
        0f
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 9.dp, horizontal = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(icon = app.icon, name = app.info.label)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.info.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(if (app.usageMinutes > 0) "今日 ${TimeUtils.formatUsage(app.usageMinutes)}" else "今日未使用")
                        val labels = buildList {
                            if (rule?.blockFocus == true) add("专注")
                            if (rule?.blockRest == true) add("休息")
                            if (rule?.blockSleep == true) add("睡眠")
                            if (limit > 0) add("限额 ${limit} 分")
                        }
                        if (labels.isNotEmpty()) append(" · ").append(labels.joinToString(" / "))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (restricted) palette.accentDeep else palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (limit > 0) {
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (progress >= 1f) palette.error else palette.accent,
                        trackColor = palette.surfaceHigh,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = restricted,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.background,
                    checkedTrackColor = palette.accent,
                    uncheckedThumbColor = palette.muted,
                    uncheckedTrackColor = palette.surfaceHigh,
                    uncheckedBorderColor = palette.line,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleSheet(
    app: InstalledAppInfo,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    usageMinutes: Long,
    rule: AppRule?,
    onDismiss: () -> Unit,
    onSave: (AppRule) -> Unit,
) {
    val palette = LocalQizhiPalette.current
    var blockFocus by remember(rule) { mutableStateOf(rule?.blockFocus == true) }
    var blockRest by remember(rule) { mutableStateOf(rule?.blockRest ?: true) }
    var blockSleep by remember(rule) { mutableStateOf(rule?.blockSleep ?: true) }
    var limit by remember(rule) { mutableFloatStateOf((rule?.dailyLimitMinutes ?: 0).toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        contentColor = palette.ink,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(icon = icon, name = app.label, size = 52.dp)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                    )
                    Text(
                        text = if (usageMinutes > 0) "今日已使用 ${TimeUtils.formatUsage(usageMinutes)}" else "今日未使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Hairline()
            Spacer(Modifier.height(8.dp))
            SettingToggle(
                title = "专注时禁用",
                subtitle = "专注会话进行中，打开即被收起。",
                checked = blockFocus,
                onCheckedChange = { blockFocus = it },
            )
            SettingToggle(
                title = "休息时禁用",
                subtitle = "强制休息期间，把它从屏幕上移开。",
                checked = blockRest,
                onCheckedChange = { blockRest = it },
            )
            SettingToggle(
                title = "睡眠时禁用",
                subtitle = "睡眠守护期间不打开这个应用。",
                checked = blockSleep,
                onCheckedChange = { blockSleep = it },
            )
            Spacer(Modifier.height(8.dp))
            Hairline()
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "每日限额",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.ink,
                    )
                    Text(
                        text = "0 表示不设限额",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                    )
                }
                Text(
                    text = if (limit <= 0f) "不限额" else "${limit.roundToInt()} 分钟",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accentDeep,
                )
            }
            Slider(
                value = limit,
                onValueChange = { limit = (it / 15).roundToInt() * 15f },
                valueRange = 0f..240f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accent,
                    activeTrackColor = palette.accent,
                    inactiveTrackColor = palette.surfaceHigh,
                ),
            )
            Text(
                text = "限额在专注、休息与睡眠期间即时生效；开启无障碍后，全天也会执行。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = "保存设置",
                onClick = {
                    onSave(
                        AppRule(
                            packageName = app.packageName,
                            blockFocus = blockFocus,
                            blockRest = blockRest,
                            blockSleep = blockSleep,
                            dailyLimitMinutes = limit.roundToInt(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("取消", color = palette.muted)
            }
        }
    }
}
