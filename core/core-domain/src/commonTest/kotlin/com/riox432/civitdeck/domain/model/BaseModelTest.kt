package com.riox432.civitdeck.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BaseModelTest {

    @Test
    fun filterOptions_without_a_selection_is_the_default_options() {
        assertEquals(BaseModel.DEFAULT_OPTIONS, BaseModel.filterOptions(emptySet()))
    }

    @Test
    fun filterOptions_appends_selected_values_outside_the_defaults_once_in_sorted_order() {
        val selected = setOf(BaseModel("Pony"), BaseModel("Krea 2"), BaseModel("Anima"))

        val options = BaseModel.filterOptions(selected)

        assertEquals(BaseModel.DEFAULT_OPTIONS + listOf(BaseModel("Anima"), BaseModel("Krea 2")), options)
    }
}
