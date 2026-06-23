package com.riox432.civitdeck.feature.collections.domain.usecase

import app.cash.turbine.test
import com.riox432.civitdeck.domain.model.ExportProgress
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
import kotlin.test.assertIs

class ExportWithPluginUseCaseTest {

    // --- fakes ---

    /**
     * A controllable ExportFormatPlugin whose export() return value is set per-test.
     */
    private class FakeExportPlugin(
        override val manifest: PluginManifest,
        override val state: PluginState = PluginState.ACTIVE,
        override val supportedFormats: List<PluginExportFormat>,
        private val exportFlow: Flow<PluginExportProgress> = flowOf(),
    ) : ExportFormatPlugin {

        var lastExportDatasetId: Long? = null
        var lastExportFormatId: String? = null
        var lastExportOptions: Map<String, String>? = null

        override fun export(
            datasetId: Long,
            formatId: String,
            options: Map<String, String>,
        ): Flow<PluginExportProgress> {
            lastExportDatasetId = datasetId
            lastExportFormatId = formatId
            lastExportOptions = options
            return exportFlow
        }

        override suspend fun initialize() {}
        override suspend fun activate() {}
        override suspend fun deactivate() {}
        override suspend fun destroy() {}
    }

    /**
     * A non-export plugin registered alongside export plugins to confirm the use case
     * ignores plugins of the wrong type during format discovery.
     */
    private class FakeWorkflowPlugin(id: String) : Plugin {
        override val manifest = PluginManifest(
            id = id,
            name = "Workflow $id",
            version = "1.0.0",
            author = "Test",
            description = "A workflow plugin",
            pluginType = PluginType.WORKFLOW_ENGINE,
            capabilities = emptyList(),
        )
        override val state: PluginState = PluginState.ACTIVE
        override suspend fun initialize() {}
        override suspend fun activate() {}
        override suspend fun deactivate() {}
        override suspend fun destroy() {}
    }

    // --- helpers ---

    private fun exportManifest(id: String) = PluginManifest(
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

    private fun buildUseCase(vararg plugins: Plugin): ExportWithPluginUseCase {
        val registry = InMemoryPluginRegistry()
        plugins.forEach { registry.register(it) }
        return ExportWithPluginUseCase(registry)
    }

    // --- tests ---

    @Test
    fun invoke_foundPlugin_emitsMappedPreparingProgress() = runTest {
        // Arrange: active plugin supporting "kohya-zip", emitting Preparing
        val plugin = FakeExportPlugin(
            manifest = exportManifest("kohya"),
            supportedFormats = listOf(format("kohya-zip")),
            exportFlow = flowOf(PluginExportProgress.Preparing),
        )
        val useCase = buildUseCase(plugin)

        // Act & Assert
        useCase.invoke(datasetId = 1L, formatId = "kohya-zip").test {
            assertIs<ExportProgress.Preparing>(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_foundPlugin_mapsDownloadingProgressWithCounts() = runTest {
        // Arrange: plugin emits a Downloading event
        val plugin = FakeExportPlugin(
            manifest = exportManifest("kohya"),
            supportedFormats = listOf(format("kohya-zip")),
            exportFlow = flowOf(PluginExportProgress.Downloading(current = 5, total = 20)),
        )
        val useCase = buildUseCase(plugin)

        // Act & Assert
        useCase.invoke(datasetId = 10L, formatId = "kohya-zip").test {
            val item = awaitItem()
            assertIs<ExportProgress.Downloading>(item)
            assertEquals(5, item.current)
            assertEquals(20, item.total)
            awaitComplete()
        }
    }

    @Test
    fun invoke_foundPlugin_mapsWritingManifestProgress() = runTest {
        // Arrange
        val plugin = FakeExportPlugin(
            manifest = exportManifest("p"),
            supportedFormats = listOf(format("jsonl")),
            exportFlow = flowOf(PluginExportProgress.WritingManifest),
        )
        val useCase = buildUseCase(plugin)

        // Act & Assert
        useCase.invoke(datasetId = 1L, formatId = "jsonl").test {
            assertIs<ExportProgress.WritingManifest>(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_foundPlugin_mapsCompletedProgressWithOutputPath() = runTest {
        // Arrange
        val plugin = FakeExportPlugin(
            manifest = exportManifest("p"),
            supportedFormats = listOf(format("kohya-zip")),
            exportFlow = flowOf(PluginExportProgress.Completed(outputPath = "/tmp/export.zip", warningCount = 3)),
        )
        val useCase = buildUseCase(plugin)

        // Act & Assert
        useCase.invoke(datasetId = 5L, formatId = "kohya-zip").test {
            val item = awaitItem()
            assertIs<ExportProgress.Completed>(item)
            assertEquals("/tmp/export.zip", item.outputPath)
            assertEquals(3, item.warningCount)
            awaitComplete()
        }
    }

    @Test
    fun invoke_foundPlugin_mapsFailedProgressWithMessage() = runTest {
        // Arrange
        val plugin = FakeExportPlugin(
            manifest = exportManifest("p"),
            supportedFormats = listOf(format("kohya-zip")),
            exportFlow = flowOf(PluginExportProgress.Failed("disk full")),
        )
        val useCase = buildUseCase(plugin)

        // Act & Assert
        useCase.invoke(datasetId = 1L, formatId = "kohya-zip").test {
            val item = awaitItem()
            assertIs<ExportProgress.Failed>(item)
            assertEquals("disk full", item.message)
            awaitComplete()
        }
    }

    @Test
    fun invoke_foundPlugin_emitsFullProgressSequence() = runTest {
        // Arrange: a realistic sequence emitted by a plugin
        val plugin = FakeExportPlugin(
            manifest = exportManifest("p"),
            supportedFormats = listOf(format("kohya-zip")),
            exportFlow = flowOf(
                PluginExportProgress.Preparing,
                PluginExportProgress.Downloading(current = 1, total = 3),
                PluginExportProgress.Downloading(current = 2, total = 3),
                PluginExportProgress.Downloading(current = 3, total = 3),
                PluginExportProgress.WritingManifest,
                PluginExportProgress.Completed(outputPath = "/out/archive.zip", warningCount = 0),
            ),
        )
        val useCase = buildUseCase(plugin)

        // Act & Assert
        useCase.invoke(datasetId = 99L, formatId = "kohya-zip").test {
            assertIs<ExportProgress.Preparing>(awaitItem())
            assertIs<ExportProgress.Downloading>(awaitItem())
            assertIs<ExportProgress.Downloading>(awaitItem())
            assertIs<ExportProgress.Downloading>(awaitItem())
            assertIs<ExportProgress.WritingManifest>(awaitItem())
            val last = awaitItem()
            assertIs<ExportProgress.Completed>(last)
            assertEquals("/out/archive.zip", last.outputPath)
            awaitComplete()
        }
    }

    @Test
    fun invoke_foundPlugin_forwardsDatasetIdAndOptionsToPlugin() = runTest {
        // Arrange
        val plugin = FakeExportPlugin(
            manifest = exportManifest("p"),
            supportedFormats = listOf(format("kohya-zip")),
            exportFlow = flowOf(PluginExportProgress.Preparing),
        )
        val useCase = buildUseCase(plugin)
        val options = mapOf("resolution" to "512", "caption_ext" to "txt")

        // Act
        useCase.invoke(datasetId = 42L, formatId = "kohya-zip", options = options).test {
            awaitItem()
            awaitComplete()
        }

        // Assert — the exact arguments were forwarded to the plugin
        assertEquals(42L, plugin.lastExportDatasetId)
        assertEquals("kohya-zip", plugin.lastExportFormatId)
        assertEquals(options, plugin.lastExportOptions)
    }

    @Test
    fun invoke_noMatchingPlugin_emitsSingleFailedEvent() = runTest {
        // Arrange: registry has no plugins at all
        val useCase = ExportWithPluginUseCase(InMemoryPluginRegistry())

        // Act & Assert
        useCase.invoke(datasetId = 1L, formatId = "unknown-format").test {
            val item = awaitItem()
            assertIs<ExportProgress.Failed>(item)
            assertEquals("No export plugin found for format: unknown-format", item.message)
            awaitComplete()
        }
    }

    @Test
    fun invoke_pluginExistsButFormatNotSupported_emitsFailed() = runTest {
        // Arrange: plugin supports "kohya-zip" but "jsonl" is requested
        val plugin = FakeExportPlugin(
            manifest = exportManifest("p"),
            supportedFormats = listOf(format("kohya-zip")),
        )
        val useCase = buildUseCase(plugin)

        // Act & Assert
        useCase.invoke(datasetId = 1L, formatId = "jsonl").test {
            assertIs<ExportProgress.Failed>(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_pluginInactiveForRequestedFormat_emitsFailed() = runTest {
        // Arrange: plugin supports the format but is INACTIVE — it must be ignored
        val inactivePlugin = FakeExportPlugin(
            manifest = exportManifest("inactive-plugin"),
            state = PluginState.INACTIVE,
            supportedFormats = listOf(format("kohya-zip")),
        )
        val useCase = buildUseCase(inactivePlugin)

        // Act & Assert
        useCase.invoke(datasetId = 1L, formatId = "kohya-zip").test {
            val item = awaitItem()
            assertIs<ExportProgress.Failed>(item)
            // error message references the format id
            assertEquals("No export plugin found for format: kohya-zip", item.message)
            awaitComplete()
        }
    }

    @Test
    fun invoke_nonExportPluginsIgnored_emitsFailed() = runTest {
        // Arrange: only a workflow plugin registered — it should never match export format lookup
        val workflowPlugin = FakeWorkflowPlugin("wf-only")
        val useCase = buildUseCase(workflowPlugin)

        // Act & Assert
        useCase.invoke(datasetId = 1L, formatId = "kohya-zip").test {
            assertIs<ExportProgress.Failed>(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun invoke_picksFirstActivePluginMatchingFormat_whenMultipleRegistered() = runTest {
        // Arrange: two plugins both support "kohya-zip"; only the ACTIVE one should be used
        val activePlugin = FakeExportPlugin(
            manifest = exportManifest("active-plugin"),
            state = PluginState.ACTIVE,
            supportedFormats = listOf(format("kohya-zip")),
            exportFlow = flowOf(PluginExportProgress.Preparing),
        )
        val inactivePlugin = FakeExportPlugin(
            manifest = exportManifest("inactive-plugin"),
            state = PluginState.INACTIVE,
            supportedFormats = listOf(format("kohya-zip")),
            exportFlow = flowOf(PluginExportProgress.Preparing),
        )
        val useCase = buildUseCase(activePlugin, inactivePlugin)

        // Act & Assert
        useCase.invoke(datasetId = 1L, formatId = "kohya-zip").test {
            assertIs<ExportProgress.Preparing>(awaitItem())
            awaitComplete()
        }

        // Only the active plugin was called
        assertEquals("kohya-zip", activePlugin.lastExportFormatId)
        assertEquals(null, inactivePlugin.lastExportFormatId, "Inactive plugin must not be called")
    }
}
