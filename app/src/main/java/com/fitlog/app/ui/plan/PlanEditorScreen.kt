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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.PartBadge
import com.fitlog.app.ui.components.TagChip
import com.fitlog.app.ui.components.toast
import com.fitlog.app.ui.nav.PickerBus
import kotlinx.coroutines.launch

@Composable
fun PlanEditorScreen(nav: NavController, planId: Long?, defaultFolderArg: String = "none") {
    val vm: PlanEditorViewModel = viewModel()
    val ctx = LocalContext.current
    val app = ctx.applicationContext as FitLogApp
    val scope = rememberCoroutineScope()
    LaunchedEffect(planId, defaultFolderArg) { vm.load(planId, defaultFolderArg) }
    if (!vm.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val folders by app.repo.folders().collectAsStateWithLifecycle(initialValue = emptyList())
    var delConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            BackTopBar(if (vm.planId != null) "编辑计划" else "新建计划", onBack = { nav.popBackStack() })
            Spacer(Modifier.height(2.dp))
            Text("计划名称", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                placeholder = { Text("例如：推力日 · 胸肩", fontSize = 14.5.sp) },
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Text("所属文件夹", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
            LazyRow(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = mutableListOf("none" to "未分类")
                folders.forEach { options.add(it.id.toString() to it.name) }
                items(options.size) { oi ->
                    val (key, label) = options[oi]
                    val on = vm.folderId?.toString() ?: "none"
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (on == key) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (on == key) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (on == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                            .clickable { vm.folderId = if (key == "none") null else key.toLongOrNull() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("动作清单（${vm.items.size}）", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            vm.items.forEachIndexed { i, it ->
                EditorItemCard(it,
                    onRemove = { vm.removeAt(i) },
                    onMove = { d -> vm.move(i, d) },
                    onUpdate = { t -> vm.replaceAt(i, t) }
                )
                Spacer(Modifier.height(12.dp))
            }
            if (vm.items.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("还没有动作\n从训练库挑选动作加入吧", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable {
                        PickerBus.await { ids ->
                            ids.forEach { vm.addItem(it) }
                            if (ids.isNotEmpty()) toast(ctx, "已添加 ${ids.size} 个动作")
                        }
                        nav.navigate("library?pick=1")
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("＋ 从训练库添加动作", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(130.dp))
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (vm.name.isBlank()) { toast(ctx, "给计划起个名字吧"); return@Button }
                    if (vm.items.isEmpty()) { toast(ctx, "至少添加一个动作"); return@Button }
                    vm.save {
                        toast(ctx, if (vm.planId != null) "计划已保存" else "计划已创建")
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("保存计划", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            if (vm.planId != null) {
                Text(
                    "删除计划",
                    fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { delConfirm = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    if (delConfirm) {
        com.fitlog.app.ui.components.FitConfirmDialog(
            title = "删除计划",
            message = "「${vm.name}」及其动作清单将被永久删除，确认删除？",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                delConfirm = false
                scope.launch {
                    vm.planId?.let { app.repo.deletePlan(it) }
                    toast(ctx, "计划已删除")
                    nav.navigate("home") { popUpTo(0) { inclusive = true } }
                }
            },
            onDismiss = { delConfirm = false }
        )
    }
}

@Composable
private fun EditorItemCard(
    it: EditorItem,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
    onUpdate: (EditorItem) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PartBadge(it.part, size = 40.dp, radius = 12.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(it.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(it.equip, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(com.fitlog.app.data.model.Modes.label(it.mode), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            MiniIcon(Icons.Filled.KeyboardArrowUp, "上移") { onMove(-1) }
            Spacer(Modifier.size(6.dp))
            MiniIcon(Icons.Filled.KeyboardArrowDown, "下移") { onMove(1) }
            Spacer(Modifier.size(6.dp))
            MiniIcon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.error) { onRemove() }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            CtlCell("组数", it.sets, "组", { onUpdate(it.copy(sets = (it.sets - 1).coerceAtLeast(1))) }, { onUpdate(it.copy(sets = it.sets + 1)) }, Modifier.weight(1f))
            if (it.mode == "wr") {
                CtlCell("重量", com.fitlog.app.util.fmtWeight(it.weight), "kg", { onUpdate(it.copy(weight = stepW(it.weight, -2.5f))) }, { onUpdate(it.copy(weight = stepW(it.weight, 2.5f))) }, Modifier.weight(1f))
            } else if (it.mode == "t") {
                CtlCell("时长", it.timeSec ?: 45, "秒", { onUpdate(it.copy(timeSec = ((it.timeSec ?: 45) - 5).coerceAtLeast(5))) }, { onUpdate(it.copy(timeSec = (it.timeSec ?: 45) + 5)) }, Modifier.weight(1f))
            } else {
                CtlCell("次数", it.reps ?: 12, "次", { onUpdate(it.copy(reps = ((it.reps ?: 12) - 1).coerceAtLeast(1))) }, { onUpdate(it.copy(reps = (it.reps ?: 12) + 1)) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            if (it.mode == "wr") {
                CtlCell("次数", it.reps ?: 8, "次", { onUpdate(it.copy(reps = ((it.reps ?: 8) - 1).coerceAtLeast(1))) }, { onUpdate(it.copy(reps = (it.reps ?: 8) + 1)) }, Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            CtlCell("组间休息", it.restSec, "秒", { onUpdate(it.copy(restSec = (it.restSec - 15).coerceAtLeast(0))) }, { onUpdate(it.copy(restSec = it.restSec + 15)) }, Modifier.weight(1f))
        }
    }
}

private fun stepW(cur: Float?, d: Float): Float = (((cur ?: 0f) + d).coerceAtLeast(0f) * 10).toInt() / 10f

@Composable
private fun CtlCell(
    label: String,
    value: Any,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepBtn("−", onMinus)
            Text(
                buildString { append(value); append(" ") },
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StepBtn("＋", onPlus)
        }
    }
}

@Composable
private fun StepBtn(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MiniIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(15.dp))
    }
}
