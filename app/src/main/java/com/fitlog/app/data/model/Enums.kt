package com.fitlog.app.data.model

import androidx.compose.ui.graphics.Color

object Parts {
    // 与数据集分类一致：手臂拆 前臂、腿拆 小腿，另含数据集的 颈
    val all = listOf("胸", "背", "肩", "手臂", "前臂", "腿", "小腿", "臀部", "腹部", "有氧", "颈")
    private val shorts = mapOf("手臂" to "臂", "有氧" to "氧")
    fun short(p: String): String = shorts[p] ?: p
    fun color(p: String): Color = when (p) {
        "胸" -> Color(0xFFF97316)
        "背" -> Color(0xFF3B82F6)
        "肩" -> Color(0xFF8B5CF6)
        "手臂" -> Color(0xFFEC4899)
        "前臂" -> Color(0xFFD946EF)
        "腿" -> Color(0xFF10B981)
        "小腿" -> Color(0xFF059669)
        "臀部" -> Color(0xFFF59E0B)
        "有氧" -> Color(0xFFEF4444)
        "颈" -> Color(0xFF64748B)
        else -> Color(0xFF14B8A6) // 腹部
    }
}

object Equips {
    // 数据集器械常见分类 + 自定义动作兜底的「其他」
    // （滚轮/负重/药球/健身球/战绳 已从动作库移除）
    val all = listOf(
        "徒手", "杠铃", "哑铃", "壶铃", "龙门架绳索", "史密斯机", "固定器械",
        "弹力带", "有氧器械", "其他"
    )
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

/** 数据集肌群英文名 → 中文（未收录的原样返回） */
object Muscles {
    private val zh = mapOf(
        "abductors" to "外展肌", "abs" to "腹肌", "abdominals" to "腹部肌群",
        "adductors" to "内收肌", "ankle stabilizers" to "踝稳定肌", "ankles" to "踝部",
        "back" to "背部", "biceps" to "肱二头肌", "brachialis" to "肱肌",
        "calves" to "小腿", "cardiovascular system" to "心肺", "chest" to "胸",
        "core" to "核心肌群", "deltoids" to "三角肌", "delts" to "三角肌",
        "feet" to "足部", "forearms" to "前臂", "glutes" to "臀肌",
        "grip muscles" to "握力肌群", "groin" to "腹股沟", "hamstrings" to "腘绳肌",
        "hands" to "手部", "hip flexors" to "屈髋肌", "inner thighs" to "大腿内侧",
        "latissimus dorsi" to "背阔肌", "lats" to "背阔肌", "levator scapulae" to "肩胛提肌",
        "lower abs" to "下腹部", "lower back" to "下背部", "obliques" to "腹斜肌",
        "pectorals" to "胸大肌", "quadriceps" to "股四头肌", "quads" to "股四头肌",
        "rear deltoids" to "三角肌后束", "rhomboids" to "菱形肌", "rotator cuff" to "肩袖肌群",
        "serratus anterior" to "前锯肌", "shins" to "胫骨前肌", "shoulders" to "肩",
        "soleus" to "比目鱼肌", "spine" to "脊柱", "sternocleidomastoid" to "胸锁乳突肌",
        "trapezius" to "斜方肌", "traps" to "斜方肌", "triceps" to "肱三头肌",
        "upper back" to "上背部", "upper chest" to "上胸",
        "wrist extensors" to "腕伸肌", "wrist flexors" to "腕屈肌", "wrists" to "腕部"
    )
    fun zh(name: String): String = zh[name.trim().lowercase()] ?: name.trim()

    /** 是否含汉字（识别数据集里未中文化的肌群字段，如 "pectorals"） */
    fun hasHan(s: String): Boolean = s.any { it in '\u4e00'..'\u9fff' }
}
