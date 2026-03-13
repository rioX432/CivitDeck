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
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.feature.collections.presentation.CollectionDetailViewModel
import com.riox432.civitdeck.feature.collections.presentation.CollectionsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSendViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIQueueViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ModelFileBrowserViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.WorkflowTemplateViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.feature.gallery.presentation.ImageGalleryViewModel
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.feature.search.presentation.SwipeDiscoveryViewModel
import com.riox432.civitdeck.notification.ModelUpdateScheduler
import com.riox432.civitdeck.ui.analytics.AnalyticsViewModel
import com.riox432.civitdeck.ui.backup.BackupViewModel
import com.riox432.civitdeck.ui.dataset.BatchTagEditorViewModel
import com.riox432.civitdeck.ui.dataset.DatasetDetailViewModel
import com.riox432.civitdeck.ui.dataset.DatasetListViewModel
import com.riox432.civitdeck.ui.dataset.DuplicateReviewViewModel
import com.riox432.civitdeck.ui.feed.FeedViewModel
import com.riox432.civitdeck.ui.plugin.PluginManagementViewModel
import com.riox432.civitdeck.ui.tutorial.GestureTutorialViewModel
import com.riox432.civitdeck.widget.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    private val observeNotificationsEnabled: ObserveNotificationsEnabledUseCase by inject()
    private val observePollingInterval: ObservePollingIntervalUseCase by inject()

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@CivitDeckApplication)
            modules(androidModule)
        }
        registerWorkflowPlugins()
        registerExportPlugins()
        CoroutineScope(Dispatchers.IO).launch { registerThemePlugins() }
        CoroutineScope(Dispatchers.IO).launch { initializeAuth() }
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
        CoroutineScope(Dispatchers.IO).launch {
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
    viewModel {
        ModelSearchViewModel(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(),
        )
    }
    viewModel { CollectionsViewModel(get(), get(), get(), get()) }
    viewModel { params -> CollectionDetailViewModel(params.get(), get(), get(), get(), get()) }
    viewModel { params ->
        ModelDetailViewModel(
            params.get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { params -> ImageGalleryViewModel(params.get(), get(), get(), get(), get(), get()) }
    viewModel { ModelFileBrowserViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { GestureTutorialViewModel(get(), get()) }
    viewModel { SwipeDiscoveryViewModel(get(), get()) }
    viewModel { ComfyUISettingsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { ComfyUIGenerationViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ComfyUIHistoryViewModel(get(), get(), get(), get(), get()) }
    viewModel { ComfyUIQueueViewModel(get(), get()) }
    viewModel { WorkflowTemplateViewModel(get(), get(), get(), get(), get()) }
    viewModel { SDWebUISettingsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SDWebUIGenerationViewModel(get(), get(), get(), get(), get()) }
    viewModel { CivitaiLinkSettingsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { CivitaiLinkSendViewModel(get(), get()) }
    viewModel { ExternalServerSettingsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { ExternalServerGalleryViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { DatasetListViewModel(get(), get(), get(), get()) }
    viewModel { params -> DatasetDetailViewModel(params.get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { params -> BatchTagEditorViewModel(params.get(), get(), get(), get()) }
    viewModel { params -> DuplicateReviewViewModel(params.get(), get(), get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { FeedViewModel(get(), get(), get()) }
    viewModel { BackupViewModel(get(), get(), get()) }
    viewModel { PluginManagementViewModel(get(), get(), get(), get(), get(), get()) }
}
