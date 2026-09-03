package com.fitlog.app.data.model

import androidx.compose.ui.graphics.Color

object Parts {
    val all = listOf("胸", "背", "肩", "手臂", "腿", "臀部", "腹部")
    private val shorts = mapOf("手臂" to "臂")
    fun short(p: String): String = shorts[p] ?: p
    fun color(p: String): Color = when (p) {
        "胸" -> Color(0xFFF97316)
        "背" -> Color(0xFF3B82F6)
        "肩" -> Color(0xFF8B5CF6)
        "手臂" -> Color(0xFFEC4899)
        "腿" -> Color(0xFF10B981)
        "臀部" -> Color(0xFFF59E0B)
        else -> Color(0xFF14B8A6) // 腹部
    }
}

object Equips {
    val all = listOf("徒手", "哑铃", "杠铃", "固定器械", "弹力带", "壶铃", "其他")
}

object Modes {
    const val WEIGHT_REPS = "wr"
    const val REPS = "r"
    const val TIME = "t"
    val all = listOf(WEIGHT_REPS to "重量 × 次数", REPS to "仅次数", TIME to "计时")
    fun label(m: String): String = when (m) {
        WEIGHT_REPS -> "重量 × 次数"
        REPS -> "仅次数"
        else -> "计时"
    }
}
