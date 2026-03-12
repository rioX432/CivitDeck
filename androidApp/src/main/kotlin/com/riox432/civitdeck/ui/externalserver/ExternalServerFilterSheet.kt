package com.riox432.civitdeck.ui.externalserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.ui.theme.Spacing

private val sortOptions = listOf("newest" to "Newest", "oldest" to "Oldest", "score" to "Score")
private val nsfwOptions = listOf("" to "All", "true" to "NSFW", "false" to "SFW")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalServerFilterSheet(
    filters: ExternalServerImageFilters,
    onFiltersChanged: (ExternalServerImageFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by rememberSaveable { mutableStateOf(filters.search) }
    var character by rememberSaveable { mutableStateOf(filters.character) }
    var scenario by rememberSaveable { mutableStateOf(filters.scenario) }
    var sort by rememberSaveable { mutableStateOf(filters.sort) }
    var nsfw by rememberSaveable { mutableStateOf(filters.nsfw) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            FilterSheetHeader(onReset = {
                search = ""
                character = ""
                scenario = ""
                sort = "newest"
                nsfw = ""
                onFiltersChanged(ExternalServerImageFilters())
                onDismiss()
            })
            FilterTextFields(search, character, scenario, { search = it }, { character = it }, { scenario = it })
            FilterChipSections(sort, nsfw, { sort = it }, { nsfw = it })
            FilterApplyButton {
                onFiltersChanged(
                    ExternalServerImageFilters(
                        search = search.trim(),
                        character = character.trim(),
                        scenario = scenario.trim(),
                        sort = sort,
                        nsfw = nsfw,
                    ),
                )
                onDismiss()
            }
        }
    }
}

@Composable
private fun FilterSheetHeader(onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Filters", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onReset) { Text("Reset") }
    }
}

@Composable
private fun FilterTextFields(
    search: String,
    character: String,
    scenario: String,
    onSearchChange: (String) -> Unit,
    onCharacterChange: (String) -> Unit,
    onScenarioChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = search,
        onValueChange = onSearchChange,
        label = { Text("Search") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = character,
        onValueChange = onCharacterChange,
        label = { Text("Character") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = scenario,
        onValueChange = onScenarioChange,
        label = { Text("Scenario") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FilterChipSections(
    sort: String,
    nsfw: String,
    onSortChange: (String) -> Unit,
    onNsfwChange: (String) -> Unit,
) {
    FilterSection(title = "Sort") {
        sortOptions.forEach { (value, label) ->
            FilterChip(selected = sort == value, onClick = { onSortChange(value) }, label = { Text(label) })
        }
    }
    FilterSection(title = "Content") {
        nsfwOptions.forEach { (value, label) ->
            FilterChip(selected = nsfw == value, onClick = { onNsfwChange(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun FilterApplyButton(onApply: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.lg),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(onClick = onApply) { Text("Apply") }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) { content() }
    }
}
