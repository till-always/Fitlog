package com.fitlog.app.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.fitlog.app.FitLogApp
import com.fitlog.app.R
import com.fitlog.app.data.db.PhotoEntity
import com.fitlog.app.data.prefs.UserProfile
import com.fitlog.app.ui.components.SectionHeader
import com.fitlog.app.ui.components.toast
import com.fitlog.app.ui.theme.Volt
import com.fitlog.app.ui.theme.VoltDeep
import com.fitlog.app.ui.theme.VoltInk
import com.fitlog.app.util.streakOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProfileScreen(nav: NavController) {
    val app = LocalContext.current.applicationContext as FitLogApp
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val profile by app.settings.profile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val dark by app.settings.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val sessions by app.repo.allSessions().collectAsStateWithLifecycle(initialValue = emptyList())
    val photos by app.repo.photos().collectAsStateWithLifecycle(initialValue = emptyList())

    val count = sessions.size
    val totalSec = sessions.sumOf { it.durSec.toLong() }
    val totalVol = sessions.sumOf { it.volume.toDouble() }
    val streak = streakOf(sessions.map { it.date })
    val level = (count / 6 + 1).coerceAtMost(9)

    var lightbox by remember { mutableStateOf<PhotoEntity?>(null) }

    // 系统相册多选（一次最多 9 张）
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(9)) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
                    uris.mapNotNull { uri ->
                        val f = File(dir, "ph_${System.currentTimeMillis()}_${uris.indexOf(uri)}.jpg")
                        runCatching {
                            ctx.contentResolver.openInputStream(uri)?.use { input ->
                                f.outputStream().use { input.copyTo(it) }
                            }
                            f.absolutePath
                        }.getOrNull()
                    }
                }
                saved.forEach { app.repo.addPhoto(it) }
                toast(ctx, "已添加 ${saved.size} 张照片")
            }
        }
    }

    fun launchPicker() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("我的", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Volt, VoltDeep))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profile.name.firstOrNull()?.toString() ?: "F",
                        fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = VoltInk
                    )
                }
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Lv.$level",
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(profile.signature, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            scope.launch { app.settings.setDarkMode(!dark) }
                            toast(ctx, if (!dark) "夜间模式已开启 🌙" else "日间模式已开启 ☀️")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_moon), "切换深浅色", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.size(8.dp))
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { nav.navigate("editProfile") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, "编辑资料", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat("$count", "累计训练", Modifier.weight(1f))
            MiniStat("%.1f".format(totalSec / 3600.0) + "h", "总时长", Modifier.weight(1f))
            MiniStat("%.1f".format(totalVol / 1000.0) + "t", "总容量", Modifier.weight(1f))
            MiniStat("$streak", "连续打卡", Modifier.weight(1f))
        }

        SectionHeader("照片墙", "＋ 添加") {
            launchPicker()
        }

        val rows = photos.chunked(3)
        rows.forEachIndexed { ri, rowPhotos ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowPhotos.forEach { ph ->
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(ctx)
                            .data(File(ph.path))
                            .crossfade(180)
                            .build(),
                        contentDescription = "照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .clickable { lightbox = ph }
                    )
                }
                if (ri == rows.size - 1) {
                    repeat(3 - rowPhotos.size) { rem ->
                        if (rem == 0) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .border(1.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
                                    .clickable {
                                        launchPicker()
                                    },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Add, "添加照片", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (photos.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .border(1.6.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(13.dp))
                    .clickable {
                        launchPicker()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("从相册选择照片", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        SectionHeader("更多")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                MenuRow("设置") { nav.navigate("settings") }
                MenuRow("导出训练数据") { toast(ctx, "演示版本：数据保存在本机，后续版本支持导出") }
                MenuRow("关于 FitLog") { toast(ctx, "FitLog v0.1 · 本地健身训练记录") }
            }
        }
        Spacer(Modifier.height(26.dp))
    }

    lightbox?.let { ph ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { lightbox = null }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xEE101218))
                    .clickable { lightbox = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(ph.path),
                    contentDescription = "大图",
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "删除",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 46.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable {
                            scope.launch {
                                withContext(Dispatchers.IO) { File(ph.path).delete() }
                                app.repo.deletePhoto(ph)
                                lightbox = null
                                toast(ctx, "已删除")
                            }
                        }
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun MenuRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}
