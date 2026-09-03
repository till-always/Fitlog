package com.fitlog.app.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavController
import com.fitlog.app.FitLogApp
import com.fitlog.app.ui.components.BackTopBar
import com.fitlog.app.ui.components.TagChip
import com.fitlog.app.ui.components.toast
import kotlinx.coroutines.launch

private val REST_OPTIONS = listOf(30, 45, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val dark by app.settings.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val rest by app.settings.defaultRest.collectAsStateWithLifecycle(initialValue = 60)
    val vibrate by app.settings.vibrate.collectAsStateWithLifecycle(initialValue = true)
    val sound by app.settings.sound.collectAsStateWithLifecycle(initialValue = true)
    var restSheet by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        BackTopBar("设置", onBack = { nav.popBackStack() })
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                SwitchRow("夜间模式", dark) { v -> scope.launch { app.settings.setDarkMode(v) } }
                MenuDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { restSheet = true }
                        .padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("默认组间休息", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TagChip(
                        "$rest 秒",
                        fg = MaterialTheme.colorScheme.primary,
                        bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                }
                MenuDivider()
                SwitchRow("休息结束震动提醒", vibrate) { v -> scope.launch { app.settings.setVibrate(v) } }
                MenuDivider()
                SwitchRow("休息结束提示音", sound) { v -> scope.launch { app.settings.setSound(v) } }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("重量单位", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TagChip("kg")
                }
                MenuDivider()
                Row(
                    Modifier.fillMaxWidth().clickable { toast(ctx, "在训练库长按自定义动作即可管理") }.padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("管理自定义动作", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TagChip("训练库", fg = MaterialTheme.colorScheme.primary, bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                }
            }
        }
        Spacer(Modifier.height(26.dp))
    }

    if (restSheet) {
        ModalBottomSheet(onDismissRequest = { restSheet = false }) {
            Text("默认组间休息时间", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            REST_OPTIONS.forEach { v ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            restSheet = false
                            scope.launch { app.settings.setDefaultRest(v) }
                            toast(ctx, "默认休息 $v 秒")
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$v 秒", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (v == rest) {
                        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF22C55E),
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White
            )
        )
    }
}

@Composable
private fun MenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}
