package com.fitlog.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun todayStr(): String = LocalDate.now().toString() // yyyy-MM-dd

fun monthRange(year: Int, month: Int): Pair<String, String> {
    val first = LocalDate.of(year, month, 1)
    val last = first.plusDays((first.lengthOfMonth() - 1).toLong())
    return first.toString() to last.toString()
}

fun weekdayCn(date: String): String {
    val d = LocalDate.parse(date)
    return listOf("一", "二", "三", "四", "五", "六", "日")[d.dayOfWeek.value - 1]
}

fun dateCn(date: String): String {
    val d = LocalDate.parse(date)
    return "${d.monthValue}月${d.dayOfMonth}日 · 星期${weekdayCn(date)}"
}

/** 连续打卡天数：从今天（或昨天）往前数连续有记录的天数 */
fun streakOf(dates: Collection<String>): Int {
    val set = dates.toSet()
    var d = LocalDate.now()
    if (d.toString() !in set) d = d.minusDays(1)
    var n = 0
    while (d.toString() in set) {
        n++
        d = d.minusDays(1)
    }
    return n
}

fun fmtDur(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(Locale.US, m, s)
}

fun fmtLong(sec: Long): String {
    val h = sec / 3600
    val m = Math.round((sec % 3600) / 60.0).toInt()
    return if (h > 0) "${h}小时${m}分" else "${m}分钟"
}

fun fmtVolume(v: Float): String =
    if (v % 1f == 0f) "%.0f".format(Locale.US, v) else "%.1f".format(Locale.US, v)

fun fmtClock(ts: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    return "%02d:%02d".format(Locale.US, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
}

fun planEstMinutes(sets: Int, totalRestSec: Int): Int = (sets * 45 + totalRestSec) / 60 + 8

/** 重量显示：整数不带小数，非整数保留 1 位 */
fun fmtWeight(v: Float?): String {
    val x = v ?: 0f
    return if (x % 1f == 0f) "%.0f".format(Locale.US, x) else "%.1f".format(Locale.US, x)
}
