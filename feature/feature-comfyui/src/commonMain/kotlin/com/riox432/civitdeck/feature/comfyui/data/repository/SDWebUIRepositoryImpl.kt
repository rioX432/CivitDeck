package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.webui.SDWebUIApi
import com.riox432.civitdeck.data.api.webui.SDWebUIImg2ImgRequest
import com.riox432.civitdeck.data.api.webui.SDWebUITxt2ImgRequest
import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.SDWebUIConnectionDao
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import com.riox432.civitdeck.domain.model.SDWebUIConnection
import com.riox432.civitdeck.domain.model.SDWebUIGenerationParams
import com.riox432.civitdeck.domain.model.SDWebUIGenerationProgress
import com.riox432.civitdeck.domain.repository.SDWebUIRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private const val PROGRESS_POLL_MS = 500L

class SDWebUIRepositoryImpl(
    private val dao: SDWebUIConnectionDao,
    private val api: SDWebUIApi,
) : SDWebUIRepository {

    override fun observeConnections(): Flow<List<SDWebUIConnection>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeActiveConnection(): Flow<SDWebUIConnection?> =
        dao.observeActive().map { it?.toDomain() }

    override suspend fun getActiveConnection(): SDWebUIConnection? =
        dao.getActive()?.toDomain()

    override suspend fun saveConnection(connection: SDWebUIConnection): Long {
        val entity = connection.toEntity()
        return if (connection.id == 0L) {
            val id = dao.insert(entity)
            if (dao.getActive() == null) dao.activate(id)
            id
        } else {
            dao.update(entity)
            connection.id
        }
    }

    override suspend fun deleteConnection(id: Long) = dao.deleteById(id)

    override suspend fun activateConnection(id: Long) {
        dao.deactivateAll()
        dao.activate(id)
    }

    override suspend fun testConnection(connection: SDWebUIConnection): Boolean {
        api.setBaseUrl(connection.hostname, connection.port)
        return try {
            api.getSamplers()
            true
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            false
        }
    }

    override suspend fun updateTestResult(id: Long, success: Boolean) {
        dao.updateTestResult(id, currentTimeMillis(), success)
    }

    override suspend fun fetchModels(): List<String> {
        ensureApiConfigured()
        return api.getModels().map { it.title }
    }

    override suspend fun fetchSamplers(): List<String> {
        ensureApiConfigured()
        return api.getSamplers().map { it.name }
    }

    override suspend fun fetchVaes(): List<String> {
        ensureApiConfigured()
        return api.getVaes().map { it.modelName }
    }

    override fun generateImage(params: SDWebUIGenerationParams): Flow<SDWebUIGenerationProgress> =
        flow {
            ensureApiConfigured()
            try {
                coroutineScope {
                    val genDeferred = async { callGenerationApi(params) }
                    while (!genDeferred.isCompleted) {
                        val progress = safeGetProgress()
                        emit(progress)
                        delay(PROGRESS_POLL_MS)
                    }
                    val result = genDeferred.await()
                    emit(SDWebUIGenerationProgress.Completed(result.images))
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                emit(SDWebUIGenerationProgress.Error(e.message ?: "Generation failed"))
            }
        }

    override suspend fun interruptGeneration() {
        ensureApiConfigured()
        api.interrupt()
    }

    private suspend fun safeGetProgress(): SDWebUIGenerationProgress.Generating {
        return try {
            val p = api.getProgress()
            SDWebUIGenerationProgress.Generating(
                step = p.state.samplingStep,
                totalSteps = p.state.samplingSteps,
                fraction = p.progress,
            )
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            SDWebUIGenerationProgress.Generating(0, 0, 0.0)
        }
    }

    private suspend fun callGenerationApi(params: SDWebUIGenerationParams) =
        if (params.isImg2Img) {
            api.img2img(
                SDWebUIImg2ImgRequest(
                    prompt = params.prompt,
                    negativePrompt = params.negativePrompt,
                    steps = params.steps,
                    cfgScale = params.cfgScale,
                    width = params.width,
                    height = params.height,
                    samplerName = params.samplerName,
                    seed = params.seed,
                    initImages = listOfNotNull(params.initImageBase64),
                    denoisingStrength = params.denoisingStrength,
                ),
            )
        } else {
            api.txt2img(
                SDWebUITxt2ImgRequest(
                    prompt = params.prompt,
                    negativePrompt = params.negativePrompt,
                    steps = params.steps,
                    cfgScale = params.cfgScale,
                    width = params.width,
                    height = params.height,
                    samplerName = params.samplerName,
                    seed = params.seed,
                ),
            )
        }

    private suspend fun ensureApiConfigured() {
        val active = dao.getActive() ?: error("No active SD WebUI connection")
        api.setBaseUrl(active.hostname, active.port)
    }

    private fun SDWebUIConnectionEntity.toDomain() = SDWebUIConnection(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        isActive = isActive,
        lastTestedAt = lastTestedAt,
        lastTestSuccess = lastTestSuccess,
    )

    private fun SDWebUIConnection.toEntity() = SDWebUIConnectionEntity(
        id = id,
        name = name,
        hostname = hostname,
        port = port,
        isActive = isActive,
        lastTestedAt = lastTestedAt,
        lastTestSuccess = lastTestSuccess,
        createdAt = currentTimeMillis(),
    )
}
