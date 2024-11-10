package dev.eastar.naverdan24.network.initializer


import dev.eastar.naverdan24.network.BuildConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import okhttp3.internal.toLongOrDefault

val jsonParser by lazy {
    Json {
        prettyPrint = BuildConfig.DEBUG
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
}

/**
 * @Serializable(with = LongOrStringSerializer::class)
 * val recentNoticeAddDate: Long = 0L,
 * https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#custom-serializers
 */
object LongOrStringSerializer : JsonTransformingSerializer<Long>(Long.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonPrimitive -> {
                if (element.isString) {
                    JsonPrimitive(element.content.toLongOrDefault(0L))
                } else {
                    element
                }
            }

            else -> throw SerializationException("Unexpected JSON element: $element")
        }
    }
}
