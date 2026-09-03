package com.fitlog.app.ui.training

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fitlog.app.data.model.Modes
import com.fitlog.app.ui.components.CheckCircle
import com.fitlog.app.ui.components.FitConfirmDialog
import com.fitlog.app.ui.components.PartBadge
import com.fitlog.app.ui.components.toast
import com.fitlog.app.ui.nav.PickerBus
import com.fitlog.app.ui.theme.Green
import com.fitlog.app.ui.theme.HeroBlue1
import com.fitlog.app.ui.theme.HeroBlue2
import com.fitlog.app.ui.theme.HeroBlue3
import com.fitlog.app.ui.theme.Red
import com.fitlog.app.util.fmtDur
import com.fitlog.app.util.fmtVolume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(nav: NavController, planId: Long?) {
    val activity = LocalContext.current as ComponentActivity
    val vm: TrainingViewModel = viewModel(viewModelStoreOwner = activity)
    val ctx = LocalContext.current

    // 每次进入训练页都按所点计划无条件重建，杜绝旧训练残留
    LaunchedEffect(planId) { vm.start(planId) }

    val w = vm.workout
    if (w == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var showExit by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var editIdx by remember { mutableStateOf(-1) }
    var delIdx by remember { mutableStateOf(-1) }
    var setEdit by remember { mutableStateOf<Triple<Int, Int, String>?>(null) }
    // 折叠状态按动作 id 记录（按索引会在增删动作后串位）
    val collapsed = remember { mutableStateMapOf<Long, Boolean>() }

    // 系统返回键分级处理：全屏计时 → 取消计时；休息中 → 跳过；否则确认结束
    BackHandler {
        when {
            vm.timed != null -> vm.cancelTimed()
            vm.rest != null -> vm.skipRest()
            else -> showExit = true
        }
    }

    val (total, done, volume) = vm.totals()
    val pct = if (total > 0) done.toFloat() / total else 0f

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(HeroBlue1, HeroBlue2, HeroBlue3)))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "TRAINING", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.5f), letterSpacing = 2.5.sp
                        )
                        Text(
                            w.planName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            modifier = Modifier.weight(1f).padding(start = 10.dp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { showExit = true },
                            contentAlignment = Alignment.Center
                        ) { Text("✕", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            // 计时单独隔离重组：每 500ms 只有这一行重绘，不牵动整屏
                            ElapsedText(vm)
                            Text(
                                "已完成 $done/$total 组 · 容量 ${fmtVolume(volume)} kg",
                                fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(62.dp)) {
                                val stroke = 6.dp.toPx()
                                drawArc(
                                    color = Color.White.copy(alpha = 0.14f), startAngle = -90f, sweepAngle = 360f,
                                    useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = Color(0xFF3B7CFF), startAngle = -90f, sweepAngle = 360f * pct,
                                    useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                            }
                            Text("${(pct * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
            }

            items(w.items.size, key = { i -> "${w.items[i].exId}-$i" }) { i ->
                ExerciseCard(
                    vm = vm, w = w, i = i,
                    collapsed = collapsed[w.items[i].exId] ?: false,
                    onToggleCollapse = { collapsed[w.items[i].exId] = !(collapsed[w.items[i].exId] ?: false) },
                    onEdit = { editIdx = i },
                    onDelete = { delIdx = i },
                    onEditSet = { j, f -> setEdit = Triple(i, j, f) },
                    onAllDone = { collapsed[w.items[i].exId] = true }
                )
            }

            item { Spacer(Modifier.height(120.dp)) }
        }

        // 组间休息：全屏居中弹窗（与计时遮罩同款，防误触）
        vm.rest?.let { r ->
            RestOverlay(
                remain = r.remain,
                total = r.total,
                nextText = nextUpText(w),
                onSub15 = { vm.subRest15() },
                onAdd15 = { vm.addRest15() },
                onSkip = { vm.skipRest() }
            )
        }

        // 底部操作（固定宽度，防小屏换行）
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    PickerBus.await { ids ->
                        if (ids.isNotEmpty()) {
                            vm.addExercises(ids)
                            toast(ctx, "已添加 ${ids.size} 个动作")
                        }
                    }
                    nav.navigate("library?pick=1")
                },
                modifier = Modifier.width(96.dp).height(50.dp),
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) { Text("＋ 动作", maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            Button(
                onClick = {
                    if (done < total) confirmFinish = true
                    else {
                        vm.finish()
                        nav.navigate("summary?sessionId=-1") {
                            popUpTo("workout?planId={planId}") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) { Text("完成训练", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White, maxLines = 1) }
        }
    }

    // 全屏计时遮罩：状态读取隔离在 Host 内部，倒计时不牵动整屏重组
    TimedOverlayHost(vm = vm, w = w)

    if (showExit) {
        FitConfirmDialog(
            title = "结束本次训练？",
            message = "训练尚未保存，退出后本次记录不会保留。",
            confirmText = "退出",
            danger = true,
            onConfirm = {
                showExit = false
                vm.discard()
                nav.popBackStack()
            },
            onDismiss = { showExit = false }
        )
    }

    if (confirmFinish) {
        FitConfirmDialog(
            title = "还有组未完成",
            message = "本次训练已完成 $done/$total 组，确认提前结束并结算吗？",
            confirmText = "完成训练",
            onConfirm = {
                confirmFinish = false
                vm.finish()
                nav.navigate("summary?sessionId=-1") {
                    popUpTo("workout?planId={planId}") { inclusive = true }
                }
            },
            onDismiss = { confirmFinish = false }
        )
    }

    if (editIdx >= 0) {
        val ex = w.items.getOrNull(editIdx)
        if (ex == null) {
            editIdx = -1
        } else {
            EditTargetsSheet(
                ex = ex,
                onDismiss = { editIdx = -1 },
                onApply = { name, weight, reps, time, rest ->
                    vm.updateTargets(editIdx, name, weight, reps, time, rest)
                    editIdx = -1
                    toast(ctx, "已更新动作目标")
                }
            )
        }
    }

    if (delIdx >= 0) {
        val ex = w.items.getOrNull(delIdx)
        FitConfirmDialog(
            title = "移除动作",
            message = "本次训练不再记录「${ex?.displayName ?: ""}」，已完成的组也不会计入结算。",
            confirmText = "移除",
            danger = true,
            onConfirm = {
                vm.removeExercise(delIdx)
                delIdx = -1
                toast(ctx, "已移除")
            },
            onDismiss = { delIdx = -1 }
        )
    }

    if (setEdit != null) {
        val (i, j, f) = setEdit!!
        val ex = w.items.getOrNull(i)
        val row = ex?.rows?.getOrNull(j)
        if (ex == null || row == null) {
            setEdit = null
        } else {
            SetEditorSheet(
                title = "调整${fieldLabel(f)} · 第 ${j + 1} 组",
                subtitle = ex.displayName,
                field = f,
                initWeight = row.weight, initReps = row.reps, initTime = row.timeSec,
                onDismiss = { setEdit = null },
                onApply = { weight, reps, time ->
                    vm.updateRowValue(i, j, weight, reps, time)
                    setEdit = null
                }
            )
        }
    }
}

private fun fieldLabel(f: String): String = when (f) {
    "w" -> "重量"
    "r" -> "次数"
    else -> "时长"
}

/** 计时文本：唯一读取 elapsedSec 的重组作用域，保证每 500ms 只重绘这一行 */
@Composable
private fun ElapsedText(vm: TrainingViewModel) {
    val elapsed = vm.elapsedSec
    Text(
        fmtDur(elapsed), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
        color = Color.White, maxLines = 1
    )
}

/** 计时遮罩宿主：内部读取 timed 状态，避免训练页每秒整屏重组 */
@Composable
private fun TimedOverlayHost(vm: TrainingViewModel, w: WorkoutState) {
    val t = vm.timed ?: return
    val ex = w.items.getOrNull(t.exIdx)
    TimedOverlay(
        remain = t.remain,
        total = t.total,
        name = ex?.displayName ?: "",
        part = ex?.part ?: "腹",
        setNo = t.rowIdx + 1,
        onCancel = { vm.cancelTimed() },
        onDone = { vm.stopTimedEarly() }
    )
}

/** 休息结束后下一组提示：按顺序找第一组未完成的 */
private fun nextUpText(w: WorkoutState): String {
    w.items.forEach { ex ->
        ex.rows.forEachIndexed { j, r ->
            if (!r.done) return "下一组：${ex.displayName} · 第 ${j + 1} 组"
        }
    }
    return "全部完成，可以结束训练 💪"
}

/** 组间休息全屏遮罩：深色打底拦截误触，−15s / +15s / 跳过 */
@Composable
private fun RestOverlay(
    remain: Int,
    total: Int,
    nextText: String,
    onSub15: () -> Unit,
    onAdd15: () -> Unit,
    onSkip: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF217171D))
            .clickable(interactionSource = interaction, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "组间休息", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f), letterSpacing = 4.sp
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 26.dp)) {
                Canvas(Modifier.size(216.dp)) {
                    val stroke = 10.dp.toPx()
                    drawArc(Color.White.copy(alpha = 0.1f), -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(
                        Color(0xFF3B7CFF), -90f,
                        360f * (remain.toFloat() / total.coerceAtLeast(1)),
                        false, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$remain", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("秒", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }
            Text(
                nextText, fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp).padding(horizontal = 32.dp),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(Modifier.padding(top = 34.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSub15,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) { Text("−15s", color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1) }
                OutlinedButton(
                    onClick = onAdd15,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) { Text("+15s", color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1) }
                Button(
                    onClick = onSkip,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF17181A))
                ) { Text("跳过休息", fontWeight = FontWeight.Bold, maxLines = 1) }
            }
        }
    }
}

/** 全屏倒计时遮罩：深色打底拦截一切误触 */
@Composable
private fun TimedOverlay(
    remain: Int,
    total: Int,
    name: String,
    part: String,
    setNo: Int,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF217171D))
            .clickable(interactionSource = interaction, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PartBadge(part, size = 56.dp, radius = 18.dp)
            Text(
                name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 12.dp).padding(horizontal = 32.dp)
            )
            Text("第 $setNo 组", fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 4.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 26.dp)) {
                Canvas(Modifier.size(216.dp)) {
                    val stroke = 10.dp.toPx()
                    drawArc(Color.White.copy(alpha = 0.1f), -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(
                        Color(0xFF3B7CFF), -90f,
                        360f * (remain.toFloat() / total.coerceAtLeast(1)),
                        false, style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$remain", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("秒", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                }
            }
            Row(Modifier.padding(top = 36.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) { Text("取消计时", color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1) }
                Button(
                    onClick = onDone,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF17181A))
                ) { Text("提前完成", fontWeight = FontWeight.Bold, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    vm: TrainingViewModel,
    w: WorkoutState,
    i: Int,
    collapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditSet: (Int, String) -> Unit,
    onAllDone: () -> Unit
) {
    val ex = w.items[i]
    val timed = vm.timed
    val liveThis = timed != null && timed.exIdx == i
    val allDone = ex.rows.isNotEmpty() && ex.rows.all { it.done }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // 全部组完成 → 自动折叠
    LaunchedEffect(allDone) { if (allDone) onAllDone() }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .animateContentSize()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onToggleCollapse() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PartBadge(ex.part)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    ex.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (ex.mode == Modes.TIME) "${ex.equip} · 点击 ✓ 开始倒计时 · 休息 ${ex.restSec}s"
                    else "${ex.equip} · ${Modes.label(ex.mode)} · 休息 ${ex.restSec}s",
                    fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (allDone) {
                Text(
                    "✓ ${ex.rows.size}/${ex.rows.size}",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Green,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Green.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            Icon(
                if (collapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = if (collapsed) "展开" else "折叠",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp).size(18.dp)
            )
            Spacer(Modifier.size(6.dp))
            MiniAction(Icons.Filled.Edit, "编辑") { onEdit() }
            Spacer(Modifier.size(6.dp))
            MiniAction(Icons.Filled.Delete, "移除", tint = MaterialTheme.colorScheme.error) { onDelete() }
        }

        if (!collapsed) {
            Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 4.dp)) {
                ex.rows.forEachIndexed { j, r ->
                    val live = liveThis && timed?.rowIdx == j
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    live -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    r.done -> Green.copy(alpha = 0.07f)
                                    else -> Color.Transparent
                                }
                            )
                            .padding(vertical = 9.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${j + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(34.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        when (ex.mode) {
                            Modes.WEIGHT_REPS -> {
                                SetCell("重量", fmtWeight(r.weight), "kg") { onEditSet(j, "w") }
                                SetCell("次数", "${r.reps ?: 0}", "次") { onEditSet(j, "r") }
                            }
                            Modes.REPS -> SetCell("次数", "${r.reps ?: 0}", "次") { onEditSet(j, "r") }
                            else -> SetCell(
                                "时长",
                                if (live) "${timed?.remain ?: 0}" else "${r.timeSec ?: 0}",
                                if (live) "秒后完成" else "秒"
                            ) { if (!live) onEditSet(j, "t") }
                        }
                        if (live) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Red)
                                    .clickable { vm.stopTimedEarly() }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White)
                                )
                            }
                        } else {
                            CheckCircle(done = r.done, onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                vm.toggleRow(i, j)
                            }, size = 30.dp)
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "− 删减一组", fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .clickable { vm.removeRow(i) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text("共 ${ex.rows.size} 组", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Text(
                    "＋ 添加一组", fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .clickable { vm.addRow(i) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SetCell(label: String, value: String, unit: String, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
            Text(value, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
            Text(" $unit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun MiniAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(14.dp)) }
}

private fun fmtWeight(w: Float?): String {
    val v = w ?: 0f
    return if (v % 1f == 0f) "%.0f".format(v) else "%.1f".format(v)
}
