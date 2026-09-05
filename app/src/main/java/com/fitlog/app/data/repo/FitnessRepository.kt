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
import com.fitlog.app.data.model.Muscles
import kotlinx.coroutines.flow.Flow

class FitnessRepository(private val app: FitLogApp) {
    private val db = app.db
    private val exerciseDao = db.exerciseDao()
    private val folderDao = db.folderDao()
    private val planDao = db.planDao()
    private val sessionDao = db.sessionDao()
    private val photoDao = db.photoDao()

    /** 启动种子：全新安装时导入示例文件夹与示例计划（动作库完全来自数据集导入，需先于本方法执行） */
    suspend fun ensureSeed() = db.withTransaction {
        if (planDao.count() > 0) return@withTransaction
        val demoNames = SeedData.demoPlanExerciseNames()
        val idByName = exerciseDao
            .byNames(demoNames)
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

    // ---- 动作 ----
    fun exercises(): Flow<List<ExerciseEntity>> = exerciseDao.all()
    suspend fun exercise(id: Long): ExerciseEntity? = exerciseDao.byId(id)
    suspend fun addExercise(e: ExerciseEntity): Long = exerciseDao.insert(e)
    suspend fun updateExercise(e: ExerciseEntity) = exerciseDao.update(e)

    /**
     * 导入 assets/dataset/exercises.json 的扩展动作库（带中文分步教学、缩略图、动图演示与次要肌群）。
     * 按名字去重，可重复执行；已存在的动作（内置/自定义）不会被覆盖。
     * 对旧版本导入过的动作做一次性回填：anim 为空即视为待升级，补齐缺失的媒体/教学字段。
     */
    suspend fun importDatasetFromAssets(assetPath: String = "dataset/exercises.json") {
        val json = runCatching {
            app.assets.open(assetPath).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return
        val arr = org.json.JSONArray(json)
        if (arr.length() == 0) return

        class Rec(
            val name: String, val en: String, val part: String, val equip: String, val mode: String,
            val tip: String, val steps: String, val image: String, val anim: String,
            val target: String, val sec: String
        )
        val recs = HashMap<String, Rec>(arr.length() * 2)
        // 媒体文件可能不在包内（版权素材不随仓库分发）：仅当 assets 里真实存在时才写路径，
        // 缺失则留空 → 界面退回部位色徽章展示
        val haveImages = runCatching {
            app.assets.list("ex")?.toSet().orEmpty()
        }.getOrDefault(emptySet())
        val haveAnims = runCatching {
            app.assets.list("exg")?.toSet().orEmpty()
        }.getOrDefault(emptySet())

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.optString("n")
            if (name.isBlank() || recs.containsKey(name)) continue
            val id = o.optString("id", "0")
            recs[name] = Rec(
                name = name,
                en = o.optString("en"),
                part = o.optString("p", "其他"),
                equip = o.optString("e", "其他"),
                mode = o.optString("m", "wr"),
                tip = o.optString("t"),
                steps = o.optString("s"),
                image = if ("$id.jpg" in haveImages) "ex/$id.jpg" else "",
                anim = if ("$id.gif" in haveAnims) "exg/$id.gif" else "",
                target = Muscles.zh(o.optString("tg")),
                sec = o.optJSONArray("sec")?.let { sa ->
                    (0 until sa.length()).joinToString(",") { sa.getString(it) }
                } ?: ""
            )
        }

        val existing = exerciseDao.allNames().toHashSet()
        val userDeleted = app.settings.deletedDatasetExercises()
        val batch = recs.values
            .filter { it.name !in existing && it.name !in userDeleted }
            .map { r ->
            ExerciseEntity(
                name = r.name,
                enName = r.en,
                part = r.part,
                equip = r.equip,
                mode = r.mode,
                tip = r.tip,
                steps = r.steps,
                image = r.image,
                anim = r.anim,
                targetMuscle = r.target,
                secMuscles = r.sec
            )
        }
        // 分批插入，避免单条语句过大
        batch.chunked(200).forEach { exerciseDao.insertAll(it) }

        // 老安装升级 / 分类扩充：非自定义动作与数据集比对，刷新部位/器械/记录方式/媒体路径
        //（随数据集版本演进；媒体缺失时 r.anim/r.image 为空，陈旧路径一并清掉）
        val all = exerciseDao.allList()
        val stale = all
            .filter { !it.isCustom && recs.containsKey(it.name) }
            .filter { e ->
                val r = recs.getValue(e.name)
                e.anim != r.anim || e.image != r.image || e.part != r.part || e.equip != r.equip || e.mode != r.mode
            }
        if (stale.isNotEmpty()) db.withTransaction {
            stale.forEach { e ->
                val r = recs.getValue(e.name)
                exerciseDao.update(
                    e.copy(
                        part = r.part,
                        equip = r.equip,
                        mode = r.mode,
                        image = r.image,
                        anim = r.anim,
                        steps = e.steps.ifBlank { r.steps },
                        tip = e.tip.ifBlank { r.tip },
                        // 目标肌群一并归一化：老数据里可能残留未翻译的英文名（如 "pectorals"）
                        targetMuscle = if (e.targetMuscle.isBlank() || !Muscles.hasHan(e.targetMuscle)) r.target else e.targetMuscle,
                        secMuscles = e.secMuscles.ifBlank { r.sec },
                        enName = e.enName.ifBlank { r.en }
                    )
                )
            }
        }

        // 数据集更名/收敛后的陈旧行清理：非自定义、不在当前数据集且未被任何计划引用的动作删除
        val referenced = planDao.referencedExerciseIds().toHashSet()
        val orphans = all.filter { !it.isCustom && !recs.containsKey(it.name) && it.id !in referenced }
        if (orphans.isNotEmpty()) exerciseDao.deleteAll(orphans)
    }

    /** 动作是否被计划引用（被引用则不允许删除，避免悬空数据） */
    suspend fun exerciseInUse(exerciseId: Long): Boolean = planDao.countItemsOfExercise(exerciseId) > 0

    suspend fun deleteExercise(e: ExerciseEntity) {
        exerciseDao.delete(e)
        // 数据集动作被用户删除后不再随导入复活
        if (!e.isCustom) app.settings.addDeletedDatasetExercise(e.name)
    }

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
