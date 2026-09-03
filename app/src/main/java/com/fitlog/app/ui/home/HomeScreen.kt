package com.fitlog.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.R
import com.fitlog.app.data.prefs.UserProfile
import com.fitlog.app.ui.components.FitConfirmDialog
import com.fitlog.app.ui.components.PartBadge
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.components.TagChip
import com.fitlog.app.ui.components.toast
import com.fitlog.app.ui.theme.Green
import com.fitlog.app.ui.theme.HeroBlue1
import com.fitlog.app.ui.theme.HeroBlue2
import com.fitlog.app.ui.theme.HeroBlue3
import com.fitlog.app.util.planEstMinutes
import com.fitlog.app.util.todayStr
import com.fitlog.app.util.weekdayCn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val plans by app.repo.plans().collectAsStateWithLifecycle(initialValue = emptyList())
    val exercises by app.repo.exercises().collectAsStateWithLifecycle(initialValue = emptyList())
    val folders by app.repo.folders().collectAsStateWithLifecycle(initialValue = emptyList())
    val profile by app.settings.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val todaySessions by app.repo.sessionsByDate(todayStr()).collectAsStateWithLifecycle(initialValue = emptyList())
    val trained = todaySessions.isNotEmpty()
    var startSheet by remember { mutableStateOf(false) }
    var folderSheet by remember { mutableStateOf(false) }
    var delFolder by remember { mutableStateOf<Long?>(null) }

    // "all"=全部 "none"=未分类 其他=文件夹 id
    var selFolder by rememberSaveable { mutableStateOf("all") }
    val visiblePlans = when (selFolder) {
        "all" -> plans
        "none" -> plans.filter { it.plan.folderId == null }
        else -> plans.filter { it.plan.folderId.toString() == selFolder }
    }
    val folderNameById = folders.associate { it.id to it.name }

    val hour = remember { LocalTime.now().hour }
    val greeting = when {
        hour < 11 -> "早上好"
        hour < 14 -> "中午好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }
    val today = LocalDate.now()
    val dateLine = "${today.monthValue}月${today.dayOfMonth}日 星期${weekdayCn(today.toString())} · 今天练点什么？"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$greeting，${profile.name} 💪", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(dateLine, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (trained) Green.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    if (trained) "今天已训练 ✓" else "今天还未训练",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (trained) Green else MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        // 开始训练
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(HeroBlue1, HeroBlue2, HeroBlue3)))
                .clickable { startSheet = true }
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("开始训练", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("选择计划，或来一次空白训练", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.65f), modifier = Modifier.padding(top = 5.dp))
                }
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // 训练库卡片
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { nav.navigate("library?pick=0") }
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF3B7CFF), Color(0xFF7C5CFF)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_nav_workout), null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("训练库", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                        Text(
                            " ${exercises.size} 个动作",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text("按部位、器械筛选 · 支持自定义动作", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionHeader("我的计划", "＋ 新建计划") { nav.navigate("planEdit?planId=-1&folderId=$selFolder") }

        // 文件夹筛选条
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                Modifier.weight(1f),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                val chips = mutableListOf<Triple<String, String, Boolean>>()
                chips.add(Triple("all", "全部", true))
                chips.add(Triple("none", "未分类", false))
                folders.forEach { chips.add(Triple(it.id.toString(), it.name, true)) }
                items(chips) { (key, label, _) ->
                    val on = selFolder == key
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                            .clickable { selFolder = key }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .clickable { folderSheet = true }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("📁", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(10.dp))

        visiblePlans.forEach { pw ->
            val items = pw.items.sortedBy { it.orderIdx }
            val sets = items.sumOf { it.sets }
            val restSum = items.sumOf { it.restSec * (it.sets - 1).coerceAtLeast(0) }
            val minutes = planEstMinutes(sets, restSum)
            val lastLine = pw.plan.lastDone?.let { last ->
                val d = java.time.Instant.ofEpochMilli(last).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                "上次训练：${d.monthValue}月${d.dayOfMonth}日"
            } ?: "从未训练"
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { nav.navigate("planDetail/${pw.plan.id}") }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                pw.plan.name, fontSize = 15.5.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            pw.plan.folderId?.let { fid ->
                                folderNameById[fid]?.let { fname ->
                                    Text(
                                        " 📁 $fname",
                                        fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Text(
                            "${items.size} 个动作 · $sets 组 · 约 $minutes 分钟",
                            fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                        Text(lastLine, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(top = 3.dp))
                    }
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable { nav.navigate("workout?planId=${pw.plan.id}") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, "开始", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (visiblePlans.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 26.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("这个分类下还没有计划\n点右上角「＋ 新建计划」创建一个", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .clickable { nav.navigate("planEdit?planId=-1&folderId=$selFolder") }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("＋ 新建训练计划", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(26.dp))
        Text(
            "FitLog v0.2 · 数据保存在本机",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))
    }

    if (startSheet) {
        ModalBottomSheet(onDismissRequest = { startSheet = false }) {
            Text("开始训练", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            plans.forEach { pw ->
                val items = pw.items.sortedBy { it.orderIdx }
                val sets = items.sumOf { it.sets }
                val restSum = items.sumOf { it.restSec * (it.sets - 1).coerceAtLeast(0) }
                val firstPart = items.firstOrNull()?.let { itp ->
                    exercises.firstOrNull { it.id == itp.exerciseId }?.part
                } ?: "胸"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            startSheet = false
                            nav.navigate("workout?planId=${pw.plan.id}")
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PartBadge(firstPart)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(pw.plan.name, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("${items.size} 个动作 · $sets 组 · 约 ${planEstMinutes(sets, restSum)} 分钟", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        startSheet = false
                        nav.navigate("workout?planId=-1")
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { Text("＋", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("空白训练", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("不使用计划，现场添加动作", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }

    // 文件夹管理
    if (folderSheet) {
        ModalBottomSheet(onDismissRequest = { folderSheet = false }) {
            var newName by remember { mutableStateOf("") }
            Text("管理文件夹", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "删除文件夹不会删除其中的计划，计划会移入「未分类」",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("新文件夹名称", fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        val n = newName.trim()
                        if (n.isNotEmpty()) {
                            scope.launch {
                                app.repo.addFolder(n)
                                newName = ""
                                toast(ctx, "已创建「$n」")
                            }
                        } else toast(ctx, "请输入文件夹名称")
                    }
                ) { Text("创建", fontWeight = FontWeight.Bold) }
            }
            folders.forEach { f ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selFolder = f.id.toString(); folderSheet = false }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📁", fontSize = 14.sp)
                    Text(
                        "${f.name}（${plans.count { it.plan.folderId == f.id }} 个计划）",
                        fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 10.dp)
                    )
                    Icon(
                        Icons.Filled.Delete, "删除文件夹",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { delFolder = f.id }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    delFolder?.let { fid ->
        val fname = folderNameById[fid] ?: ""
        FitConfirmDialog(
            title = "删除文件夹",
            message = "「$fname」中的计划不会被删除，将移入「未分类」。确认删除？",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                scope.launch {
                    app.repo.deleteFolder(fid)
                    if (selFolder == fid.toString()) selFolder = "all"
                    toast(ctx, "文件夹已删除")
                }
                delFolder = null
            },
            onDismiss = { delFolder = null }
        )
    }
}
