package com.fitlog.app.data.db

/**
 * 首启示例内容。动作库不再内置种子动作——全部 1318 个动作来自
 * assets/dataset/exercises.json（见 FitnessRepository.importDatasetFromAssets），
 * 这里只负责全新安装时的示例文件夹与示例计划，动作按名字关联数据集动作。
 */
object SeedData {
    data class DemoItem(
        val exName: String, val sets: Int,
        val weight: Float?, val reps: Int?, val timeSec: Int?, val restSec: Int
    )

    /** 示例计划引用的数据集动作名（merge_dataset.py 会校验这些名字存在） */
    fun demoPlanExerciseNames(): List<String> =
        demoPlans(0L, null).second.flatten().map { it.exName }.distinct()

    /** 首次启动的示例计划：返回 计划列表 + 每个计划的动作组（按计划下标对应） */
    fun demoPlans(now: Long, folderId: Long?): Pair<List<PlanEntity>, List<List<DemoItem>>> {
        val plans = listOf(
            PlanEntity(name = "推力日 · 胸肩", createdAt = now, lastDone = now - 2L * 86400000, folderId = folderId),
            PlanEntity(name = "拉力日 · 背臂", createdAt = now + 1, lastDone = now - 5L * 86400000, folderId = folderId),
            PlanEntity(name = "腿臀 + 核心", createdAt = now + 2, lastDone = now - 7L * 86400000, folderId = folderId)
        )
        val groups = listOf(
            listOf(
                DemoItem("杠铃卧推", 4, 60f, 8, null, 90),
                DemoItem("器械推胸", 3, 35f, 10, null, 60),
                DemoItem("哑铃侧平举", 4, 8f, 12, null, 45),
                DemoItem("绳索直立龙门架夹胸", 3, 15f, 12, null, 45),
                DemoItem("俯卧撑", 2, null, 15, null, 30)
            ),
            listOf(
                DemoItem("引体向上", 4, null, 8, null, 90),
                DemoItem("交替侧下拉", 4, 45f, 10, null, 60),
                DemoItem("绳索坐姿划船", 3, 40f, 10, null, 60),
                DemoItem("杠铃弯举", 3, 30f, 10, null, 45)
            ),
            listOf(
                DemoItem("杠铃全蹲", 5, 80f, 5, null, 120),
                DemoItem("雪橇机 45° 腿举", 4, 120f, 10, null, 90),
                DemoItem("杠铃臀桥", 4, 60f, 10, null, 60),
                DemoItem("登山跑", 3, null, null, 45, 30)
            )
        )
        return plans to groups
    }
}
