package com.riox432.civitdeck.plugin.usecase

import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.model.InstalledPluginType
import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.plugin.InMemoryPluginRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies [ImportThemeUseCase]: a valid JSON theme is parsed, persisted to the repository
 * (with the derived plugin id and THEME type), and registered in memory. Invalid JSON yields
 * a failed Result and persists nothing.
 */
class ImportThemeUseCaseTest {

    private class RecordingPluginRepository : PluginRepository {
        val installed = mutableListOf<InstalledPlugin>()
        override fun observeAll(): Flow<List<InstalledPlugin>> = flowOf(emptyList())
        override fun observeById(pluginId: String): Flow<InstalledPlugin?> = flowOf(null)
        override suspend fun getById(pluginId: String): InstalledPlugin? = null
        override suspend fun install(plugin: InstalledPlugin) {
            installed.add(plugin)
        }
        override suspend fun uninstall(pluginId: String) {}
        override suspend fun updateState(pluginId: String, state: InstalledPluginState) {}
        override suspend fun getConfig(pluginId: String): String = "{}"
        override suspend fun updateConfig(pluginId: String, configJson: String) {}
    }

    @Test
    fun importsValidTheme_persistsAndRegisters() = runTest {
        val repo = RecordingPluginRepository()
        val registry = InMemoryPluginRegistry()
        val useCase = ImportThemeUseCase(repo, registry)

        val result = useCase(validThemeJson(id = "midnight", name = "Midnight"))

        assertTrue(result.isSuccess)
        // Plugin id is namespaced by JsonThemePlugin: "theme.<definition.id>".
        assertEquals("theme.midnight", result.getOrNull())

        // Persisted with the resolved id, name and THEME type.
        val saved = repo.installed.single()
        assertEquals("theme.midnight", saved.id)
        assertEquals("Midnight", saved.name)
        assertEquals(InstalledPluginType.THEME, saved.pluginType)
        assertEquals(InstalledPluginState.INSTALLED, saved.state)
        // configJson preserves the original JSON for re-loading.
        assertTrue(saved.configJson.contains("midnight"))

        // Registered in the in-memory registry so it is immediately usable.
        assertTrue(registry.getPlugin("theme.midnight") != null)
    }

    @Test
    fun invalidJson_returnsFailureAndPersistsNothing() = runTest {
        val repo = RecordingPluginRepository()
        val registry = InMemoryPluginRegistry()
        val useCase = ImportThemeUseCase(repo, registry)

        val result = useCase("{ not a theme }")

        assertTrue(result.isFailure)
        assertTrue(repo.installed.isEmpty())
    }

    @Test
    fun missingRequiredColorFields_returnsFailure() = runTest {
        val repo = RecordingPluginRepository()
        val useCase = ImportThemeUseCase(repo, InMemoryPluginRegistry())

        // Has id/name but the "light"/"dark" color blocks are absent.
        val result = useCase("""{"id":"x","name":"X"}""")

        assertTrue(result.isFailure)
        assertTrue(repo.installed.isEmpty())
    }

    private fun validThemeJson(id: String, name: String): String {
        val colors = """
            {
              "primary": "#FF000001", "onPrimary": "#FF000002",
              "primaryContainer": "#FF000003", "onPrimaryContainer": "#FF000004",
              "secondary": "#FF000005", "onSecondary": "#FF000006",
              "secondaryContainer": "#FF000007", "onSecondaryContainer": "#FF000008",
              "tertiary": "#FF000009", "onTertiary": "#FF00000A",
              "tertiaryContainer": "#FF00000B", "onTertiaryContainer": "#FF00000C",
              "background": "#FF00000D", "onBackground": "#FF00000E",
              "surface": "#FF00000F", "onSurface": "#FF000010",
              "surfaceVariant": "#FF000011", "onSurfaceVariant": "#FF000012",
              "error": "#FF000013", "onError": "#FF000014",
              "errorContainer": "#FF000015", "onErrorContainer": "#FF000016",
              "outline": "#FF000017", "outlineVariant": "#FF000018"
            }
        """.trimIndent()
        return """
            {
              "id": "$id",
              "name": "$name",
              "author": "Tester",
              "version": "1.0",
              "light": $colors,
              "dark": $colors
            }
        """.trimIndent()
    }
}
