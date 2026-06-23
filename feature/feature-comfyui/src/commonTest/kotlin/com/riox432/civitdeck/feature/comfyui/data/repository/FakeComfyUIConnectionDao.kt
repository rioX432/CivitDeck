package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.dao.ComfyUIConnectionDao
import com.riox432.civitdeck.data.local.entity.ComfyUIConnectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Hand-written in-memory fake for [ComfyUIConnectionDao] used by repository tests.
 * Mirrors the SQL semantics of the Room DAO (auto-increment id, single active row,
 * createdAt DESC ordering) closely enough for unit-test assertions.
 */
class FakeComfyUIConnectionDao : ComfyUIConnectionDao {
    val rows = mutableListOf<ComfyUIConnectionEntity>()
    private var idCounter = 1L
    private val updates = MutableStateFlow(0)

    private fun bump() { updates.value++ }

    override fun observeAll(): Flow<List<ComfyUIConnectionEntity>> =
        updates.map { rows.sortedByDescending { it.createdAt } }

    override fun observeActive(): Flow<ComfyUIConnectionEntity?> =
        updates.map { rows.firstOrNull { it.isActive } }

    override suspend fun getActive(): ComfyUIConnectionEntity? = rows.firstOrNull { it.isActive }

    override suspend fun getById(id: Long): ComfyUIConnectionEntity? = rows.firstOrNull { it.id == id }

    override suspend fun getAll(): List<ComfyUIConnectionEntity> =
        rows.sortedByDescending { it.createdAt }

    override suspend fun insert(entity: ComfyUIConnectionEntity): Long {
        val id = idCounter++
        rows.add(entity.copy(id = id))
        bump()
        return id
    }

    override suspend fun insertAll(entities: List<ComfyUIConnectionEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: ComfyUIConnectionEntity): Int {
        val index = rows.indexOfFirst { it.id == entity.id }
        if (index < 0) return 0
        rows[index] = entity
        bump()
        return 1
    }

    override suspend fun deactivateAll(): Int {
        rows.replaceAll { it.copy(isActive = false) }
        bump()
        return rows.size
    }

    override suspend fun activate(id: Long): Int {
        val index = rows.indexOfFirst { it.id == id }
        if (index < 0) return 0
        rows[index] = rows[index].copy(isActive = true)
        bump()
        return 1
    }

    override suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean): Int {
        val index = rows.indexOfFirst { it.id == id }
        if (index < 0) return 0
        rows[index] = rows[index].copy(lastTestedAt = testedAt, lastTestSuccess = success)
        bump()
        return 1
    }

    override suspend fun deleteById(id: Long): Int {
        val removed = rows.count { it.id == id }
        rows.removeAll { it.id == id }
        bump()
        return removed
    }

    override suspend fun deleteAll(): Int {
        val removed = rows.size
        rows.clear()
        bump()
        return removed
    }
}
