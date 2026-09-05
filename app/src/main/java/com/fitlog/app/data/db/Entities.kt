package com.fitlog.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val enName: String = "",   // 数据集英文名（详情页副标题）
    val part: String,      // 胸/背/肩/手臂/腿/臀部/腹部/有氧
    val equip: String,     // 徒手/哑铃/杠铃/固定器械/弹力带/壶铃/其他
    val mode: String,      // wr=重量×次数 r=仅次数 t=计时
    val tip: String,
    val isCustom: Boolean = false,
    val steps: String = "",        // 分步教学（\n 分隔）
    val image: String = "",        // 资产缩略图路径（assets/ex/xxx.jpg → "ex/xxx.jpg"）
    val anim: String = "",         // 动图演示路径（assets/exg/xxx.gif → "exg/xxx.gif"）
    val targetMuscle: String = "", // 目标肌群（中文）
    val secMuscles: String = ""    // 次要肌群（英文原名，逗号分隔，展示时经 Muscles 翻译）
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

@Entity(tableName = "plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val lastDone: Long? = null,
    val folderId: Long? = null
)

@Entity(
    tableName = "plan_items",
    indices = [Index("planId"), Index("exerciseId")]
)
data class PlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val exerciseId: Long,
    val orderIdx: Int,
    val sets: Int,
    val weight: Float?,
    val reps: Int?,
    val timeSec: Int?,
    val restSec: Int
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planName: String,
    val date: String,          // yyyy-MM-dd
    val startTs: Long,
    val durSec: Int,
    val setsTotal: Int,
    val setsDone: Int,
    val volume: Float,
    val parts: String          // 逗号分隔
)

@Entity(
    tableName = "session_exercises",
    indices = [Index("sessionId")]
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long = 0,
    val orderIdx: Int = 0,
    val name: String,
    val part: String,
    val mode: String
)

@Entity(
    tableName = "session_sets",
    indices = [Index("sessionExId")]
)
data class SessionSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExId: Long = 0,
    val idx: Int = 0,
    val done: Boolean,
    val weight: Float?,
    val reps: Int?,
    val timeSec: Int?
)

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val createdAt: Long
)

data class PlanWithItems(
    @Embedded val plan: PlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId")
    val items: List<PlanItemEntity>
)

data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExId")
    val sets: List<SessionSetEntity>
)

data class SessionWithDetails(
    @Embedded val session: SessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>
)
