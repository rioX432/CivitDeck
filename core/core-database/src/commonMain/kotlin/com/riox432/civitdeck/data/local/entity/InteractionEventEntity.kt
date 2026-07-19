package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only log of user interactions with a model (recommendation clicks, favorites,
 * downloads, shares). Unlike the single overwritable `interactionType` column on
 * [BrowsingHistoryEntity], every interaction is retained as its own row so signals
 * accumulate and are never dropped — including for models that have no browsing-history
 * row yet. Recommendation weighting derives engagement from this event log.
 */
@Entity(
    tableName = "interaction_event",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type", "timestamp"]),
    ],
)
data class InteractionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: Long,
    val type: String,
    val timestamp: Long,
)
