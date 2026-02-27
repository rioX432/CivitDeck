package com.riox432.civitdeck.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.riox432.civitdeck.domain.usecase.GetRecommendationsUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val getRecommendationsUseCase: GetRecommendationsUseCase by inject()

    override suspend fun doWork(): Result {
        return try {
            val sections = getRecommendationsUseCase()
            val trendingModel = sections
                .flatMap { it.models }
                .firstOrNull()

            if (trendingModel != null) {
                val version = trendingModel.modelVersions.firstOrNull()
                val thumbUrl = version?.images?.firstOrNull()?.url

                saveWidgetPrefs(
                    applicationContext,
                    WidgetData(
                        modelId = trendingModel.id,
                        modelName = trendingModel.name,
                        thumbnailUrl = thumbUrl,
                    ),
                )
                TrendingModelWidget().updateAll(applicationContext)
            }

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "widget_refresh"
    }
}
