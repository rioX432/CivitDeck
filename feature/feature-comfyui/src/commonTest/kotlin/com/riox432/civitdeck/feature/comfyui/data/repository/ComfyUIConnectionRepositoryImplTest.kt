package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [ComfyUIConnectionRepositoryImpl]'s DAO-backed CRUD (save/activate/delete),
 * entity<->domain mapping, and the network-backed [testConnection] happy/error paths.
 */
class ComfyUIConnectionRepositoryImplTest {

    private fun api(handler: () -> Boolean): ComfyUIApi {
        val client = mockClient {
            if (handler()) okJson("""{"queue_running":[],"queue_pending":[]}""")
            else respondError(HttpStatusCode.InternalServerError)
        }
        return ComfyUIApi(client, testJson)
    }

    @Test
    fun saveConnection_inserts_new_and_activates_first_connection() = runTest {
        val dao = FakeComfyUIConnectionDao()
        val repo = ComfyUIConnectionRepositoryImpl(dao, api { true })

        val id = repo.saveConnection(ComfyUIConnection(id = 0, name = "Home", hostname = "1.2.3.4"))

        assertEquals(1L, id)
        assertEquals(1, dao.rows.size)
        assertTrue(dao.rows.first().isActive) // first connection auto-activated
    }

    @Test
    fun saveConnection_second_insert_does_not_steal_active_flag() = runTest {
        val dao = FakeComfyUIConnectionDao()
        val repo = ComfyUIConnectionRepositoryImpl(dao, api { true })
        repo.saveConnection(ComfyUIConnection(id = 0, name = "A", hostname = "1.1.1.1"))

        repo.saveConnection(ComfyUIConnection(id = 0, name = "B", hostname = "2.2.2.2"))

        assertEquals(2, dao.rows.size)
        assertEquals(1, dao.rows.count { it.isActive }) // still only the first is active
    }

    @Test
    fun saveConnection_existing_id_updates_in_place() = runTest {
        val dao = FakeComfyUIConnectionDao()
        dao.rows.add(ComfyUIConnectionEntity(id = 7, name = "Old", hostname = "h", createdAt = 1))
        val repo = ComfyUIConnectionRepositoryImpl(dao, api { true })

        val id = repo.saveConnection(ComfyUIConnection(id = 7, name = "New", hostname = "h2"))

        assertEquals(7L, id)
        assertEquals(1, dao.rows.size)
        assertEquals("New", dao.rows.first().name)
    }

    @Test
    fun activateConnection_deactivates_others_then_activates_target() = runTest {
        val dao = FakeComfyUIConnectionDao()
        dao.rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", isActive = true, createdAt = 1))
        dao.rows.add(ComfyUIConnectionEntity(id = 2, name = "B", hostname = "h", createdAt = 2))
        val repo = ComfyUIConnectionRepositoryImpl(dao, api { true })

        repo.activateConnection(2)

        assertFalse(dao.rows.first { it.id == 1L }.isActive)
        assertTrue(dao.rows.first { it.id == 2L }.isActive)
    }

    @Test
    fun observeActiveConnection_maps_entity_to_domain() = runTest {
        val dao = FakeComfyUIConnectionDao()
        dao.rows.add(
            ComfyUIConnectionEntity(
                id = 3,
                name = "Active",
                hostname = "host",
                port = 9000,
                isActive = true,
                useHttps = true,
                createdAt = 1,
            ),
        )
        val repo = ComfyUIConnectionRepositoryImpl(dao, api { true })

        val active = repo.observeActiveConnection().first()

        assertEquals(3L, active?.id)
        assertEquals("https://host:9000", active?.baseUrl)
    }

    @Test
    fun deleteConnection_removes_row() = runTest {
        val dao = FakeComfyUIConnectionDao()
        dao.rows.add(ComfyUIConnectionEntity(id = 5, name = "X", hostname = "h", createdAt = 1))
        val repo = ComfyUIConnectionRepositoryImpl(dao, api { true })

        repo.deleteConnection(5)

        assertNull(dao.getById(5))
    }

    @Test
    fun testConnection_returns_true_when_queue_succeeds() = runTest {
        val repo = ComfyUIConnectionRepositoryImpl(FakeComfyUIConnectionDao(), api { true })

        val ok = repo.testConnection(ComfyUIConnection(name = "n", hostname = "h"))

        assertTrue(ok)
    }

    @Test
    fun testConnection_returns_false_when_queue_fails() = runTest {
        val repo = ComfyUIConnectionRepositoryImpl(FakeComfyUIConnectionDao(), api { false })

        val ok = repo.testConnection(ComfyUIConnection(name = "n", hostname = "h"))

        assertFalse(ok)
    }

    @Test
    fun updateTestResult_persists_success_flag() = runTest {
        val dao = FakeComfyUIConnectionDao()
        dao.rows.add(ComfyUIConnectionEntity(id = 1, name = "A", hostname = "h", createdAt = 1))
        val repo = ComfyUIConnectionRepositoryImpl(dao, api { true })

        repo.updateTestResult(1, success = true)

        assertEquals(true, dao.rows.first().lastTestSuccess)
        assertTrue(dao.rows.first().lastTestedAt != null)
    }
}
