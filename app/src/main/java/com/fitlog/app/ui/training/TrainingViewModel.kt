package com.fitlog.app.ui.training

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.db.SessionEntity
import com.fitlog.app.data.db.SessionExerciseEntity
import com.fitlog.app.data.db.SessionSetEntity
import com.fitlog.app.util.fmtClock
import com.fitlog.app.util.todayStr
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
data class TrainingRow(
    val done: Boolean = false,
    val weight: Float? = null,
    val reps: Int? = null,
    val timeSec: Int? = null
)

@Immutable
data class TrainingEx(
    val exId: Long,
    val name: String,
    val part: String,
    val equip: String,
    val mode: String,     // wr / r / t
    val restSec: Int,
    val rows: List<TrainingRow>,
    val customName: String? = null
) {
    val displayName: String get() = customName ?: name
}

@Immutable
data class WorkoutState(
    val planId: Long?,
    val planName: String,
    val startTs: Long,
    val items: List<TrainingEx>
)

@Immutable
data class RestState(val total: Int, val remain: Int)

@Immutable
data class TimedState(val exIdx: Int, val rowIdx: Int, val total: Int, val remain: Int)

@Immutable
data class SummaryRow(val done: Boolean, val weight: Float?, val reps: Int?, val timeSec: Int?)

@Immutable
data class SummaryItem(val name: String, val part: String, val mode: String, val rows: List<SummaryRow>)

@Immutable
data class SummaryState(
    val planId: Long?,
    val planName: String,
    val date: String,
    val time: String,
    val durSec: Int,
    val setsTotal: Int,
    val setsDone: Int,
    val volume: Float,
    val parts: List<String>,
    val items: List<SummaryItem>,
    val saved: Boolean = false
)

class TrainingViewModel(app: android.app.Application) : AndroidViewModel(app) {
    private val ctx = app as FitLogApp
    val repo = ctx.repo

    var workout by mutableStateOf<WorkoutState?>(null); private set
    var rest by mutableStateOf<RestState?>(null); private set
    var timed by mutableStateOf<TimedState?>(null); private set
    var summary by mutableStateOf<SummaryState?>(null); private set
    var elapsedSec by mutableStateOf(0L); private set
    var vibrateOn by mutableStateOf(true); private set
    var soundOn by mutableStateOf(true); private set
    var defaultRest by mutableStateOf(60); private set

    private var ticker: Job? = null
    private var restJob: Job? = null
    private var timedJob: Job? = null

    init {
        viewModelScope.launch { ctx.settings.vibrate.collect { vibrateOn = it } }
        viewModelScope.launch { ctx.settings.sound.collect { soundOn = it } }
        viewModelScope.launch { ctx.settings.defaultRest.collect { defaultRest = it } }
    }

    /** 每次进入训练页都无条件按所点计划重建（杜绝旧训练残留） */
    fun start(planId: Long?) {
        viewModelScope.launch {
            val items = mutableListOf<TrainingEx>()
            var planName = "空白训练"
            var pid: Long? = null
            if (planId != null && planId > 0) {
                val loaded = repo.planWithExercises(planId)
                if (loaded != null) {
                    val (p, exList) = loaded
                    pid = p.plan.id
                    planName = p.plan.name
                    p.items.sortedBy { it.orderIdx }.forEach { item ->
                        val e = exList.firstOrNull { it.id == item.exerciseId } ?: return@forEach
                        items += TrainingEx(
                            exId = e.id, name = e.name, part = e.part, equip = e.equip, mode = e.mode,
                            restSec = item.restSec,
                            rows = List(item.sets) {
                                TrainingRow(weight = item.weight, reps = item.reps, timeSec = item.timeSec)
                            }
                        )
                    }
                }
            }
            restJob?.cancel(); rest = null
            timedJob?.cancel(); timed = null
            summary = null
            workout = WorkoutState(pid, planName, System.currentTimeMillis(), items)
            elapsedSec = 0
            ticker?.cancel()
            ticker = viewModelScope.launch {
                while (workout != null) {
                    delay(500)
                    workout?.let { elapsedSec = (System.currentTimeMillis() - it.startTs) / 1000 }
                }
            }
        }
    }

    fun totals(): Triple<Int, Int, Float> {
        val w = workout ?: return Triple(0, 0, 0f)
        var t = 0; var d = 0; var v = 0f
        w.items.forEach { ex ->
            ex.rows.forEach { r ->
                t++
                if (r.done) {
                    d++
                    if (r.weight != null && r.reps != null) v += r.weight * r.reps
                }
            }
        }
        return Triple(t, d, v)
    }

    private fun updateItems(fn: (Int, TrainingEx) -> TrainingEx) {
        workout = workout?.let { w -> w.copy(items = w.items.mapIndexed { i, ex -> fn(i, ex) }) }
    }

    fun toggleRow(exIdx: Int, rowIdx: Int) {
        val w = workout ?: return
        val ex = w.items.getOrNull(exIdx) ?: return
        val row = ex.rows.getOrNull(rowIdx) ?: return
        if (timed != null && timed!!.exIdx == exIdx && timed!!.rowIdx == rowIdx) return
        if (ex.mode == "t" && !row.done) { startTimed(exIdx, rowIdx); return }
        setRowDone(exIdx, rowIdx, !row.done)
        if (!row.done) startRest(ex.restSec)
    }

    private fun setRowDone(exIdx: Int, rowIdx: Int, done: Boolean) {
        updateItems { i, ex ->
            if (i != exIdx) ex
            else ex.copy(rows = ex.rows.mapIndexed { j, r -> if (j == rowIdx) r.copy(done = done) else r })
        }
    }

    private fun startTimed(exIdx: Int, rowIdx: Int) {
        timedJob?.cancel()
        val ex = workout?.items?.getOrNull(exIdx) ?: return
        val total = ex.rows.getOrNull(rowIdx)?.timeSec ?: 30
        timed = TimedState(exIdx, rowIdx, total, total)
        timedJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val t = timed ?: break
                if (t.remain <= 1) {
                    timed = null
                    setRowDone(t.exIdx, t.rowIdx, true)
                    startRest(workout?.items?.getOrNull(t.exIdx)?.restSec ?: defaultRest)
                    vibrate()
                    break
                }
                timed = t.copy(remain = t.remain - 1)
            }
        }
    }

    fun stopTimedEarly() {
        val t = timed ?: return
        timedJob?.cancel()
        timed = null
        setRowDone(t.exIdx, t.rowIdx, true)
        startRest(workout?.items?.getOrNull(t.exIdx)?.restSec ?: defaultRest)
    }

    /** 取消这组计时（不打卡） */
    fun cancelTimed() {
        timedJob?.cancel()
        timed = null
    }

    fun addRow(exIdx: Int) {
        updateItems { i, ex ->
            if (i != exIdx) ex
            else ex.copy(rows = ex.rows + (ex.rows.lastOrNull()?.copy(done = false) ?: TrainingRow()))
        }
    }

    fun removeRow(exIdx: Int) {
        val ex = workout?.items?.getOrNull(exIdx) ?: return
        if (ex.rows.size <= 1) return
        if (timed?.exIdx == exIdx && timed?.rowIdx == ex.rows.size - 1) { timedJob?.cancel(); timed = null }
        updateItems { i, e -> if (i != exIdx) e else e.copy(rows = e.rows.dropLast(1)) }
    }

    fun removeExercise(exIdx: Int) {
        if (timed?.exIdx == exIdx) { timedJob?.cancel(); timed = null }
        workout = workout?.let { w -> w.copy(items = w.items.filterIndexed { idx, _ -> idx != exIdx }) }
    }

    fun updateRowValue(exIdx: Int, rowIdx: Int, weight: Float?, reps: Int?, timeSec: Int?) {
        updateItems { i, ex ->
            if (i != exIdx) ex
            else ex.copy(rows = ex.rows.mapIndexed { j, r ->
                if (j != rowIdx) r else r.copy(weight = weight, reps = reps, timeSec = timeSec)
            })
        }
    }

    fun updateTargets(exIdx: Int, name: String, w: Float?, r: Int?, t: Int?, restSec: Int) {
        updateItems { i, ex ->
            if (i != exIdx) ex
            else ex.copy(
                restSec = restSec,
                customName = name.ifBlank { null },
                rows = ex.rows.map { row -> if (row.done) row else row.copy(weight = w, reps = r, timeSec = t) }
            )
        }
    }

    /** 批量添加动作到本次训练（多选选择模式用） */
    fun addExercises(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val current = workout ?: return@launch
            val additions = ids.mapNotNull { id ->
                val e = repo.exercise(id) ?: return@mapNotNull null
                val rows = when (e.mode) {
                    "wr" -> List(4) { TrainingRow(weight = 20f, reps = 10) }
                    "r" -> List(4) { TrainingRow(reps = 12) }
                    else -> List(3) { TrainingRow(timeSec = 45) }
                }
                TrainingEx(
                    exId = e.id, name = e.name, part = e.part, equip = e.equip, mode = e.mode,
                    restSec = defaultRest, rows = rows
                )
            }
            if (additions.isNotEmpty()) {
                workout = current.copy(items = current.items + additions)
            }
        }
    }

    fun startRest(sec: Int) {
        restJob?.cancel()
        if (sec <= 0) return
        rest = RestState(sec, sec)
        restJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val r = rest ?: break
                if (r.remain <= 1) { rest = null; vibrate(); break }
                rest = r.copy(remain = r.remain - 1)
            }
        }
    }

    fun skipRest() { restJob?.cancel(); rest = null }

    fun addRest15() {
        val r = rest ?: return
        rest = r.copy(remain = r.remain + 15, total = maxOf(r.total, r.remain + 15))
    }

    /** 减 15 秒：减到 0 视同休息结束 */
    fun subRest15() {
        val r = rest ?: return
        val next = r.remain - 15
        if (next <= 0) {
            restJob?.cancel()
            rest = null
        } else {
            rest = r.copy(remain = next)
        }
    }

    private fun vibrate() {
        if (!vibrateOn) return
        val v = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(300)
        }
    }

    fun finish(): SummaryState? {
        val w = workout ?: return null
        val (t, d, v) = totals()
        val s = SummaryState(
            planId = w.planId,
            planName = w.planName,
            date = todayStr(),
            time = fmtClock(System.currentTimeMillis()),
            durSec = maxOf(60, ((System.currentTimeMillis() - w.startTs) / 1000).toInt()),
            setsTotal = t, setsDone = d, volume = v,
            parts = w.items.map { it.part }.distinct(),
            items = w.items.map { ex ->
                SummaryItem(ex.displayName, ex.part, ex.mode, ex.rows.map { SummaryRow(it.done, it.weight, it.reps, it.timeSec) })
            }
        )
        restJob?.cancel(); rest = null
        timedJob?.cancel(); timed = null
        ticker?.cancel()
        summary = s
        workout = null
        return s
    }

    fun discard() {
        restJob?.cancel(); rest = null
        timedJob?.cancel(); timed = null
        ticker?.cancel()
        workout = null
    }

    private var saving = false
    fun saveSummary(onSaved: (Long) -> Unit) {
        val s = summary ?: return
        if (s.saved || saving) return
        saving = true
        viewModelScope.launch {
            val sid = repo.saveSession(
                SessionEntity(
                    planName = s.planName, date = s.date,
                    startTs = System.currentTimeMillis() - s.durSec * 1000L,
                    durSec = s.durSec, setsTotal = s.setsTotal, setsDone = s.setsDone,
                    volume = s.volume, parts = s.parts.joinToString(",")
                ),
                s.items.mapIndexed { idx, itm ->
                    SessionExerciseEntity(name = itm.name, part = itm.part, mode = itm.mode, orderIdx = idx) to
                        itm.rows.map { r -> SessionSetEntity(done = r.done, weight = r.weight, reps = r.reps, timeSec = r.timeSec) }
                },
                s.planId
            )
            summary = summary?.copy(saved = true)
            saving = false
            onSaved(sid)
        }
    }
}
