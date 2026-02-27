package com.riox432.civitdeck.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import com.riox432.civitdeck.MainActivity

internal const val PREFS_NAME = "com.riox432.civitdeck.widget"
internal const val PREF_MODEL_ID = "widget_model_id"
internal const val PREF_MODEL_NAME = "widget_model_name"
internal const val PREF_MODEL_THUMB = "widget_model_thumb"

data class WidgetData(
    val modelId: Long?,
    val modelName: String?,
    val thumbnailUrl: String?,
)

internal fun loadWidgetPrefs(context: Context): WidgetData {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val id = prefs.getLong(PREF_MODEL_ID, -1L)
    return WidgetData(
        modelId = if (id != -1L) id else null,
        modelName = prefs.getString(PREF_MODEL_NAME, null),
        thumbnailUrl = prefs.getString(PREF_MODEL_THUMB, null),
    )
}

internal fun saveWidgetPrefs(context: Context, data: WidgetData) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
        data.modelId?.let { putLong(PREF_MODEL_ID, it) } ?: remove(PREF_MODEL_ID)
        data.modelName?.let { putString(PREF_MODEL_NAME, it) } ?: remove(PREF_MODEL_NAME)
        data.thumbnailUrl?.let { putString(PREF_MODEL_THUMB, it) } ?: remove(PREF_MODEL_THUMB)
    }.apply()
}

class TrendingModelWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetPrefs(context)
        provideContent {
            GlanceTheme {
                WidgetContent(data)
            }
        }
    }

    @Composable
    private fun WidgetContent(data: WidgetData) {
        val clickAction = actionStartActivity<MainActivity>()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .clickable(clickAction),
            contentAlignment = Alignment.Center,
        ) {
            if (data.modelName != null) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .background(GlanceTheme.colors.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "CivitDeck",
                            style = TextDefaults.defaultTextStyle,
                        )
                    }
                    Text(
                        text = data.modelName,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        style = TextDefaults.defaultTextStyle.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 2,
                    )
                }
            } else {
                Text(
                    text = "CivitDeck",
                    style = TextDefaults.defaultTextStyle,
                )
            }
        }
    }
}
