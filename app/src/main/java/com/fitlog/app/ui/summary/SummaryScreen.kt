package com.fitlog.app.ui.summary

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.db.SessionWithDetails
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.PartBadge
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.toast
import com.fitlog.app.ui.training.SummaryItem
import com.fitlog.app.ui.training.SummaryRow
import com.fitlog.app.ui.training.SummaryState
import com.fitlog.app.ui.training.TrainingViewModel
import com.fitlog.app.ui.theme.Green
import com.fitlog.app.util.fmtDur
import com.fitlog.app.util.fmtVolume

@Composable
fun SummaryScreen(nav: NavController, sessionId: Long) {
    val activity = LocalContext.current as ComponentActivity
    val vm: TrainingViewModel = viewModel(viewModelStoreOwner = activity)
    val app = LocalContext.current.applicationContext as FitLogApp
    val ctx = LocalContext.current
    val fromHistory = sessionId > 0

    var hist by remember { mutableStateOf<SummaryState?>(null) }
    var missed by remember { mutableStateOf(false) }
    LaunchedEffect(sessionId) {
        if (fromHistory) {
            hist = app.repo.sessionDetail(sessionId)?.let { toSummaryState(it) }
            if (hist == null) missed = true
        } else {
            // 进程被杀等极端情况下无结算数据：自动退回，避免空白页
            kotlinx.coroutines.delay(400)
            if (vm.summary == null) nav.popBackStack()
        }
    }
    val s = if (fromHistory) hist else vm.summary
    if (s == null) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (missed) Text("记录不存在", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        LaunchedEffect(missed) { if (missed) nav.popBackStack() }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        if (fromHistory) {
            BackTopBar("训练详情", onBack = { nav.popBackStack() })
        } else {
            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Green.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = Green, modifier = Modifier.size(34.dp)) }
                Text("训练完成！", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp))
                Text(
                    "${s.planName} · ${s.time} 结束",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(fmtDur(s.durSec.toLong()), "训练时长", Modifier.weight(1f))
            StatTile("${s.setsDone}/${s.setsTotal} 组", "完成组数", Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${fmtVolume(s.volume)} kg", "总容量", Modifier.weight(1f))
            StatTile("${s.parts.size} 个部位", s.parts.joinToString(" / "), Modifier.weight(1f))
        }

        com.fitlog.app.ui.components.SectionHeader("动作明细")

        s.items.forEachIndexed { idx, item ->
            SummaryItemCard(item)
            if (idx < s.items.size - 1) Spacer(Modifier.height(12.dp))
        }

        if (!fromHistory) {
            Spacer(Modifier.height(24.dp))
            if (s.saved) {
                Text(
                    "已保存，今天打卡成功 ✓",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Green,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { toast(ctx, "演示：此处调用系统分享") },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Icon(Icons.Filled.Share, null, Modifier.size(16.dp))
                        Text(" 分享")
                    }
                    Button(
                        onClick = {
                            vm.saveSummary {
                                toast(ctx, "已保存 · 今天打卡成功 ✓")
                                nav.navigate("home") { popUpTo(0) { inclusive = true } }
                            }
                        },
                        modifier = Modifier.weight(2f).height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("保存记录", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }
                TextButton(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("不保存，直接返回", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SummaryItemCard(item: SummaryItem) {
    val done = item.rows.count { it.done }
    var vol = 0f
    item.rows.forEach { r -> if (r.done && r.weight != null && r.reps != null) vol += r.weight * r.reps }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PartBadge(item.part, size = 40.dp, radius = 12.dp)
                Text(
                    item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1
                )
                if (vol > 0f) {
                    Text(
                        "容量 ${fmtVolume(vol)}kg", fontSize = 13.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.size(8.dp))
                Text(
                    "$done/${item.rows.size} 组",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (done == item.rows.size) Green else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            (if (done == item.rows.size) Green else MaterialTheme.colorScheme.primary).copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            item.rows.forEachIndexed { j, r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "第 ${j + 1} 组 · ${valueText(item.mode, r)}",
                        fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (r.done) "✓ 完成" else "未完成",
                        fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                        color = if (r.done) Green else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun valueText(mode: String, r: SummaryRow): String = when (mode) {
    "wr" -> "${fmtVol(r.weight ?: 0f)}kg × ${r.reps ?: 0}次"
    "r" -> "${r.reps ?: 0}次"
    else -> "${r.timeSec ?: 0}秒"
}

private fun fmtVol(v: Float): String =
    if (v % 1f == 0f) "%.0f".format(v) else "%.1f".format(v)

fun toSummaryState(d: SessionWithDetails): SummaryState = SummaryState(
    planId = null,
    planName = d.session.planName,
    date = d.session.date,
    time = com.fitlog.app.util.fmtClock(d.session.startTs),
    durSec = d.session.durSec,
    setsTotal = d.session.setsTotal,
    setsDone = d.session.setsDone,
    volume = d.session.volume,
    parts = d.session.parts.split(",").filter { it.isNotBlank() },
    items = d.exercises.sortedBy { it.exercise.orderIdx }.map { exw ->
        SummaryItem(
            exw.exercise.name, exw.exercise.part, exw.exercise.mode,
            exw.sets.sortedBy { it.idx }.map { r -> SummaryRow(r.done, r.weight, r.reps, r.timeSec) }
        )
    }
)
