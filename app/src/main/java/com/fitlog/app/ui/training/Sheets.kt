package com.fitlog.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.model.Modes

private fun sanitizeNumber(raw: String, allowDot: Boolean): String {
    var out = raw.filter { c -> c.isDigit() || (allowDot && c == '.') }
    if (allowDot) {
        val firstDot = out.indexOf('.')
        if (firstDot >= 0) {
            out = out.substring(0, firstDot + 1) + out.substring(firstDot + 1).replace(".", "")
        }
    }
    return out
}

private fun fmtNum(v: Double): String =
    if (v % 1.0 == 0.0) "%.0f".format(v) else "%.1f".format(v)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTargetsSheet(
    ex: TrainingEx,
    onDismiss: () -> Unit,
    onApply: (name: String, weight: Float?, reps: Int?, time: Int?, rest: Int) -> Unit
) {
    var name by remember { mutableStateOf(ex.displayName) }
    var weight by remember { mutableStateOf(fmtNum((ex.rows.firstOrNull()?.weight ?: 0f).toDouble())) }
    var reps by remember { mutableStateOf("${ex.rows.firstOrNull()?.reps ?: 10}") }
    var time by remember { mutableStateOf("${ex.rows.firstOrNull()?.timeSec ?: 45}") }
    var rest by remember { mutableStateOf("${ex.restSec}") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.imePadding()) {
            Text("自定义动作", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                Modes.label(ex.mode) + " · 应用到未完成组",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (ex.mode == Modes.WEIGHT_REPS) {
                CtlInputRow("目标重量", weight, "kg", step = 2.5) { weight = it }
            }
            if (ex.mode == Modes.WEIGHT_REPS || ex.mode == Modes.REPS) {
                CtlInputRow("目标次数", reps, "次", step = 1.0) { reps = it }
            }
            if (ex.mode == Modes.TIME) {
                CtlInputRow("每组时长", time, "秒", step = 5.0) { time = it }
            }
            CtlInputRow("组间休息", rest, "秒", step = 15.0) { rest = it }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("取消") }
                Button(
                    onClick = {
                        val w = if (ex.mode == Modes.WEIGHT_REPS) (weight.toDoubleOrNull() ?: 0.0).toFloat().coerceAtLeast(0f) else null
                        val r = if (ex.mode != Modes.TIME) ((reps.toIntOrNull() ?: 0)).coerceAtLeast(0) else null
                        val t = if (ex.mode == Modes.TIME) ((time.toIntOrNull() ?: 0)).coerceAtLeast(0) else null
                        val rs = (rest.toIntOrNull() ?: 0).coerceAtLeast(0)
                        onApply(name.trim(), w, r, t, rs)
                    },
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("应用到未完成组", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

/** 标签 + 数值直输 + ± 步进 */
@Composable
private fun CtlInputRow(label: String, value: String, unit: String, step: Double, onChange: (String) -> Unit) {
    val allowDot = step % 1.0 != 0.0
    fun bumped(delta: Double): String {
        val cur = value.toDoubleOrNull() ?: 0.0
        val next = ((cur + delta).coerceAtLeast(0.0) * 10).toInt() / 10.0
        return fmtNum(next)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        StepCircle("−") { onChange(bumped(-step)) }
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(sanitizeNumber(it, allowDot)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = TextStyle(
                fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.width(92.dp).padding(horizontal = 8.dp)
        )
        Text(" $unit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StepCircle("＋") { onChange(bumped(step)) }
    }
}

@Composable
private fun StepCircle(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetEditorSheet(
    title: String,
    subtitle: String,
    field: String,
    initWeight: Float?,
    initReps: Int?,
    initTime: Int?,
    onDismiss: () -> Unit,
    onApply: (weight: Float?, reps: Int?, time: Int?) -> Unit
) {
    val initial = when (field) {
        "w" -> fmtNum((initWeight ?: 0f).toDouble())
        "r" -> "${initReps ?: 0}"
        else -> "${initTime ?: 0}"
    }
    var text by remember { mutableStateOf(initial) }
    val allowDot = field == "w"

    fun bumped(delta: Double): String {
        val cur = text.toDoubleOrNull() ?: 0.0
        return fmtNum(((cur + delta).coerceAtLeast(0.0) * 10).toInt() / 10.0)
    }

    val unit = when (field) {
        "w" -> "kg"
        "r" -> "次"
        else -> "秒"
    }
    val step = when (field) {
        "w" -> 2.5
        "r" -> 1.0
        else -> 5.0
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.imePadding()) {
            Text(
                title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), textAlign = TextAlign.Center
            )
            Text(
                subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepCircleBig("−") { text = bumped(-step) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = sanitizeNumber(it, allowDot) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.width(150.dp)
                )
                StepCircleBig("＋") { text = bumped(step) }
            }
            Text(
                unit, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("取消") }
                Button(
                    onClick = {
                        val v = text.toDoubleOrNull() ?: 0.0
                        when (field) {
                            "w" -> onApply(v.toFloat().coerceAtLeast(0f), initReps, initTime)
                            "r" -> onApply(initWeight, v.toInt().coerceAtLeast(0), initTime)
                            else -> onApply(initWeight, initReps, v.toInt().coerceAtLeast(0))
                        }
                    },
                    modifier = Modifier.weight(2f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("确定", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun StepCircleBig(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
