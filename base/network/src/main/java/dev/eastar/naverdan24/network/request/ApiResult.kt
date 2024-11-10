package dev.eastar.naverdan24.network.request

import dev.eastar.naverdan24.domain.entity.EntityThrowable

typealias ApiResult<T> = Result<T>
typealias ApiThrowable = EntityThrowable

fun <T> initial(): ApiResult<T> = ApiResult.failure(ApiThrowable(ApiThrowable.INITIAL, "Initial"))

