package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.local.dao.SDWebUIConnectionDao
import com.riox432.civitdeck.data.local.entity.SDWebUIConnectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Hand-written in-memory fake for [SDWebUIConnectionDao] used by repository tests.
 * Mirrors the Room DAO semantics (auto id, single active row, createdAt DESC order).
 */
class FakeSDWebUIConnectionDao : SDWebUIConnectionDao {
    val rows = mutableListOf<SDWebUIConnectionEntity>()
    private var idCounter = 1L
    private val updates = MutableStateFlow(0)

    private fun bump() { updates.value++ }

    override fun observeAll(): Flow<List<SDWebUIConnectionEntity>> =
        updates.map { rows.sortedByDescending { it.createdAt } }

    override fun observeActive(): Flow<SDWebUIConnectionEntity?> =
        updates.map { rows.firstOrNull { it.isActive } }

    override suspend fun getActive(): SDWebUIConnectionEntity? = rows.firstOrNull { it.isActive }

    override suspend fun getAll(): List<SDWebUIConnectionEntity> =
        rows.sortedByDescending { it.createdAt }

    override suspend fun insert(entity: SDWebUIConnectionEntity): Long {
        val id = idCounter++
        rows.add(entity.copy(id = id))
        bump()
        return id
    }

    override suspend fun insertAll(entities: List<SDWebUIConnectionEntity>) {
        entities.forEach { insert(it) }
    }

    override suspend fun update(entity: SDWebUIConnectionEntity): Int {
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
