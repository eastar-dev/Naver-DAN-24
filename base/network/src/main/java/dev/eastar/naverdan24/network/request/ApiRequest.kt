@file:Suppress("UNCHECKED_CAST")

package dev.eastar.naverdan24.network.request

import android.log.Log
import dev.eastar.naverdan24.network.error.toApiThrowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

/** request -> ApiResult<T> */
suspend inline fun <T> requestResult(
    /** localData 경우는 람다 식보다 값이 suspend 제약에서 벗어날수 있어 활용성이 더 높다. */
    localData: T? = null,
    /** localData == null 인경우는 내부에서 자동으로 처리된다. */
    crossinline isRemote: (T?) -> Boolean = { it == null },
    crossinline request: suspend () -> T,
): Result<T> {
    return if (localData == null || isRemote(localData)) {
        try {
            Result.success(request())
        } catch (th: Throwable) {
            Log.printStackTrace(th)
            Result.failure(th.toApiThrowable())
        }
    } else {
        Result.success(localData)
    }
}
