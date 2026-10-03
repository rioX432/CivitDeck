package com.riox432.civitdeck.ui.search

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.BaseModelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class DesktopBaseModelPickerTest {

    private val catalog = BaseModelCatalog(
        active = listOf("Krea 2", "Wan Video 2.2", "SDXL 1.0").map(::BaseModel),
        retired = listOf("SVD", "Wan Video").map(::BaseModel),
    )

    @Test
    fun searchMatchesLabelsCaseInsensitivelyInEverySection() {
        val sections = baseModelPickerSections(catalog, setOf(BaseModel("Wan Video")), query = " WAN ")

        assertEquals(listOf(BaseModel("Wan Video")), sections.selected)
        assertEquals(listOf(BaseModel("Wan Video 2.2")), sections.current)
        assertEquals(listOf(BaseModel("Wan Video")), sections.older)
    }

    @Test
    fun selectionFollowsCatalogOrderThenUnknownValuesByLabel() {
        val selection = setOf("Zeta", "SVD", "Alpha", "SDXL 1.0", "Krea 2").map(::BaseModel).toSet()

        assertEquals(
            listOf("Krea 2", "SDXL 1.0", "SVD", "Alpha", "Zeta").map(::BaseModel),
            orderedBaseModelSelection(selection, catalog),
        )
    }

    @Test
    fun chipLabelMarksRetiredAndUnknownValuesOnlyOnceTheCatalogIsLoaded() {
        assertEquals("Krea 2", baseModelChipLabel(BaseModel("Krea 2"), catalog))
        assertEquals("SVD (retired)", baseModelChipLabel(BaseModel("SVD"), catalog))
        assertEquals("Gone (unknown)", baseModelChipLabel(BaseModel("Gone"), catalog))
        assertEquals("Gone", baseModelChipLabel(BaseModel("Gone"), catalog = null))
    }

    @Test
    fun doneAppliesTheWholeSelectionOnce() = runComposeUiTest {
        val applied = mutableListOf<Set<BaseModel>>()
        setContent {
            DesktopBaseModelPicker(
                catalog = catalog,
                initialSelection = setOf(BaseModel("SDXL 1.0")),
                onApply = { applied += it },
                onDismiss = {},
            )
        }

        onNodeWithText("Krea 2").performClick()
        onAllNodesWithText("SDXL 1.0").onFirst().performClick()
        assertEquals(emptyList(), applied)

        onNodeWithText("Done").performClick()
        assertEquals(listOf(setOf(BaseModel("Krea 2"))), applied)
    }

    @Test
    fun olderIsCollapsedUntilASearchIsActive() = runComposeUiTest {
        setContent {
            DesktopBaseModelPicker(catalog = catalog, initialSelection = emptySet(), onApply = {}, onDismiss = {})
        }

        onNodeWithText("Older (2)").assertIsDisplayed()
        onNodeWithText("SVD").assertDoesNotExist()

        onNodeWithText("Search base models").performTextInput("svd")
        onNodeWithText("SVD").assertIsDisplayed()
    }

    @Test
    fun cancelDiscardsTheSelection() = runComposeUiTest {
        var applied: Set<BaseModel>? = null
        var dismissed = false
        setContent {
            DesktopBaseModelPicker(
                catalog = catalog,
                initialSelection = emptySet(),
                onApply = { applied = it },
                onDismiss = { dismissed = true },
            )
        }

        onNodeWithText("Krea 2").performClick()
        onNodeWithText("Cancel").performClick()

        assertNull(applied)
        assertEquals(true, dismissed)
    }
}
