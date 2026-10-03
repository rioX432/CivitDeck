package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.di.coreDataModule
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.BaseModelStatus
import com.riox432.civitdeck.domain.repository.BaseModelCatalogRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BaseModelCatalogRepositoryImplTest {

    private suspend fun bundledCatalog() = BaseModelCatalogRepositoryImpl().observeCatalog().first()

    @Test
    fun bundled_catalog_matches_the_2026_10_03_enums_response() = runTest {
        val catalog = bundledCatalog()

        assertEquals(78, catalog.active.size)
        assertEquals(34, catalog.retired.size)
        assertEquals(BaseModel("Anima"), catalog.active.first())
        assertEquals(BaseModel("Sonilo"), catalog.active.last())
        assertEquals(BaseModel("Ideogram 4.5"), catalog.retired.first())
        assertEquals(BaseModel("Trellis.2"), catalog.retired.last())
        assertTrue(catalog.active.intersect(catalog.retired.toSet()).isEmpty())
    }

    @Test
    fun statusOf_classifies_active_retired_and_unknown_values() = runTest {
        val catalog = bundledCatalog()

        assertEquals(BaseModelStatus.Active, catalog.statusOf(BaseModel("Krea 2")))
        assertEquals(BaseModelStatus.Active, catalog.statusOf(BaseModel("SDXL 1.0")))
        assertEquals(BaseModelStatus.Retired, catalog.statusOf(BaseModel("SVD")))
        assertEquals(BaseModelStatus.Retired, catalog.statusOf(BaseModel("Wan Video")))
        assertEquals(BaseModelStatus.Unknown, catalog.statusOf(BaseModel("NotAModel")))
        // CivitAI matches base models case-sensitively, so a differently cased value is not in the catalog.
        assertEquals(BaseModelStatus.Unknown, catalog.statusOf(BaseModel("sdxl 1.0")))
    }

    @Test
    fun buildBaseModelCatalog_drops_values_containing_a_comma() {
        val catalog = buildBaseModelCatalog(
            activeValues = listOf("Krea 2", "Foo, Bar", "Anima"),
            allValues = listOf("Krea 2", "Foo, Bar", "Anima", "SVD", "Old,Model", "Wan Video"),
        )

        assertEquals(listOf("Krea 2", "Anima"), catalog.active.map { it.apiValue })
        assertEquals(listOf("SVD", "Wan Video"), catalog.retired.map { it.apiValue })
        assertEquals(BaseModelStatus.Unknown, catalog.statusOf(BaseModel("Foo, Bar")))
        assertEquals(BaseModelStatus.Unknown, catalog.statusOf(BaseModel("Old,Model")))
    }

    @Test
    fun buildBaseModelCatalog_keeps_an_active_value_missing_from_the_full_list_as_active() {
        val catalog = buildBaseModelCatalog(
            activeValues = listOf("New Model", "Krea 2"),
            allValues = listOf("Krea 2", "SVD"),
        )

        assertEquals(listOf("New Model", "Krea 2"), catalog.active.map { it.apiValue })
        assertEquals(listOf("SVD"), catalog.retired.map { it.apiValue })
    }

    @Test
    fun coreDataModule_provides_the_repository() {
        val koin = koinApplication { modules(coreDataModule) }.koin

        assertIs<BaseModelCatalogRepositoryImpl>(koin.get<BaseModelCatalogRepository>())
    }
}
