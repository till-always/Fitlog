package com.fitlog.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExerciseEntity::class, FolderEntity::class, PlanEntity::class, PlanItemEntity::class,
        SessionEntity::class, SessionExerciseEntity::class, SessionSetEntity::class,
        PhotoEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun folderDao(): FolderDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
    abstract fun photoDao(): PhotoDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fitlog.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
