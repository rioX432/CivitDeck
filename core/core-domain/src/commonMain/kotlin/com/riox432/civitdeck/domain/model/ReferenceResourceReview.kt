package com.riox432.civitdeck.domain.model

/**
 * What a prepared reference generation will meet on its own target, reviewed before submission.
 *
 * [capability] decides whether the graph can be confirmed at all. Resource identity and setting
 * differences never block; they only ask the user to accept a variation.
 */
data class ReferenceResourceReview(
    val target: ComfyUIJobTarget,
    val capability: TargetCapability,
    val resources: List<ResourceReview>,
    val settings: List<SettingComparison>,
) {
    val canConfirm: Boolean get() = capability is TargetCapability.Supported

    val requiresVariationAcknowledgement: Boolean
        get() = resources.any { it.confidence != ResourceConfidence.VERIFIED } ||
            settings.any { it.kind != ComparisonKind.MATCH }
}

/** Whether the target accepts every node and choice of the prepared graph. */
sealed interface TargetCapability {
    data object Supported : TargetCapability

    data class Blocked(val problems: List<CapabilityProblem>) : TargetCapability

    /** The target's object_info could not be fetched or read, so nothing was checked. */
    data class Unavailable(val reason: String) : TargetCapability
}

sealed interface CapabilityProblem {
    data class MissingNode(val classType: String) : CapabilityProblem

    /** [value] is not among the choices the target offers for [classType].[input]. */
    data class UnsupportedChoice(val classType: String, val input: String, val value: String) : CapabilityProblem

    data class UnknownInput(val classType: String, val input: String) : CapabilityProblem
}

enum class ResourceConfidence {
    /** A SHA-256 recorded for this exact target file matches the recorded version's file. */
    VERIFIED,

    /** The target has a file with the version's file name; its content is unconfirmed. */
    FILENAME_ONLY,

    /** The target has no file named like the version's files, or its hash differs. */
    MISSING,

    /** The version or its file names could not be resolved. */
    UNKNOWN,
}

/** One recorded source resource version, as found on the target. */
data class ResourceReview(
    val versionId: Long,
    val type: String?,
    val confidence: ResourceConfidence,
    val evidence: String,
    /** Target files whose base name matches a file of the version; equal names do not prove identity. */
    val targetFiles: List<String> = emptyList(),
    /** Whether the prepared graph loads one of [targetFiles]. */
    val selected: Boolean = false,
    /** The phone holds a file with the version's hash. Says nothing about the target. */
    val ownedLocally: Boolean = false,
)

enum class ReferenceSetting { PROMPT, MODEL, DIMENSIONS, STEPS, CFG, SAMPLER, SCHEDULER, SEED, LORA_WEIGHT }

enum class ComparisonKind {
    MATCH,
    VARIATION,

    /** The source did not record this value; the effective one is not a reproduction of it. */
    SOURCE_UNAVAILABLE,
}

data class SettingComparison(
    val setting: ReferenceSetting,
    val source: String?,
    /** Null when the prepared graph does not use the source's item at all. */
    val effective: String?,
    val kind: ComparisonKind,
    /** Which item the comparison is about when a setting repeats, such as a LoRA file. */
    val subject: String? = null,
)

/** A SHA-256 known for a file on one specific target. */
data class TargetFileHash(
    val target: ComfyUIJobTarget,
    val fileName: String,
    val sha256: String,
)
