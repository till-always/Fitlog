package com.fitlog.app.data.repo

import androidx.room.withTransaction
import com.fitlog.app.FitLogApp
import com.fitlog.app.data.db.ExerciseEntity
import com.fitlog.app.data.db.FolderEntity
import com.fitlog.app.data.db.PlanEntity
import com.fitlog.app.data.db.PlanItemEntity
import com.fitlog.app.data.db.PlanWithItems
import com.fitlog.app.data.db.SeedData
import com.fitlog.app.data.db.SessionEntity
import com.fitlog.app.data.db.SessionExerciseEntity
import com.fitlog.app.data.db.SessionSetEntity
import com.fitlog.app.data.db.SessionWithDetails
import com.fitlog.app.data.db.PhotoEntity
import kotlinx.coroutines.flow.Flow

class FitnessRepository(private val app: FitLogApp) {
    private val db = app.db
    private val exerciseDao = db.exerciseDao()
    private val folderDao = db.folderDao()
    private val planDao = db.planDao()
    private val sessionDao = db.sessionDao()
    private val photoDao = db.photoDao()

    /** 启动种子：1) 内置动作库按名字增量导入（版本升级补新动作，不丢用户数据）
     *  2) 全新安装时导入示例文件夹与示例计划 */
    suspend fun ensureSeed() = db.withTransaction {
        val builtin = SeedData.exercises()
        val existingNames = exerciseDao.byNames(builtin.map { it.name }).map { it.name }.toSet()
        val missing = builtin.filter { it.name !in existingNames }
        if (missing.isNotEmpty()) exerciseDao.insertAll(missing)

        if (planDao.count() == 0) {
            val idByName = exerciseDao
                .byNames(builtin.map { it.name })
                .associateBy { it.name }
                .mapValues { it.value.id }
            val folderId = folderDao.insert(FolderEntity(name = "三分化", createdAt = System.currentTimeMillis()))
            val (plans, groups) = SeedData.demoPlans(System.currentTimeMillis(), folderId)
            val planIds = plans.map { planDao.insertPlan(it) }
            groups.forEachIndexed { pi, group ->
                planDao.insertItems(group.mapIndexed { ii, d ->
                    PlanItemEntity(
                        planId = planIds[pi], exerciseId = idByName[d.exName] ?: 0L, orderIdx = ii,
                        sets = d.sets, weight = d.weight, reps = d.reps, timeSec = d.timeSec, restSec = d.restSec
                    )
                })
            }
        }
    }

    // ---- 动作 ----
    fun exercises(): Flow<List<ExerciseEntity>> = exerciseDao.all()
    suspend fun exercise(id: Long): ExerciseEntity? = exerciseDao.byId(id)
    suspend fun addExercise(e: ExerciseEntity): Long = exerciseDao.insert(e)
    suspend fun updateExercise(e: ExerciseEntity) = exerciseDao.update(e)

    /** 动作是否被计划引用（被引用则不允许删除，避免悬空数据） */
    suspend fun exerciseInUse(exerciseId: Long): Boolean = planDao.countItemsOfExercise(exerciseId) > 0

    suspend fun deleteExercise(e: ExerciseEntity) = exerciseDao.delete(e)

    // ---- 文件夹 ----
    fun folders(): Flow<List<FolderEntity>> = folderDao.all()
    suspend fun addFolder(name: String): Long =
        folderDao.insert(FolderEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    /** 删除文件夹：其中的计划自动归入「未分类」，计划本身不删 */
    suspend fun deleteFolder(id: Long) = db.withTransaction {
        folderDao.detachPlans(id)
        folderDao.delete(id)
    }

    // ---- 计划 ----
    fun plans(): Flow<List<PlanWithItems>> = planDao.plansWithItems()
    suspend fun plan(id: Long): PlanWithItems? = planDao.withItems(id)

    /** 计划 + 动作明细一次取回（避免训练开始时 N+1 查询） */
    suspend fun planWithExercises(planId: Long): Pair<PlanWithItems, List<ExerciseEntity>>? {
        val p = planDao.withItems(planId) ?: return null
        val exs = exerciseDao.byIds(p.items.map { it.exerciseId }.distinct())
        return p to exs
    }

    suspend fun savePlan(
        name: String,
        items: List<NewPlanItem>,
        planId: Long? = null,
        folderId: Long? = null
    ) = db.withTransaction {
        val pid = if (planId != null) {
            planDao.renamePlan(planId, name, folderId)
            planId
        } else {
            planDao.insertPlan(PlanEntity(name = name, createdAt = System.currentTimeMillis(), folderId = folderId))
        }
        planDao.deleteItemsOf(pid)
        planDao.insertItems(items.mapIndexed { idx, it ->
            PlanItemEntity(
                planId = pid, exerciseId = it.exerciseId, orderIdx = idx,
                sets = it.sets, weight = it.weight, reps = it.reps, timeSec = it.timeSec, restSec = it.restSec
            )
        })
        pid
    }

    suspend fun deletePlan(planId: Long) = db.withTransaction {
        planDao.deleteItemsOf(planId)
        planDao.deletePlan(planId)
    }

    suspend fun touchPlan(planId: Long?, ts: Long = System.currentTimeMillis()) {
        if (planId != null) planDao.touchLastDone(planId, ts)
    }

    suspend fun appendPlanItem(planId: Long, item: NewPlanItem) {
        val order = (planDao.maxOrder(planId) ?: -1) + 1
        planDao.insertItem(
            PlanItemEntity(
                planId = planId, exerciseId = item.exerciseId, orderIdx = order,
                sets = item.sets, weight = item.weight, reps = item.reps,
                timeSec = item.timeSec, restSec = item.restSec
            )
        )
    }

    // ---- 训练记录（整段保存包事务，杜绝半条记录）----
    fun sessionsByDate(date: String): Flow<List<SessionEntity>> = sessionDao.byDate(date)
    fun sessionsBetween(from: String, to: String): Flow<List<SessionEntity>> = sessionDao.between(from, to)
    fun allSessions(): Flow<List<SessionEntity>> = sessionDao.all()
    suspend fun sessionDetail(id: Long): SessionWithDetails? = sessionDao.detail(id)

    suspend fun saveSession(
        session: SessionEntity,
        exercises: List<Pair<SessionExerciseEntity, List<SessionSetEntity>>>,
        planId: Long?
    ): Long = db.withTransaction {
        val sid = sessionDao.insertSession(session)
        exercises.forEach { (ex, sets) ->
            val exId = sessionDao.insertExercises(listOf(ex.copy(sessionId = sid)))[0]
            sessionDao.insertSets(sets.map { it.copy(sessionExId = exId) })
        }
        if (planId != null) planDao.touchLastDone(planId, System.currentTimeMillis())
        sid
    }

    // ---- 照片墙 ----
    fun photos(): Flow<List<PhotoEntity>> = photoDao.all()
    suspend fun addPhoto(path: String): Long = photoDao.insert(PhotoEntity(path = path, createdAt = System.currentTimeMillis()))
    suspend fun deletePhoto(p: PhotoEntity) = photoDao.delete(p)

    data class NewPlanItem(
        val exerciseId: Long, val sets: Int,
        val weight: Float?, val reps: Int?, val timeSec: Int?, val restSec: Int
    )

    companion object {
        fun defaultItem(exercise: ExerciseEntity, restSec: Int): NewPlanItem = when (exercise.mode) {
            "wr" -> NewPlanItem(exercise.id, 4, 20f, 10, null, restSec)
            "r" -> NewPlanItem(exercise.id, 4, null, 12, null, restSec)
            else -> NewPlanItem(exercise.id, 3, null, null, 45, restSec)
        }
    }
}
