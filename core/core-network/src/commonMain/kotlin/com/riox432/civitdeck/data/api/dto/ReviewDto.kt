package com.riox432.civitdeck.data.api.dto

import kotlinx.serialization.Serializable

// --- tRPC wrapper types ---

@Serializable
data class TrpcResponse<T>(
    val result: TrpcResult<T>,
)

@Serializable
data class TrpcResult<T>(
    val data: TrpcData<T>,
)

@Serializable
data class TrpcData<T>(
    val json: T,
)

// --- Review DTOs ---

@Serializable
data class ReviewListResponse(
    val items: List<ReviewDto> = emptyList(),
    val nextCursor: Int? = null,
)

@Serializable
data class ReviewDto(
    val id: Long = 0,
    val modelId: Long = 0,
    val modelVersionId: Long = 0,
    val rating: Int = 0,
    val recommended: Boolean = false,
    val details: String? = null,
    val createdAt: String = "",
    val nsfw: Boolean = false,
    val exclude: Boolean = false,
    val user: ReviewUserDto? = null,
    val thread: ReviewThreadDto? = null,
)

@Serializable
data class ReviewUserDto(
    val id: Long = 0,
    val username: String? = null,
    val image: String? = null,
)

@Serializable
data class ReviewThreadDto(
    val commentCount: Int = 0,
)

@Serializable
data class RatingTotalsResponse(
    @kotlinx.serialization.SerialName("1") val star1: Int = 0,
    @kotlinx.serialization.SerialName("2") val star2: Int = 0,
    @kotlinx.serialization.SerialName("3") val star3: Int = 0,
    @kotlinx.serialization.SerialName("4") val star4: Int = 0,
    @kotlinx.serialization.SerialName("5") val star5: Int = 0,
    val up: Int = 0,
    val down: Int = 0,
)

// --- tRPC input wrappers ---

@Serializable
data class TrpcInput<T>(
    val json: T,
)

@Serializable
data class ReviewListInput(
    val modelId: Long,
    val modelVersionId: Long? = null,
    val limit: Int = 20,
    val cursor: Int? = null,
)

@Serializable
data class RatingTotalsInput(
    val modelId: Long,
    val modelVersionId: Long? = null,
)

@Serializable
data class CreateReviewInput(
    val modelId: Long,
    val modelVersionId: Long,
    val rating: Int,
    val recommended: Boolean,
    val details: String? = null,
)
