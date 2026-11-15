package com.riox432.civitdeck.data.api.civitailink

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Fixtures follow civitai shared-types.ts (0c1f308) and civitai-comfy-nodes link_protocol.py (b34090e).
class CivitaiLinkProtocolTest {

    private val sha256 = "3f1a9c0e5b7d2468ace013579bdf2468ace013579bdf2468ace013579bdf2468"
    private val createdAt = "2026-11-15T10:00:00.000Z"

    private fun encode(command: CivitaiLinkCommand): JsonObject =
        civitaiLinkJson.encodeToJsonElement(CivitaiLinkCommand.serializer(), command).jsonObject

    private fun parse(raw: String): JsonObject = civitaiLinkJson.parseToJsonElement(raw).jsonObject

    private fun decodeStatus(raw: String): CivitaiLinkCommandStatus =
        civitaiLinkJson.decodeFromString(CivitaiLinkCommandStatus.serializer(), raw)

    @Test
    fun resourcesAddEncodesOfficialFlatShapeWithExactHash() {
        val command = CivitaiLinkCommand.ResourcesAdd(
            id = "cmd-add-1",
            createdAt = createdAt,
            resource = CivitaiLinkCommandResource(
                type = "LORA",
                hash = sha256,
                name = "detail_tweaker.safetensors",
                modelName = "Detail Tweaker",
                modelVersionName = "v1.0",
                previewImage = "https://image.civitai.com/preview.jpeg",
                url = "https://civitai.com/api/download/models/62833",
            ),
        )

        val expected = parse(
            """
            {"type":"resources:add","id":"cmd-add-1","createdAt":"$createdAt",
             "resource":{"type":"LORA","hash":"$sha256","name":"detail_tweaker.safetensors",
               "modelName":"Detail Tweaker","modelVersionName":"v1.0",
               "previewImage":"https://image.civitai.com/preview.jpeg",
               "url":"https://civitai.com/api/download/models/62833"}}
            """.trimIndent(),
        )
        val encoded = encode(command)
        assertEquals(expected, encoded)
        assertFalse("command" in encoded)
        assertFalse("payload" in encoded)
    }

    @Test
    fun resourcesAddOmitsAbsentOptionalResourceFields() {
        val command = CivitaiLinkCommand.ResourcesAdd(
            id = "cmd-add-2",
            createdAt = createdAt,
            resource = CivitaiLinkCommandResource(
                type = "Checkpoint",
                hash = sha256,
                name = "model.safetensors",
                modelName = "Model",
                url = "https://civitai.com/api/download/models/1",
            ),
        )

        val resource = encode(command).getValue("resource").jsonObject
        assertFalse("modelVersionName" in resource)
        assertFalse("previewImage" in resource)
    }

    @Test
    fun activitiesCancelKeepsCommandIdAndActivityIdSeparate() {
        val command = CivitaiLinkCommand.ActivitiesCancel(
            id = "cmd-cancel-9",
            createdAt = createdAt,
            activityId = "cmd-add-1",
        )

        val expected = parse(
            """{"type":"activities:cancel","id":"cmd-cancel-9","createdAt":"$createdAt","activityId":"cmd-add-1"}""",
        )
        assertEquals(expected, encode(command))
        assertNotEquals(command.id, command.activityId)
    }

    @Test
    fun listCommandsEncodeOfficialTypes() {
        assertEquals(
            parse("""{"type":"resources:list","id":"cmd-l","createdAt":"$createdAt"}"""),
            encode(CivitaiLinkCommand.ResourcesList(id = "cmd-l", createdAt = createdAt)),
        )
        assertEquals(
            parse("""{"type":"activities:list","id":"cmd-a","createdAt":"$createdAt","quantity":10}"""),
            encode(CivitaiLinkCommand.ActivitiesList(id = "cmd-a", createdAt = createdAt, quantity = 10)),
        )
    }

    @Test
    fun iamEncodesClientType() {
        assertEquals(
            parse("""{"type":"client"}"""),
            civitaiLinkJson.encodeToJsonElement(CivitaiLinkIam.serializer(), CivitaiLinkIam()),
        )
    }

    @Test
    fun joinAckDecodesFailureAndSuccess() {
        val failure = civitaiLinkJson.decodeFromString(
            CivitaiLinkJoinAck.serializer(),
            """{"success":false,"msg":"Invalid key"}""",
        )
        assertFalse(failure.success)
        assertEquals("Invalid key", failure.msg)

        val success = civitaiLinkJson.decodeFromString(CivitaiLinkJoinAck.serializer(), """{"success":true}""")
        assertTrue(success.success)
        assertNull(success.msg)
    }

    @Test
    fun roomPresenceDecodesClientAndSdCounts() {
        val presence = civitaiLinkJson.decodeFromString(
            CivitaiLinkRoomPresence.serializer(),
            """{"client":1,"sd":2,"extra":true}""",
        )
        assertEquals(CivitaiLinkRoomPresence(client = 1, sd = 2), presence)
    }

    @Test
    fun canceledStatusDecodesWithUnknownFields() {
        val status = decodeStatus(
            """
            {"type":"resources:add","id":"cmd-add-1","groupId":"g1","createdAt":"$createdAt",
             "resource":{"type":"LORA","hash":"$sha256","name":"a.safetensors","modelName":"A",
               "url":"https://civitai.com/api/download/models/1"},
             "status":"canceled","updatedAt":"$createdAt","progress":42.5,"futureField":{"x":1}}
            """.trimIndent(),
        )
        assertEquals(CivitaiLinkCommandState.Canceled, status.state)
        assertTrue(status.state.isTerminal)
        assertEquals(42.5, status.progress)
        assertEquals(sha256, status.resource?.hash)
    }

    @Test
    fun errorStatusCarriesErrorMessage() {
        val status = decodeStatus(
            """{"type":"resources:add","id":"cmd-add-3","status":"error","error":"Resource hash is not a SHA256"}""",
        )
        assertEquals(CivitaiLinkCommandState.Error, status.state)
        assertEquals("Resource hash is not a SHA256", status.error)
    }

    @Test
    fun processingStatusDecodesDownloadProgressFields() {
        val status = decodeStatus(
            """
            {"type":"resources:add","id":"cmd-add-4","status":"processing","createdAt":"$createdAt",
             "progress":12.34,"speed":1048576.0,"remainingTime":30.5}
            """.trimIndent(),
        )
        assertEquals(CivitaiLinkCommandState.Processing, status.state)
        assertFalse(status.state.isTerminal)
        assertEquals(1048576.0, status.speed)
        assertEquals(30.5, status.remainingTime)
    }

    @Test
    fun activitiesListDecodesNestedActivitiesIncludingRemove() {
        val status = decodeStatus(
            """
            {"type":"activities:list","id":"cmd-a","status":"success","activities":[
              {"type":"resources:add","id":"cmd-add-1","status":"pending"},
              {"type":"resources:remove","id":"cmd-rm-1","status":"success",
               "resource":{"type":"LORA","hash":"$sha256","modelName":"A"}}
            ]}
            """.trimIndent(),
        )
        val activities = status.activities.orEmpty()
        assertEquals(2, activities.size)
        assertEquals(CivitaiLinkCommandState.Pending, activities[0].state)
        assertNull(activities[1].resource?.url)
    }

    @Test
    fun resourcesListStatusDecodesEntries() {
        val status = decodeStatus(
            """
            {"type":"resources:list","id":"cmd-l","status":"success","resources":[
              {"type":"LORA","hash":"$sha256","name":"a.safetensors","path":"loras/a.safetensors","hasPreview":""},
              {"type":"Checkpoint","hash":"$sha256","name":"b.safetensors","hasPreview":"","downloading":true}
            ]}
            """.trimIndent(),
        )
        val resources = status.resources.orEmpty()
        assertEquals(listOf("loras/a.safetensors", null), resources.map { it.path })
        assertEquals(true, resources[1].downloading)
    }

    @Test
    fun cancelStatusDecodesTopLevelActivityId() {
        val status = decodeStatus(
            """{"type":"activities:cancel","id":"cmd-cancel-9","activityId":"cmd-add-1","status":"success"}""",
        )
        assertEquals("cmd-add-1", status.activityId)
        assertEquals("cmd-cancel-9", status.id)
    }

    @Test
    fun unknownStatusIsNeitherSuccessNorTerminal() {
        val status = decodeStatus("""{"type":"resources:add","id":"cmd-x","status":"paused"}""")
        assertEquals(CivitaiLinkCommandState.Unknown, status.state)
        assertFalse(status.state.isTerminal)
    }

    @Test
    fun knownStatusesMapFromWireValues() {
        assertEquals(
            listOf(
                CivitaiLinkCommandState.Pending,
                CivitaiLinkCommandState.Processing,
                CivitaiLinkCommandState.Success,
                CivitaiLinkCommandState.Error,
                CivitaiLinkCommandState.Canceled,
            ),
            listOf("pending", "processing", "success", "error", "canceled").map(CivitaiLinkCommandState::fromWire),
        )
    }
}
