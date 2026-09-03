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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.db.ExerciseEntity
import com.fitlog.app.data.model.Equips
import com.fitlog.app.data.model.Modes
import com.fitlog.app.data.model.Parts
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.ChipRow
import com.fitlog.app.ui.components.CheckCircle
import com.fitlog.app.ui.components.PartBadge
import com.fitlog.app.ui.components.TagChip
import com.fitlog.app.ui.nav.PickerBus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(nav: NavController, pickMode: Boolean) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val all by app.repo.exercises().collectAsStateWithLifecycle(initialValue = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var fPart by rememberSaveable { mutableStateOf("全部") }
    var fEquip by rememberSaveable { mutableStateOf("全部") }
    var selected by remember { mutableStateOf(setOf<Long>()) }

    // 离开选择模式时注销回调，防止悬空引用
    DisposableEffect(pickMode) {
        onDispose { if (pickMode) PickerBus.cancel() }
    }

    val list = all.filter { e ->
        (fPart == "全部" || e.part == fPart) &&
            (fEquip == "全部" || e.equip == fEquip) &&
            (query.isBlank() || e.name.contains(query.trim()))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (!pickMode) {
                ExtendedFloatingActionButton(
                    onClick = { nav.navigate("custom?exId=-1") },
                    icon = { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimary) },
                    text = { Text("自定义动作", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, maxLines = 1) },
                    containerColor = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        bottomBar = {
            if (pickMode && selected.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Button(
                        onClick = {
                            PickerBus.pick(selected.toList())
                            nav.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("添加 ${selected.size} 个动作", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1) }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {
            BackTopBar(if (pickMode) "选择动作" else "训练库", onBack = {
                if (pickMode) PickerBus.cancel()
                nav.popBackStack()
            }) {
                Text(
                    if (pickMode) "可多选" else "共 ${all.size} 个",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            // 搜索框
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
                Box(Modifier.weight(1f).padding(start = 8.dp)) {
                    if (query.isEmpty()) {
                        Text("搜索动作名称", fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            ChipRow(listOf("全部") + Parts.all, fPart, onSelect = { fPart = it })
            Spacer(Modifier.height(6.dp))
            ChipRow(listOf("全部") + Equips.all, fEquip, onSelect = { fEquip = it })
            Spacer(Modifier.height(8.dp))

            if (list.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("没有符合条件的动作\n换个筛选条件，或自定义一个动作", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(list.size, key = { list[it].id }) { i ->
                        val e = list[i]
                        ExerciseRow(
                            e = e,
                            selected = e.id in selected,
                            pickMode = pickMode,
                            onClick = {
                                if (pickMode) {
                                    selected = if (e.id in selected) selected - e.id else selected + e.id
                                } else {
                                    nav.navigate("exdetail/${e.id}")
                                }
                            }
                        )
                    }
                    item { Spacer(Modifier.height(if (pickMode) 20.dp else 70.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(e: ExerciseEntity, selected: Boolean, pickMode: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PartBadge(e.part)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.name, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (e.isCustom) {
                    Text(
                        "自定义",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Row(
                Modifier.padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagChip(e.part, fg = Parts.color(e.part), bg = Parts.color(e.part).copy(alpha = 0.1f))
                TagChip(e.equip)
                TagChip(Modes.label(e.mode))
            }
        }
        if (pickMode) {
            Spacer(Modifier.size(8.dp))
            CheckCircle(done = selected, onClick = onClick, size = 26.dp)
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
