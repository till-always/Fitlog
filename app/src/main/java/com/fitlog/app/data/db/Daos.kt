package com.fitlog.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY isCustom ASC, id ASC")
    fun all(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun byId(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE name IN (:names)")
    suspend fun byNames(names: List<String>): List<ExerciseEntity>

    @Query("SELECT name FROM exercises")
    suspend fun allNames(): List<String>

    /** 导入升级用的一次性全量读取（Flow 版供 UI 订阅） */
    @Query("SELECT * FROM exercises")
    suspend fun allList(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(list: List<ExerciseEntity>)

    @Insert
    suspend fun insert(e: ExerciseEntity): Long

    @Update
    suspend fun update(e: ExerciseEntity)

    @Delete
    suspend fun delete(e: ExerciseEntity)

    @Delete
    suspend fun deleteAll(list: List<ExerciseEntity>)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    fun all(): Flow<List<FolderEntity>>

    @Insert
    suspend fun insert(f: FolderEntity): Long

    @Query("UPDATE plans SET folderId = NULL WHERE folderId = :id")
    suspend fun detachPlans(id: Long)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PlanDao {
    @Transaction
    @Query("SELECT * FROM plans ORDER BY createdAt ASC")
    fun plansWithItems(): Flow<List<PlanWithItems>>

    @Transaction
    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun withItems(id: Long): PlanWithItems?

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun byId(id: Long): PlanEntity?

    @Query("SELECT COUNT(*) FROM plans")
    suspend fun count(): Int

    @Insert
    suspend fun insertPlan(p: PlanEntity): Long

    @Query("UPDATE plans SET name = :name, folderId = :folderId WHERE id = :id")
    suspend fun renamePlan(id: Long, name: String, folderId: Long?)

    @Query("SELECT COUNT(*) FROM plan_items WHERE exerciseId = :exerciseId")
    suspend fun countItemsOfExercise(exerciseId: Long): Int

    @Query("SELECT DISTINCT exerciseId FROM plan_items")
    suspend fun referencedExerciseIds(): List<Long>

    @Insert
    suspend fun insertItems(items: List<PlanItemEntity>)

    @Query("DELETE FROM plan_items WHERE planId = :planId")
    suspend fun deleteItemsOf(planId: Long)

    @Query("SELECT MAX(orderIdx) FROM plan_items WHERE planId = :planId")
    suspend fun maxOrder(planId: Long): Int?

    @Insert
    suspend fun insertItem(item: PlanItemEntity)

    @Query("UPDATE plans SET lastDone = :ts WHERE id = :id")
    suspend fun touchLastDone(id: Long, ts: Long)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlan(id: Long)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(s: SessionEntity): Long

    @Insert
    suspend fun insertExercises(list: List<SessionExerciseEntity>): List<Long>

    @Insert
    suspend fun insertSets(list: List<SessionSetEntity>)

    @Query("SELECT * FROM sessions WHERE date = :date ORDER BY startTs DESC")
    fun byDate(date: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE date BETWEEN :from AND :to ORDER BY startTs ASC")
    fun between(from: String, to: String): Flow<List<SessionEntity>>

    @Query("SELECT DISTINCT date FROM sessions ORDER BY date ASC")
    suspend fun allDates(): List<String>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun detail(id: Long): SessionWithDetails?

    @Query("SELECT * FROM sessions ORDER BY date ASC")
    fun all(): Flow<List<SessionEntity>>
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY createdAt DESC")
    fun all(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun byId(id: Long): PhotoEntity?

    @Insert
    suspend fun insert(p: PhotoEntity): Long

    @Delete
    suspend fun delete(p: PhotoEntity)
}
