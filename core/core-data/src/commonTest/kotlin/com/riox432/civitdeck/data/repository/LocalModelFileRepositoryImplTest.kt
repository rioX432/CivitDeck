package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.local.dao.LocalModelFileDao
import com.riox432.civitdeck.data.local.entity.LocalModelFileEntity
import com.riox432.civitdeck.data.local.entity.ModelDirectoryEntity
import com.riox432.civitdeck.data.scanner.FileScanner
import com.riox432.civitdeck.data.scanner.ScannedFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [LocalModelFileRepositoryImpl] logic that is reachable without real
 * platform file IO: directory/file observation + DTO -> domain mapping (matched
 * and unmatched files), [LocalModelFileRepositoryImpl.scanDirectory] orchestration
 * via a fake [FileScanner], owned-hash exposure, and the happy path of
 * [LocalModelFileRepositoryImpl.verifyFileHash] against a mocked CivitAI API.
 *
 * Not covered: the verifyFileHash error branch, which on failure calls Logger.w ->
 * android.util.Log, unavailable on the JVM androidHostTest target.
 */
class LocalModelFileRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeLocalModelFileDao : LocalModelFileDao {
        val directories = mutableListOf<ModelDirectoryEntity>()
        val files = mutableListOf<LocalModelFileEntity>()
        private var dirId = 1L
        private var fileId = 1L
        private val updates = MutableStateFlow(0)

        override fun observeDirectories(): Flow<List<ModelDirectoryEntity>> =
            updates.map { directories.sortedBy { it.id } }

        override suspend fun getEnabledDirectories(): List<ModelDirectoryEntity> =
            directories.filter { it.isEnabled }

        override suspend fun insertDirectory(directory: ModelDirectoryEntity): Long {
            val withId = directory.copy(id = dirId++)
            directories.add(withId)
            updates.value++
            return withId.id
        }

        override suspend fun deleteDirectory(id: Long): Int {
            val removed = directories.count { it.id == id }
            directories.removeAll { it.id == id }
            updates.value++
            return removed
        }

        override suspend fun updateLastScannedAt(id: Long, scannedAt: Long): Int {
            val index = directories.indexOfFirst { it.id == id }
            if (index < 0) return 0
            directories[index] = directories[index].copy(lastScannedAt = scannedAt)
            updates.value++
            return 1
        }

        override fun observeAllFiles(): Flow<List<LocalModelFileEntity>> =
            updates.map { files.sortedBy { it.fileName } }

        override suspend fun insertFile(file: LocalModelFileEntity): Long {
            val withId = file.copy(id = fileId++)
            files.add(withId)
            updates.value++
            return withId.id
        }

        override suspend fun insertFiles(files: List<LocalModelFileEntity>) {
            files.forEach { insertFile(it) }
        }

        override suspend fun deleteFilesByDirectory(directoryId: Long): Int {
            val removed = files.count { it.directoryId == directoryId }
            files.removeAll { it.directoryId == directoryId }
            updates.value++
            return removed
        }

        override fun observeOwnedHashes(): Flow<List<String>> =
            updates.map { files.filter { it.matchedModelId != null }.map { it.sha256Hash } }

        override suspend fun getOwnedHashes(): List<String> =
            files.filter { it.matchedModelId != null }.map { it.sha256Hash }

        override suspend fun updateMatchInfo(
            fileId: Long,
            modelId: Long,
            modelName: String,
            versionId: Long,
            versionName: String,
            latestVersionId: Long?,
            hasUpdate: Boolean,
        ): Int {
            val index = files.indexOfFirst { it.id == fileId }
            if (index < 0) return 0
            files[index] = files[index].copy(
                matchedModelId = modelId,
                matchedModelName = modelName,
                matchedVersionId = versionId,
                matchedVersionName = versionName,
                latestVersionId = latestVersionId,
                hasUpdate = hasUpdate,
            )
            updates.value++
            return 1
        }

        override fun observeFileCount(): Flow<Int> = updates.map { files.size }

        override fun observeMatchedCount(): Flow<Int> =
            updates.map { files.count { it.matchedModelId != null } }

        override fun observeUpdatesAvailableCount(): Flow<Int> =
            updates.map { files.count { it.hasUpdate } }
    }

    private class FakeFileScanner(private val result: List<ScannedFile>) : FileScanner {
        var scannedPaths = mutableListOf<String>()
        override suspend fun scanDirectory(
            path: String,
            onProgress: (current: Int, total: Int) -> Unit,
        ): List<ScannedFile> {
            scannedPaths.add(path)
            return result
        }
    }

    /** A [CivitAiApi] that routes responses by request path. */
    private fun routingApi(byHash: String, model: String): CivitAiApi {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val body = when {
                path.contains("by-hash") -> byHash
                path.contains("/models/") -> model
                else -> "{}"
            }
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    private fun emptyApi(): CivitAiApi {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel("{}"),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    @Test
    fun observeDirectories_maps_entities_to_domain() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.directories.add(
            ModelDirectoryEntity(id = 1, path = "/models", label = "Main", isEnabled = true),
        )
        val repo = LocalModelFileRepositoryImpl(dao, emptyApi(), FakeFileScanner(emptyList()))

        val result = repo.observeDirectories().first()

        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
        assertEquals("/models", result.first().path)
        assertEquals("Main", result.first().label)
        assertTrue(result.first().isEnabled)
    }

    @Test
    fun addDirectory_inserts_and_returns_id() = runTest {
        val dao = FakeLocalModelFileDao()
        val repo = LocalModelFileRepositoryImpl(dao, emptyApi(), FakeFileScanner(emptyList()))

        val id = repo.addDirectory("/loras", "LoRAs")

        assertEquals(1, dao.directories.size)
        assertEquals(id, dao.directories.first().id)
        assertEquals("/loras", dao.directories.first().path)
    }

    @Test
    fun removeDirectory_deletes_files_then_directory() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.directories.add(ModelDirectoryEntity(id = 1, path = "/m"))
        dao.files.add(fileEntity(id = 1, directoryId = 1))
        dao.files.add(fileEntity(id = 2, directoryId = 2))
        val repo = LocalModelFileRepositoryImpl(dao, emptyApi(), FakeFileScanner(emptyList()))

        repo.removeDirectory(1)

        assertTrue(dao.directories.isEmpty())
        // Only files belonging to the removed directory are deleted.
        assertEquals(listOf(2L), dao.files.map { it.id })
    }

    @Test
    fun observeLocalFiles_maps_unmatched_file_with_null_matched_model() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.files.add(fileEntity(id = 1, directoryId = 1))
        val repo = LocalModelFileRepositoryImpl(dao, emptyApi(), FakeFileScanner(emptyList()))

        val file = repo.observeLocalFiles().first().first()

        assertEquals(1L, file.id)
        assertEquals("model.safetensors", file.fileName)
        assertNull(file.matchedModel)
    }

    @Test
    fun observeLocalFiles_maps_matched_file_to_matched_model_info() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.files.add(
            fileEntity(id = 1, directoryId = 1).copy(
                matchedModelId = 99,
                matchedModelName = "Cool Model",
                matchedVersionId = 5,
                matchedVersionName = "v2",
                latestVersionId = 6,
                hasUpdate = true,
            ),
        )
        val repo = LocalModelFileRepositoryImpl(dao, emptyApi(), FakeFileScanner(emptyList()))

        val matched = repo.observeLocalFiles().first().first().matchedModel

        assertEquals(99L, matched?.modelId)
        assertEquals("Cool Model", matched?.modelName)
        assertEquals(5L, matched?.versionId)
        assertEquals("v2", matched?.versionName)
        assertEquals(6L, matched?.latestVersionId)
        assertEquals(true, matched?.hasUpdate)
    }

    @Test
    fun scanDirectory_clears_old_files_inserts_scanned_and_updates_timestamp() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.directories.add(ModelDirectoryEntity(id = 1, path = "/m", isEnabled = true))
        dao.files.add(fileEntity(id = 1, directoryId = 1)) // stale entry, should be replaced
        val scanner = FakeFileScanner(
            listOf(
                ScannedFile("/m/a.safetensors", "a.safetensors", "hashA", 100L),
                ScannedFile("/m/b.ckpt", "b.ckpt", "hashB", 200L),
            ),
        )
        val repo = LocalModelFileRepositoryImpl(dao, emptyApi(), scanner)

        repo.scanDirectory(directoryId = 1) { _, _ -> }

        assertEquals(listOf("/m"), scanner.scannedPaths)
        assertEquals(setOf("hashA", "hashB"), dao.files.map { it.sha256Hash }.toSet())
        assertEquals(2, dao.files.size)
        assertTrue(dao.directories.first().lastScannedAt != null)
    }

    @Test
    fun getOwnedHashes_returns_only_matched_file_hashes() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.files.add(fileEntity(id = 1, directoryId = 1, hash = "unmatched"))
        dao.files.add(
            fileEntity(id = 2, directoryId = 1, hash = "matched").copy(matchedModelId = 7),
        )
        val repo = LocalModelFileRepositoryImpl(dao, emptyApi(), FakeFileScanner(emptyList()))

        assertEquals(setOf("matched"), repo.getOwnedHashes())
        assertEquals(setOf("matched"), repo.observeOwnedHashes().first())
    }

    @Test
    fun observeCounts_reflect_dao_state() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.files.add(fileEntity(id = 1, directoryId = 1))
        dao.files.add(fileEntity(id = 2, directoryId = 1).copy(matchedModelId = 1, hasUpdate = true))
        val repo = LocalModelFileRepositoryImpl(dao, emptyApi(), FakeFileScanner(emptyList()))

        assertEquals(2, repo.observeFileCount().first())
        assertEquals(1, repo.observeMatchedCount().first())
        assertEquals(1, repo.observeUpdatesAvailableCount().first())
    }

    @Test
    fun verifyFileHash_writes_match_info_with_update_flag() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.files.add(fileEntity(id = 1, directoryId = 1, hash = "abc123"))
        // by-hash returns version id 5 for model 100; the model's latest version is 6,
        // so hasUpdate must be true.
        val byHash = """{"id":5,"modelId":100,"name":"v1"}"""
        val model = """{"id":100,"name":"Cool Model","modelVersions":[{"id":6,"modelId":100}]}"""
        val repo = LocalModelFileRepositoryImpl(dao, routingApi(byHash, model), FakeFileScanner(emptyList()))

        repo.verifyFileHash(fileId = 1, sha256Hash = "abc123")

        val updated = dao.files.first()
        assertEquals(100L, updated.matchedModelId)
        assertEquals("Cool Model", updated.matchedModelName)
        assertEquals(5L, updated.matchedVersionId)
        assertEquals("v1", updated.matchedVersionName)
        assertEquals(6L, updated.latestVersionId)
        assertEquals(true, updated.hasUpdate)
    }

    @Test
    fun verifyFileHash_marks_no_update_when_installed_version_is_latest() = runTest {
        val dao = FakeLocalModelFileDao()
        dao.files.add(fileEntity(id = 1, directoryId = 1, hash = "abc123"))
        val byHash = """{"id":6,"modelId":100,"name":"v2"}"""
        val model = """{"id":100,"name":"Cool Model","modelVersions":[{"id":6,"modelId":100}]}"""
        val repo = LocalModelFileRepositoryImpl(dao, routingApi(byHash, model), FakeFileScanner(emptyList()))

        repo.verifyFileHash(fileId = 1, sha256Hash = "abc123")

        assertEquals(false, dao.files.first().hasUpdate)
        assertEquals(6L, dao.files.first().matchedVersionId)
    }

    private fun fileEntity(
        id: Long,
        directoryId: Long,
        hash: String = "hash$id",
    ) = LocalModelFileEntity(
        id = id,
        directoryId = directoryId,
        filePath = "/m/model.safetensors",
        fileName = "model.safetensors",
        sha256Hash = hash,
        sizeBytes = 1000L,
        scannedAt = 0L,
    )
}
