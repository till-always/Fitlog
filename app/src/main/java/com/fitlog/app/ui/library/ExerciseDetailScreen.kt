package com.fitlog.app.ui.library

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.db.ExerciseEntity
import com.fitlog.app.data.model.Modes
import com.fitlog.app.data.model.Parts
import com.fitlog.app.data.repo.FitnessRepository
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.FitConfirmDialog
import com.fitlog.app.ui.components.PartBadge
import com.fitlog.app.ui.components.TagChip
import com.fitlog.app.ui.components.toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(nav: NavController, id: Long) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ex by remember { mutableStateOf<ExerciseEntity?>(null) }
    LaunchedEffect(id) { ex = app.repo.exercise(id) }
    val e = ex ?: return

    var addSheet by remember { mutableStateOf(false) }
    var delConfirm by remember { mutableStateOf(false) }
    val plans by app.repo.plans().collectAsStateWithLifecycle(initialValue = emptyList())

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            BackTopBar("动作详情", onBack = { nav.popBackStack() }) {
                if (e.isCustom) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { nav.navigate("custom?exId=${e.id}") },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Edit, "编辑", Modifier.size(16.dp)) }
                }
            }

            // 头部
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(14.dp))
                PartBadge(e.part, size = 84.dp, radius = 26.dp)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    if (e.isCustom) {
                        Text(
                            "自定义",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(e.part, fg = Parts.color(e.part), bg = Parts.color(e.part).copy(alpha = 0.1f))
                    TagChip(e.equip)
                    TagChip(Modes.label(e.mode))
                }
            }
            Spacer(Modifier.height(18.dp))

            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("记录方式", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(Modes.label(e.mode), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("动作要领", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TipRow(e.tip)
                    TipRow("训练前充分热身，选择能标准完成目标次数的重量；动作质量优先于重量。")
                }
            }
            Spacer(Modifier.height(90.dp))
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (e.isCustom) {
                OutlinedButton(
                    onClick = { delConfirm = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(15.dp)
                ) { Text("删除动作", color = MaterialTheme.colorScheme.error) }
            }
            Button(
                onClick = { addSheet = true },
                modifier = Modifier.weight(if (e.isCustom) 1f else 2f).height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("加入计划", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }

    if (addSheet) {
        ModalBottomSheet(onDismissRequest = { addSheet = false }) {
            Text("加入哪个计划？", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            plans.forEach { pw ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            addSheet = false
                            scope.launch {
                                val rest = app.settings.defaultRest.first()
                                app.repo.appendPlanItem(pw.plan.id, FitnessRepository.defaultItem(e, rest))
                                toast(ctx, "已加入「${pw.plan.name}」")
                                nav.popBackStack()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PartBadge(e.part)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(pw.plan.name, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        Text("${pw.items.size} 个动作", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (delConfirm) {
        FitConfirmDialog(
            title = "删除动作",
            message = "删除后将同时从训练库移除，历史训练记录不受影响。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                delConfirm = false
                scope.launch {
                    if (app.repo.exerciseInUse(e.id)) {
                        toast(ctx, "该动作已被训练计划使用，请先在计划中移除它")
                    } else {
                        app.repo.deleteExercise(e)
                        toast(ctx, "已删除")
                        nav.popBackStack()
                    }
                }
            },
            onDismiss = { delConfirm = false }
        )
    }
}

@Composable
private fun TipRow(text: String) {
    Row(Modifier.padding(vertical = 10.dp)) {
        Box(
            Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text, fontSize = 14.sp, lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}
