package com.riox432.civitdeck.data.api.civitailink

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Wire contract of the official Civitai Link socket.io relay, mirroring civitai's
// src/components/CivitaiLink/shared-types.ts (commit 0c1f308) and the civitai-comfy-nodes
// link_protocol.py response shapes. It coexists with the legacy CivitaiLinkDto envelope until
// the transport migrates.

object CivitaiLinkEvents {
    const val SOCKET_PATH = "/api/socketio"

    const val IAM = "iam"
    const val JOIN = "join"
    const val LEAVE = "leave"
    const val COMMAND = "command"
    const val COMMAND_STATUS = "commandStatus"
    const val ROOM_PRESENCE = "roomPresence"
    const val UPGRADE_KEY = "upgradeKey"
    const val KICKED = "kicked"
    const val ERROR = "error"

    const val CLIENT_TYPE = "client"
}

// Commands are flat objects discriminated by `type`; encode through CivitaiLinkCommand.serializer()
// so the discriminator is written. `createdAt` is the ISO-8601 string a JS Date serializes to.
// encodeDefaults keeps non-null defaults such as iam's "client" on the wire; explicitNulls = false
// still omits absent optionals.
val civitaiLinkJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    classDiscriminator = "type"
}

@Serializable
data class CivitaiLinkIam(val type: String = CivitaiLinkEvents.CLIENT_TYPE)

@Serializable
data class CivitaiLinkJoinAck(
    val success: Boolean,
    val msg: String? = null,
)

@Serializable
data class CivitaiLinkRoomPresence(
    val client: Int,
    val sd: Int,
)

@Serializable
data class CivitaiLinkErrorMessage(val msg: String)

@Serializable
data class CivitaiLinkUpgradeKey(val key: String)

@Serializable
data class CivitaiLinkCommandResource(
    val type: String,
    val hash: String,
    val name: String,
    val modelName: String,
    val modelVersionName: String? = null,
    val previewImage: String? = null,
    val url: String,
)

@Serializable
sealed class CivitaiLinkCommand {
    abstract val id: String
    abstract val createdAt: String

    @Serializable
    @SerialName("resources:add")
    data class ResourcesAdd(
        override val id: String,
        override val createdAt: String,
        val resource: CivitaiLinkCommandResource,
    ) : CivitaiLinkCommand()

    @Serializable
    @SerialName("resources:list")
    data class ResourcesList(
        override val id: String,
        override val createdAt: String,
        val types: List<String>? = null,
    ) : CivitaiLinkCommand()

    @Serializable
    @SerialName("activities:list")
    data class ActivitiesList(
        override val id: String,
        override val createdAt: String,
        val quantity: Int? = null,
    ) : CivitaiLinkCommand()

    // `id` identifies this cancel command; `activityId` is the resources:add command being canceled.
    @Serializable
    @SerialName("activities:cancel")
    data class ActivitiesCancel(
        override val id: String,
        override val createdAt: String,
        val activityId: String,
    ) : CivitaiLinkCommand()
}

enum class CivitaiLinkCommandState(val wireValue: String?) {
    Pending("pending"),
    Processing("processing"),
    Success("success"),
    Error("error"),
    Canceled("canceled"),

    // A status this client does not know yet; never treated as finished or successful.
    Unknown(null),
    ;

    val isTerminal: Boolean get() = this == Success || this == Error || this == Canceled

    companion object {
        fun fromWire(value: String): CivitaiLinkCommandState =
            entries.firstOrNull { it.wireValue == value } ?: Unknown
    }
}

// Echoed resource in a status; lenient because resources:remove activities omit name and url.
@Serializable
data class CivitaiLinkStatusResource(
    val type: String,
    val hash: String,
    val name: String? = null,
    val modelName: String? = null,
    val modelVersionName: String? = null,
    val previewImage: String? = null,
    val url: String? = null,
)

@Serializable
data class CivitaiLinkResourceEntry(
    val type: String,
    val hash: String,
    val name: String,
    val path: String? = null,
    val hasPreview: String? = null,
    val downloading: Boolean? = null,
)

// One flat type for every `commandStatus` payload: `type` and `status` stay raw strings so a
// command or status added upstream decodes instead of failing the whole socket message.
@Serializable
data class CivitaiLinkCommandStatus(
    val id: String,
    val type: String,
    val status: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val progress: Double? = null,
    val error: String? = null,
    val resource: CivitaiLinkStatusResource? = null,
    val remainingTime: Double? = null,
    val speed: Double? = null,
    val activityId: String? = null,
    val resources: List<CivitaiLinkResourceEntry>? = null,
    val activities: List<CivitaiLinkCommandStatus>? = null,
) {
    val state: CivitaiLinkCommandState get() = CivitaiLinkCommandState.fromWire(status)
}
