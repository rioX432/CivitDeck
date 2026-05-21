package com.riox432.civitdeck.domain.model

/**
 * Represents ComfyUI APP mode metadata parsed from workflow JSON.
 *
 * APP mode (ComfyUI frontend v1.41.13+) allows workflows to define which node inputs
 * are user-facing and which node outputs are visible, stored in `extra.linearData`.
 *
 * See: https://docs.comfy.org/interface/app-mode
 */
data class AppModeMetadata(
    val inputs: List<AppModeInput>,
    val outputs: List<AppModeOutput>,
    val defaultView: AppModeView,
)

/**
 * A single user-facing input designated by APP mode.
 *
 * In the ComfyUI frontend, this is stored as a tuple: [nodeId, paramName, config?]
 * where config is an optional object with widget configuration like height.
 *
 * @param nodeId The workflow node ID containing this input.
 * @param paramName The input parameter name on the node.
 * @param label Optional display label (derived from node _meta.title if available).
 * @param group Optional grouping key for UI organization.
 * @param order Position in the input list (0-based, preserves original ordering).
 * @param widgetHeight Optional height hint for the input widget (from config.height).
 */
data class AppModeInput(
    val nodeId: String,
    val paramName: String,
    val label: String? = null,
    val group: String? = null,
    val order: Int = 0,
    val widgetHeight: Int? = null,
)

/**
 * A node output designated as visible in APP mode.
 *
 * @param nodeId The workflow node ID whose output is visible.
 * @param label Optional display label (derived from node _meta.title if available).
 */
data class AppModeOutput(
    val nodeId: String,
    val label: String? = null,
)

/**
 * Default view mode for a workflow with APP mode metadata.
 */
enum class AppModeView {
    /** Show the simplified APP mode interface. */
    APP,

    /** Show the traditional node graph editor. */
    GRAPH,
}
