package com.fitlog.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.toast
import com.fitlog.app.util.monthRange
import com.fitlog.app.util.streakOf
import com.fitlog.app.util.todayStr
import java.time.LocalDate

@Composable
fun CalendarScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val ctx = LocalContext.current
    val today = LocalDate.now()
    var year by rememberSaveable { mutableStateOf(today.year) }
    var month by rememberSaveable { mutableStateOf(today.monthValue) }

    val (from, to) = monthRange(year, month)
    val sessions by app.repo.sessionsBetween(from, to).collectAsStateWithLifecycle(initialValue = emptyList())
    // 连续打卡改为响应式：保存新训练后立即刷新，无需重进页面
    val allSessions by app.repo.allSessions().collectAsStateWithLifecycle(initialValue = emptyList())

    val byDate = sessions.groupBy { it.date }
    val monthSec = sessions.sumOf { it.durSec.toLong() }
    val streak = streakOf(allSessions.map { it.date })
    // 热力强度：按当日训练时长归一化
    val dayLoad = byDate.mapValues { (_, list) -> list.sumOf { it.durSec } }
    val maxLoad = dayLoad.values.maxOrNull() ?: 0

    val firstDate = LocalDate.of(year, month, 1)
    val lead = firstDate.dayOfWeek.value - 1
    val daysInMonth = firstDate.lengthOfMonth()
    val cells: List<Int?> = List(lead) { null } + (1..daysInMonth).toList()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text("日历", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonthArrow("<") {
                        month--; if (month < 1) { month = 12; year-- }
                    }
                    Text("${year}年${month}月", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    MonthArrow(">") {
                        month++; if (month > 12) { month = 1; year++ }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { wd ->
                        Text(
                            wd, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { d ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (d != null) {
                                    val dateStr = "%04d-%02d-%02d".format(year, month, d)
                                    val has = byDate.containsKey(dateStr)
                                    val isToday = dateStr == todayStr()
                                    // 训练量 → Volt 透明度分级（有练就至少可见）
                                    val ratio = if (has && maxLoad > 0) (dayLoad[dateStr] ?: 0).toFloat() / maxLoad else 0f
                                    val heat = if (has) 0.38f + 0.62f * ratio else 0f
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    has -> MaterialTheme.colorScheme.tertiary.copy(alpha = heat)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                }
                                            )
                                            .then(
                                                if (isToday) Modifier.border(
                                                    2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                                                ) else Modifier
                                            )
                                            .clickable {
                                                if (has) nav.navigate("day/$dateStr")
                                                else toast(ctx, "这一天没有训练记录")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$d",
                                            fontSize = 13.5.sp,
                                            fontWeight = if (has || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                has -> MaterialTheme.colorScheme.onTertiary
                                                isToday -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 14.dp)) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${sessions.size}", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(" 次", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    Text("本月训练", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("%.1f".format(monthSec / 3600.0), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(" h", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    Text("累计时长", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$streak", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(" 天", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    Text("连续打卡", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Text(
            "点高亮日期查看当天训练内容 · 颜色越亮训练量越大",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun MonthArrow(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(11.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (symbol == "<") Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = symbol,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp)
        )
    }
}
