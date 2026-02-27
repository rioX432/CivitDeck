package com.riox432.civitdeck.domain.model

/**
 * Shared haptic feedback types for consistent tactile experience across platforms.
 *
 * - [Impact]: Medium impact for discrete actions (e.g., favorite toggle)
 * - [Selection]: Light tick for selection changes (e.g., filter chip toggle)
 * - [Success]: Success notification pattern (e.g., copy-to-clipboard)
 * - [Warning]: Warning notification pattern
 * - [Error]: Error notification pattern
 */
enum class HapticFeedbackType {
    Impact,
    Selection,
    Success,
    Warning,
    Error,
}
