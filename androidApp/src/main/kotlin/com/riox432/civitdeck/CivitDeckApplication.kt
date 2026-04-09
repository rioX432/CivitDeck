package com.riox432.civitdeck

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.riox432.civitdeck.di.initKoin
import com.riox432.civitdeck.di.initializeAuth
import com.riox432.civitdeck.di.registerExportPlugins
import com.riox432.civitdeck.di.registerThemePlugins
import com.riox432.civitdeck.di.registerWorkflowPlugins
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.repository.AppVersionProvider
import com.riox432.civitdeck.domain.usecase.CleanupBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.notification.ModelUpdateScheduler
import com.riox432.civitdeck.ui.dataset.DuplicateReviewViewModel
import com.riox432.civitdeck.widget.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

class CivitDeckApplication : Application(), SingletonImageLoader.Factory, KoinComponent {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val observeNotificationsEnabled: ObserveNotificationsEnabledUseCase by inject()
    private val observePollingInterval: ObservePollingIntervalUseCase by inject()
    private val cleanupBrowsingHistory: CleanupBrowsingHistoryUseCase by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CivitDeckApplication)
            modules(androidModule)
        }
        applicationScope.launch {
            registerWorkflowPlugins()
            registerExportPlugins()
            registerThemePlugins()
        }
        applicationScope.launch { initializeAuth() }
        applicationScope.launch { cleanupBrowsingHistory(System.currentTimeMillis()) }
        observeAndScheduleNotifications()
        scheduleWidgetRefresh()
    }

    private fun scheduleWidgetRefresh() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WidgetRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun observeAndScheduleNotifications() {
        applicationScope.launch {
            combine(
                observeNotificationsEnabled(),
                observePollingInterval(),
            ) { enabled, interval -> enabled to interval }
                .collectLatest { (enabled, interval) ->
                    if (enabled && interval != PollingInterval.Off) {
                        ModelUpdateScheduler.schedule(this@CivitDeckApplication, interval)
                    } else {
                        ModelUpdateScheduler.cancel(this@CivitDeckApplication)
                    }
                }
        }
    }

    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

val androidModule = module {
    single<AppVersionProvider> { AndroidAppVersionProvider() }
    viewModel { params ->
        ModelDetailViewModel(
            params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { params -> DuplicateReviewViewModel(params.get(), get(), get()) }
    // DownloadQueueViewModel now registered in shared Phase3ViewModelModule
}
