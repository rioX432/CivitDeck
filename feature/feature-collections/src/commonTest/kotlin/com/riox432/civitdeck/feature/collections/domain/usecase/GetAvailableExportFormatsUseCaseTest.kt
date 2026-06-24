package com.riox432.civitdeck.feature.collections.domain.usecase

import app.cash.turbine.test
import com.riox432.civitdeck.plugin.ExportFormatPlugin
import com.riox432.civitdeck.plugin.InMemoryPluginRegistry
import com.riox432.civitdeck.plugin.Plugin
import com.riox432.civitdeck.plugin.PluginExportFormat
import com.riox432.civitdeck.plugin.PluginExportProgress
import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies [GetAvailableExportFormatsUseCase] flattens the supported formats of all ACTIVE
 * ExportFormatPlugins, skipping inactive plugins and plugins of other types.
 */
class GetAvailableExportFormatsUseCaseTest {

    private class FakeExportPlugin(
        override val manifest: PluginManifest,
        override val state: PluginState,
        override val supportedFormats: List<PluginExportFormat>,
    ) : ExportFormatPlugin {
        override fun export(
            datasetId: Long,
            formatId: String,
            options: Map<String, String>,
        ): Flow<PluginExportProgress> = flowOf()
        override suspend fun initialize() {}
        override suspend fun activate() {}
        override suspend fun deactivate() {}
        override suspend fun destroy() {}
    }

    @Test
    fun flattensFormatsOfAllActiveExportPlugins() = runTest {
        val a = FakeExportPlugin(
            manifest("a"), PluginState.ACTIVE,
            listOf(format("kohya-zip"), format("jsonl")),
        )
        val b = FakeExportPlugin(manifest("b"), PluginState.ACTIVE, listOf(format("csv")))
        val useCase = buildUseCase(a, b)

        useCase().test {
            val ids = awaitItem().map { it.id }
            assertEquals(3, ids.size)
            assertTrue(ids.containsAll(listOf("kohya-zip", "jsonl", "csv")))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun excludesFormatsFromInactivePlugins() = runTest {
        val active = FakeExportPlugin(manifest("a"), PluginState.ACTIVE, listOf(format("kohya-zip")))
        val inactive = FakeExportPlugin(
            manifest("b"), PluginState.INACTIVE, listOf(format("should-not-appear")),
        )
        val useCase = buildUseCase(active, inactive)

        useCase().test {
            val ids = awaitItem().map { it.id }
            assertEquals(listOf("kohya-zip"), ids)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emitsEmptyList_whenNoActiveExportPlugins() = runTest {
        val useCase = buildUseCase()

        useCase().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildUseCase(vararg plugins: Plugin): GetAvailableExportFormatsUseCase {
        val registry = InMemoryPluginRegistry()
        plugins.forEach { registry.register(it) }
        return GetAvailableExportFormatsUseCase(registry)
    }

    private fun manifest(id: String) = PluginManifest(
        id = id,
        name = "Export $id",
        version = "1.0.0",
        author = "Test",
        description = "Test export plugin",
        pluginType = PluginType.EXPORT_FORMAT,
        capabilities = emptyList(),
    )

    private fun format(id: String) = PluginExportFormat(
        id = id,
        name = id,
        description = "Format $id",
        fileExtension = "zip",
        mimeType = "application/zip",
    )
}
