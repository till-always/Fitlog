package com.fitlog.app

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.fitlog.app.data.db.AppDatabase
import com.fitlog.app.data.prefs.SettingsStore
import com.fitlog.app.data.repo.FitnessRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FitLogApp : Application(), ImageLoaderFactory {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db by lazy { AppDatabase.build(this) }
    val settings by lazy { SettingsStore(this) }
    val repo by lazy { FitnessRepository(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // 动作库来自数据集导入，须先于示例计划种子执行（计划按名字关联动作）
            repo.importDatasetFromAssets()
            repo.ensureSeed()
        }
    }

    /** 全局图片加载器：注册 GIF 解码，动作库的动图资源才能逐帧播放 */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
}
