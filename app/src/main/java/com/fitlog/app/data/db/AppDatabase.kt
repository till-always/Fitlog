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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun folderDao(): FolderDao
    abstract fun planDao(): PlanDao
    abstract fun sessionDao(): SessionDao
    abstract fun photoDao(): PhotoDao

    companion object {
        /** v3 → v4：动作表新增 分步教学/缩略图/目标肌群 三列（保留用户数据） */
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN steps TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN image TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN targetMuscle TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v4 → v5：动作表新增 动图演示/次要肌群/英文名 三列（保留用户数据） */
        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN anim TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN secMuscles TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercises ADD COLUMN enName TEXT NOT NULL DEFAULT ''")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fitlog.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration()
                .build()
    }
}
