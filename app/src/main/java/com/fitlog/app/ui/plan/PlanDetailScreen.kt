package com.fitlog.app.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.db.PlanWithItems
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.PartBadge
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.TagChip
import com.fitlog.app.util.planEstMinutes

fun targetText(mode: String, sets: Int, w: Float?, r: Int?, t: Int?): String = when (mode) {
    "wr" -> "${sets}组 × ${com.fitlog.app.util.fmtWeight(w)}kg×${r ?: 0}次"
    "r" -> "${sets}组 × ${r ?: 0}次"
    else -> "${sets}组 × ${t ?: 0}秒"
}

@Composable
fun PlanDetailScreen(nav: NavController, planId: Long) {
    val app = LocalContext.current.applicationContext as FitLogApp
    var data by remember { mutableStateOf<PlanWithItems?>(null) }
    var exInfo by remember { mutableStateOf<Map<Long, Triple<String, String, String>>>(emptyMap()) } // id -> (part, name, mode)

    LaunchedEffect(planId) {
        val p = app.repo.plan(planId) ?: return@LaunchedEffect
        data = p
        val map = mutableMapOf<Long, Triple<String, String, String>>()
        p.items.forEach { itp ->
            if (!map.containsKey(itp.exerciseId)) {
                val e = app.repo.exercise(itp.exerciseId)
                if (e != null) map[e.id] = Triple(e.part, e.name, e.mode)
            }
        }
        exInfo = map
    }
    val d = data ?: return

    val items = d.items.sortedBy { it.orderIdx }
    val sets = items.sumOf { it.sets }
    val restSum = items.sumOf { it.restSec * (it.sets - 1).coerceAtLeast(0) }
    val minutes = planEstMinutes(sets, restSum)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            BackTopBar(d.plan.name, onBack = { nav.popBackStack() }) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { nav.navigate("planEdit?planId=${d.plan.id}") },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Edit, "编辑", Modifier.size(16.dp)) }
            }

            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${items.size} 个", "动作", Modifier.weight(1f))
                StatTile("$sets 组", "总组数", Modifier.weight(1f))
                StatTile("约$minutes 分", "预计时长", Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            items.forEach { itp ->
                val info = exInfo[itp.exerciseId]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (info != null) PartBadge(info.first)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(info?.second ?: "", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TagChip(
                                targetText(info?.third ?: "wr", itp.sets, itp.weight, itp.reps, itp.timeSec),
                                fg = MaterialTheme.colorScheme.primary,
                                bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            TagChip("休息 ${itp.restSec}s")
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
            Spacer(Modifier.height(92.dp))
        }

        Button(
            onClick = { nav.navigate("workout?planId=${d.plan.id}") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Text(" 开始训练", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
