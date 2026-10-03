package com.riox432.civitdeck.ui.search

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.BaseModelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals

class BaseModelPickerSectionsTest {

    private val catalog = BaseModelCatalog(
        active = listOf("SDXL 1.0", "Krea 2", "Wan Video 2.2 T2V-A14B", "Flux.1 D").map(::BaseModel),
        retired = listOf("SD 2.1", "Flux.1 Krea", "SVD").map(::BaseModel),
    )

    @Test
    fun emptyQueryListsWholeCatalogInApiOrder() {
        val sections = baseModelPickerSections(catalog, emptySet(), query = "")

        assertEquals(catalog.active, sections.current)
        assertEquals(catalog.retired, sections.older)
        assertEquals(emptyList(), sections.selected)
    }

    @Test
    fun queryMatchesCaseInsensitiveSubstringInBothSections() {
        val sections = baseModelPickerSections(catalog, emptySet(), query = "  krea ")

        assertEquals(listOf(BaseModel("Krea 2")), sections.current)
        assertEquals(listOf(BaseModel("Flux.1 Krea")), sections.older)
    }

    @Test
    fun selectedIgnoresQueryAndListsCatalogOrderThenUnknownByName() {
        val selection = setOf("Zeta Unknown", "SVD", "Alpha Unknown", "Flux.1 D", "SDXL 1.0").map(::BaseModel).toSet()

        val sections = baseModelPickerSections(catalog, selection, query = "krea")

        assertEquals(
            listOf("SDXL 1.0", "Flux.1 D", "SVD", "Alpha Unknown", "Zeta Unknown").map(::BaseModel),
            sections.selected,
        )
    }

    @Test
    fun missingCatalogStillListsSelectionByName() {
        val selection = setOf(BaseModel("SVD"), BaseModel("Krea 2"))

        val sections = baseModelPickerSections(catalog = null, selection = selection, query = "")

        assertEquals(listOf(BaseModel("Krea 2"), BaseModel("SVD")), sections.selected)
        assertEquals(emptyList(), sections.current)
        assertEquals(emptyList(), sections.older)
    }
}
