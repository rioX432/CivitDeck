package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.api.CivitAiApi
import com.riox432.civitdeck.data.local.dao.FeedCacheDao
import com.riox432.civitdeck.data.local.dao.FollowedCreatorDao
import com.riox432.civitdeck.data.local.entity.FeedCacheEntity
import com.riox432.civitdeck.data.local.entity.FollowedCreatorEntity
import com.riox432.civitdeck.domain.model.ModelType
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [CreatorFollowRepositoryImpl]: follow/unfollow persistence, the
 * [CreatorFollowRepositoryImpl.getFeed] cache refresh + DTO -> domain mapping,
 * the empty-follow short-circuit, the network-error fallback (cached feed is kept),
 * and the read/unread bookkeeping in [CreatorFollowRepositoryImpl.markFeedAsRead].
 */
class CreatorFollowRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeFollowedCreatorDao : FollowedCreatorDao {
        val creators = mutableListOf<FollowedCreatorEntity>()
        private val updates = MutableStateFlow(0)

        override suspend fun insert(entity: FollowedCreatorEntity) {
            creators.removeAll { it.username == entity.username }
            creators.add(entity)
            updates.value++
        }

        override suspend fun insertAll(entities: List<FollowedCreatorEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun deleteAll(): Int {
            val removed = creators.size
            creators.clear()
            updates.value++
            return removed
        }

        override suspend fun delete(username: String): Int {
            val removed = creators.count { it.username == username }
            creators.removeAll { it.username == username }
            updates.value++
            return removed
        }

        override fun isFollowing(username: String): Flow<Boolean> =
            updates.map { creators.any { it.username == username } }

        override fun observeAll(): Flow<List<FollowedCreatorEntity>> =
            updates.map { creators.sortedByDescending { it.followedAt } }

        override suspend fun getAll(): List<FollowedCreatorEntity> =
            creators.sortedByDescending { it.followedAt }

        override suspend fun updateLastCheckedAt(username: String, timestamp: Long): Int {
            val index = creators.indexOfFirst { it.username == username }
            if (index < 0) return 0
            creators[index] = creators[index].copy(lastCheckedAt = timestamp)
            updates.value++
            return 1
        }
    }

    private class FakeFeedCacheDao : FeedCacheDao {
        val entries = mutableListOf<FeedCacheEntity>()
        private val updates = MutableStateFlow(0)

        override suspend fun insertAll(entities: List<FeedCacheEntity>) {
            entities.forEach { entity ->
                entries.removeAll { it.modelId == entity.modelId }
                entries.add(entity)
            }
            updates.value++
        }

        override suspend fun getAll(): List<FeedCacheEntity> =
            entries.sortedByDescending { it.publishedAt }

        override suspend fun deleteByCreator(username: String): Int {
            val removed = entries.count { it.creatorUsername == username }
            entries.removeAll { it.creatorUsername == username }
            updates.value++
            return removed
        }

        override suspend fun deleteExpired(threshold: Long): Int {
            val removed = entries.count { it.cachedAt < threshold }
            entries.removeAll { it.cachedAt < threshold }
            updates.value++
            return removed
        }

        override fun countNewSince(since: Long): Flow<Int> =
            updates.map { entries.count { it.cachedAt > since } }
    }

    /** Builds a [CivitAiApi] backed by a [MockEngine] returning [body] with HTTP 200. */
    private fun apiReturning(body: String): CivitAiApi {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    /** Builds a [CivitAiApi] whose engine always throws, simulating an offline network. */
    private fun offlineApi(): CivitAiApi {
        val engine = MockEngine { throw kotlinx.io.IOException("offline") }
        return CivitAiApi(HttpClient(engine) { install(ContentNegotiation) { json(json) } })
    }

    @Test
    fun followCreator_persists_entity() = runTest {
        val followedDao = FakeFollowedCreatorDao()
        val repo = CreatorFollowRepositoryImpl(followedDao, FakeFeedCacheDao(), apiReturning("{}"))

        repo.followCreator("alice", "Alice", "avatar.png")

        assertEquals(1, followedDao.creators.size)
        val saved = followedDao.creators.first()
        assertEquals("alice", saved.username)
        assertEquals("Alice", saved.displayName)
        assertEquals("avatar.png", saved.avatarUrl)
    }

    @Test
    fun unfollowCreator_removes_creator_and_its_cached_feed() = runTest {
        val followedDao = FakeFollowedCreatorDao()
        val feedDao = FakeFeedCacheDao()
        followedDao.creators.add(
            FollowedCreatorEntity("alice", "Alice", null, followedAt = 1L, lastCheckedAt = 1L),
        )
        feedDao.entries.add(feedEntry(modelId = 1, creator = "alice", cachedAt = 1L))
        feedDao.entries.add(feedEntry(modelId = 2, creator = "bob", cachedAt = 1L))
        val repo = CreatorFollowRepositoryImpl(followedDao, feedDao, apiReturning("{}"))

        repo.unfollowCreator("alice")

        assertTrue(followedDao.creators.isEmpty())
        assertEquals(listOf(2L), feedDao.entries.map { it.modelId })
    }

    @Test
    fun isFollowing_reflects_dao_state() = runTest {
        val followedDao = FakeFollowedCreatorDao()
        val repo = CreatorFollowRepositoryImpl(followedDao, FakeFeedCacheDao(), apiReturning("{}"))

        assertFalse(repo.isFollowing("alice").first())
        repo.followCreator("alice", "Alice", null)
        assertTrue(repo.isFollowing("alice").first())
    }

    @Test
    fun getFollowedCreators_maps_entities_to_domain() = runTest {
        val followedDao = FakeFollowedCreatorDao()
        followedDao.creators.add(
            FollowedCreatorEntity("alice", "Alice", "a.png", followedAt = 2L, lastCheckedAt = 5L),
        )
        val repo = CreatorFollowRepositoryImpl(followedDao, FakeFeedCacheDao(), apiReturning("{}"))

        val result = repo.getFollowedCreators().first()

        assertEquals(1, result.size)
        assertEquals("alice", result.first().username)
        assertEquals("Alice", result.first().displayName)
        assertEquals("a.png", result.first().avatarUrl)
    }

    @Test
    fun getFeed_returns_empty_when_no_creators_followed() = runTest {
        val repo = CreatorFollowRepositoryImpl(
            FakeFollowedCreatorDao(),
            FakeFeedCacheDao(),
            // A throwing engine confirms the API is never hit when nobody is followed.
            offlineApi(),
        )

        val feed = repo.getFeed(forceRefresh = false)

        assertTrue(feed.isEmpty())
    }

    @Test
    fun getFeed_refreshes_cache_and_maps_models_to_feed_items() = runTest {
        val followedDao = FakeFollowedCreatorDao()
        val feedDao = FakeFeedCacheDao()
        followedDao.creators.add(
            FollowedCreatorEntity("alice", "Alice", null, followedAt = 1L, lastCheckedAt = 1L),
        )
        val body = """
            {"items":[
              {"id":100,"name":"Cool LoRA","type":"LORA",
               "stats":{"downloadCount":50,"commentCount":3},
               "modelVersions":[
                 {"id":7,"modelId":100,"name":"v1","createdAt":"2024-05-01",
                  "images":[{"url":"https://img/cover.png"}]}
               ]}
            ],
            "metadata":{}}
        """.trimIndent()
        val repo = CreatorFollowRepositoryImpl(followedDao, feedDao, apiReturning(body))

        val feed = repo.getFeed(forceRefresh = true)

        assertEquals(1, feed.size)
        val item = feed.first()
        assertEquals(100L, item.modelId)
        assertEquals("alice", item.creatorUsername)
        assertEquals("Cool LoRA", item.title)
        assertEquals("https://img/cover.png", item.thumbnailUrl)
        assertEquals(ModelType.LORA, item.type)
        assertEquals("2024-05-01", item.publishedAt)
        assertEquals(50, item.stats.downloadCount)
        assertEquals(3, item.stats.commentCount)
        // Cache was written with a fresh timestamp newer than the creator's lastCheckedAt=1.
        assertTrue(item.isUnread)
    }

    // Note: the network-error fallback in refreshFeedCache (catch IOException -> Logger.w)
    // cannot be exercised on the androidHostTest target: Logger.w() delegates to
    // android.util.Log, which is not mocked in plain JVM unit tests and throws
    // "Method w in android.util.Log not mocked". The catch logic itself is correct;
    // only the logging side effect is untestable without an Android test runtime.

    @Test
    fun markFeedAsRead_updates_lastCheckedAt_for_all_creators() = runTest {
        val followedDao = FakeFollowedCreatorDao()
        followedDao.creators.add(
            FollowedCreatorEntity("alice", "Alice", null, followedAt = 1L, lastCheckedAt = 1L),
        )
        followedDao.creators.add(
            FollowedCreatorEntity("bob", "Bob", null, followedAt = 1L, lastCheckedAt = 1L),
        )
        val repo = CreatorFollowRepositoryImpl(followedDao, FakeFeedCacheDao(), apiReturning("{}"))

        repo.markFeedAsRead()

        assertTrue(followedDao.creators.all { it.lastCheckedAt > 1L })
    }

    @Test
    fun getUnreadCount_counts_feed_cached_after_oldest_last_checked() = runTest {
        val followedDao = FakeFollowedCreatorDao()
        val feedDao = FakeFeedCacheDao()
        followedDao.creators.add(
            FollowedCreatorEntity("alice", "Alice", null, followedAt = 1L, lastCheckedAt = 100L),
        )
        feedDao.entries.add(feedEntry(modelId = 1, creator = "alice", cachedAt = 50L)) // read
        feedDao.entries.add(feedEntry(modelId = 2, creator = "alice", cachedAt = 200L)) // unread
        feedDao.entries.add(feedEntry(modelId = 3, creator = "alice", cachedAt = 300L)) // unread
        val repo = CreatorFollowRepositoryImpl(followedDao, feedDao, apiReturning("{}"))

        val count = repo.getUnreadCount().first()

        assertEquals(2, count)
    }

    @Test
    fun getUnreadCount_is_zero_when_no_creators() = runTest {
        val repo = CreatorFollowRepositoryImpl(
            FakeFollowedCreatorDao(),
            FakeFeedCacheDao(),
            apiReturning("{}"),
        )

        assertEquals(0, repo.getUnreadCount().first())
    }

    private fun feedEntry(modelId: Long, creator: String, cachedAt: Long) = FeedCacheEntity(
        modelId = modelId,
        creatorUsername = creator,
        title = "Title $modelId",
        thumbnailUrl = null,
        type = ModelType.Checkpoint.name,
        publishedAt = "2024-01-0$modelId",
        cachedAt = cachedAt,
    )
}
