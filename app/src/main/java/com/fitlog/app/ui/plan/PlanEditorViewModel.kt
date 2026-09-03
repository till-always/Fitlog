package com.fitlog.app.ui.plan

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.repo.FitnessRepository
import kotlinx.coroutines.launch

data class EditorItem(
    val exId: Long,
    val name: String,
    val part: String,
    val equip: String,
    val mode: String,
    val sets: Int,
    val weight: Float?,
    val reps: Int?,
    val timeSec: Int?,
    val restSec: Int
)

class PlanEditorViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app as FitLogApp
    val repo = ctx.repo

    var planId by mutableStateOf<Long?>(null); private set
    var name by mutableStateOf("")
    var folderId by mutableStateOf<Long?>(null)
    var items by mutableStateOf<List<EditorItem>>(emptyList())
    var ready by mutableStateOf(false); private set
    var defaultRest by mutableStateOf(60); private set

    private var loadStarted = false

    init {
        viewModelScope.launch { ctx.settings.defaultRest.collect { defaultRest = it } }
    }

    fun load(pid: Long?, defaultFolderArg: String? = null) {
        if (loadStarted) return
        loadStarted = true
        viewModelScope.launch {
            if (pid != null && pid > 0) {
                val p = repo.plan(pid)
                if (p != null) {
                    planId = p.plan.id
                    name = p.plan.name
                    folderId = p.plan.folderId
                    items = p.items.sortedBy { it.orderIdx }.mapNotNull { itp ->
                        val e = repo.exercise(itp.exerciseId) ?: return@mapNotNull null
                        EditorItem(e.id, e.name, e.part, e.equip, e.mode, itp.sets, itp.weight, itp.reps, itp.timeSec, itp.restSec)
                    }
                }
            } else {
                // 新建计划：继承主页当前选中的文件夹（"all"/"none" → 未分类）
                folderId = defaultFolderArg?.toLongOrNull()
            }
            ready = true
        }
    }

    fun addItem(exId: Long) {
        viewModelScope.launch {
            val e = repo.exercise(exId) ?: return@launch
            val item = when (e.mode) {
                "wr" -> EditorItem(e.id, e.name, e.part, e.equip, e.mode, 4, 20f, 10, null, defaultRest)
                "r" -> EditorItem(e.id, e.name, e.part, e.equip, e.mode, 4, null, 12, null, defaultRest)
                else -> EditorItem(e.id, e.name, e.part, e.equip, e.mode, 3, null, null, 45, defaultRest)
            }
            items = items + item
        }
    }

    fun removeAt(index: Int) {
        items = items.filterIndexed { idx, _ -> idx != index }
    }

    fun move(index: Int, delta: Int) {
        val j = index + delta
        if (j < 0 || j >= items.size) return
        val list = items.toMutableList()
        val tmp = list[index]; list[index] = list[j]; list[j] = tmp
        items = list
    }

    fun updateAt(index: Int, transform: (EditorItem) -> EditorItem) {
        items = items.mapIndexed { idx, it -> if (idx == index) transform(it) else it }
    }

    fun replaceAt(index: Int, item: EditorItem) {
        items = items.mapIndexed { idx, cur -> if (idx == index) item else cur }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.savePlan(
                name.trim(),
                items.map { FitnessRepository.NewPlanItem(it.exId, it.sets, it.weight, it.reps, it.timeSec, it.restSec) },
                planId,
                folderId
            )
            onDone()
        }
    }
}
