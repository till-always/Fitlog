package com.fitlog.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.db.ExerciseEntity
import com.fitlog.app.data.model.Equips
import com.fitlog.app.data.model.Modes
import com.fitlog.app.data.model.Parts
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.ChipRow
import com.fitlog.app.ui.components.toast
import kotlinx.coroutines.launch

@Composable
fun CustomExerciseScreen(nav: NavController, exId: Long) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val editing = exId > 0

    var name by rememberSaveable { mutableStateOf("") }
    var tip by rememberSaveable { mutableStateOf("") }
    var part by rememberSaveable { mutableStateOf("胸") }
    var equip by rememberSaveable { mutableStateOf("徒手") }
    var mode by rememberSaveable { mutableStateOf(Modes.REPS) }
    var loaded by rememberSaveable { mutableStateOf(!editing) }

    LaunchedEffect(editing) {
        if (editing) {
            val e = app.repo.exercise(exId)
            if (e == null) {
                // 动作不存在（已被删除等）：兜底返回，避免永久白屏
                toast(ctx, "动作不存在或已被删除")
                nav.popBackStack()
                return@LaunchedEffect
            }
            name = e.name; tip = e.tip; part = e.part; equip = e.equip; mode = e.mode; loaded = true
        }
    }
    if (!loaded) return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        BackTopBar(if (editing) "编辑自定义动作" else "自定义动作", onBack = { nav.popBackStack() })
        Spacer(Modifier.height(2.dp))
        Text("动作名称", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("例如：单腿臀桥", fontSize = 14.5.sp) },
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

        Text("训练部位", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
        ChipRow(Parts.all, part, { part = it }, Modifier.padding(top = 8.dp))

        Text("器械要求", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
        ChipRow(Equips.all, equip, { equip = it }, Modifier.padding(top = 8.dp))

        Text("记录方式", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Modes.all.forEach { (key, label) ->
                val on = mode == key
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            2.dp,
                            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { mode = key }
                ) {
                    Text(
                        label,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Text("动作说明（可选）", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
        OutlinedTextField(
            value = tip,
            onValueChange = { tip = it },
            placeholder = { Text("记录动作要领，方便以后查看", fontSize = 14.sp) },
            minLines = 3,
            shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                if (name.isBlank()) { toast(ctx, "请填写动作名称"); return@Button }
                scope.launch {
                    val cleanTip = tip.trim().ifBlank { "自定义动作，注意动作质量。" }
                    if (editing) {
                        val old = app.repo.exercise(exId) ?: return@launch
                        app.repo.updateExercise(old.copy(name = name.trim(), part = part, equip = equip, mode = mode, tip = cleanTip))
                        toast(ctx, "已保存修改")
                    } else {
                        app.repo.addExercise(
                            ExerciseEntity(name = name.trim(), part = part, equip = equip, mode = mode, tip = cleanTip, isCustom = true)
                        )
                        toast(ctx, "「${name.trim()}」已加入训练库")
                    }
                    nav.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("保存到训练库", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }
}
