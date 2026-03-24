package com.riox432.civitdeck.ui.settings

import androidx.compose.runtime.Composable
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod

@Composable
internal fun SortOrderRow(selected: SortOrder, onChanged: (SortOrder) -> Unit) {
    DropdownSettingRow(
        label = "Default Sort",
        value = selected.name,
        options = SortOrder.entries.map { it.name },
        onSelected = { onChanged(SortOrder.valueOf(it)) },
    )
}

@Composable
internal fun TimePeriodRow(selected: TimePeriod, onChanged: (TimePeriod) -> Unit) {
    DropdownSettingRow(
        label = "Default Period",
        value = selected.name,
        options = TimePeriod.entries.map { it.name },
        onSelected = { onChanged(TimePeriod.valueOf(it)) },
    )
}
