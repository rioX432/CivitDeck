package com.riox432.civitdeck.feature.comfyui.domain.model

/**
 * Represents a single editable parameter extracted from a ComfyUI workflow JSON.
 *
 * @param group Optional grouping key for APP mode parameters (null for legacy extraction).
 * @param order Sort order within group (0-based). APP mode preserves the original input order;
 *              legacy extraction uses priority-based ordering.
 */
data class ExtractedParameter(
    val nodeId: String,
    val nodeTitle: String,
    val nodeClassType: String,
    val paramName: String,
    val paramType: ParameterType,
    val currentValue: String,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val options: List<String> = emptyList(),
    val group: String? = null,
    val order: Int = 0,
)

/**
 * Type of a workflow parameter, determining which UI widget to render.
 */
enum class ParameterType {
    /** Free-form text input (e.g. prompts). */
    TEXT,

    /** Numeric input with optional range constraints. */
    NUMBER,

    /** Dropdown selection from a fixed list of options. */
    SELECT,

    /** Seed value with randomize button. */
    SEED,

    /** Image upload input (gallery picker or recent generations). */
    IMAGE,

    /** Boolean toggle switch. */
    BOOLEAN,
}
