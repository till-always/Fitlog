package com.fitlog.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.prefs.UserProfile
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.ChipRow
import com.fitlog.app.ui.components.toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf("") }
    var sig by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var ready by rememberSaveable { mutableStateOf(false) }

    // 一次性装载，避免数据流晚到覆盖用户正在输入的内容
    LaunchedEffect(Unit) {
        val p = app.settings.profile.first()
        name = p.name; sig = p.signature; gender = p.gender
        height = p.heightCm.toString(); weight = p.weightKg.toString()
        ready = true
    }
    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        BackTopBar("编辑资料", onBack = { nav.popBackStack() })
        Spacer(Modifier.height(6.dp))
        FieldLabel("昵称")
        OutField(name, "你的昵称") { name = it }
        FieldLabel("性别")
        ChipRow(listOf("男", "女", "保密"), gender.ifBlank { "男" }, { gender = it }, Modifier.padding(top = 8.dp))
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("身高 (cm)")
                OutField(height, "178") { height = it }
            }
            Column(Modifier.weight(1f)) {
                FieldLabel("体重 (kg)")
                OutField(weight, "72.5") { weight = it }
            }
        }
        FieldLabel("个性签名")
        OutField(sig, "一句话介绍自己", minLines = 2) { sig = it }

        Spacer(Modifier.height(26.dp))
        Button(
            onClick = {
                val p = UserProfile(
                    name = name.trim().ifBlank { "FitLog 用户" },
                    signature = sig.trim(),
                    gender = gender.ifBlank { "男" },
                    heightCm = height.toIntOrNull() ?: 178,
                    weightKg = weight.toFloatOrNull() ?: 72.5f
                )
                scope.launch {
                    app.settings.saveProfile(p)
                    toast(ctx, "资料已保存")
                    nav.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) { Text("保存", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun OutField(
    value: String,
    placeholder: String,
    minLines: Int = 1,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, fontSize = 14.5.sp) },
        minLines = minLines,
        singleLine = minLines == 1,
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
