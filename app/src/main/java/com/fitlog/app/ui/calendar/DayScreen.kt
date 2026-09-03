package com.fitlog.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.components.StatTile
import com.fitlog.app.ui.components.TagChip
import com.fitlog.app.util.dateCn
import com.fitlog.app.util.fmtClock
import com.fitlog.app.util.fmtLong

@Composable
fun DayScreen(nav: NavController, date: String) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val sessions by app.repo.sessionsByDate(date).collectAsStateWithLifecycle(initialValue = emptyList())
    val totalSec = sessions.sumOf { it.durSec.toLong() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        BackTopBar(dateCn(date), onBack = { nav.popBackStack() })
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("${sessions.size} 次", "训练次数", Modifier.weight(1f))
            StatTile(fmtLong(totalSec), "总时长", Modifier.weight(1f))
        }
        SectionHeader("训练记录")
        sessions.forEach { s ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { nav.navigate("summary?sessionId=${s.id}") }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(s.planName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(top = 6.dp)) {
                        Text("🕐 ${fmtClock(s.startTs)}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("   ⏱ ${fmtLong(s.durSec.toLong())}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("   ✓ ${s.setsDone}/${s.setsTotal} 组", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        s.parts.split(",").filter { it.isNotBlank() }.forEach { p ->
                            TagChip(p)
                        }
                    }
                }
            }
        }
        if (sessions.isEmpty()) {
            Text("这一天没有训练记录", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 30.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
    }
}
