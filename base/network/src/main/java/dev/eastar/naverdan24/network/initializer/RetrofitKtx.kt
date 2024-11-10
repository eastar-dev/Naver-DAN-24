package dev.eastar.naverdan24.network.initializer

import okhttp3.ResponseBody
import java.nio.charset.StandardCharsets

fun ResponseBody.stringClone(): String {
    val source = source()
    source.request(Long.MAX_VALUE) // Buffer the entire body.
    val buffer = source.buffer
    val charset = contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
    return buffer.clone().readString(charset)
}
