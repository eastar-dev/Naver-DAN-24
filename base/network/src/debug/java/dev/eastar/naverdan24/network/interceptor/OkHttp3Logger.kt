package dev.eastar.naverdan24.network.interceptor

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.GzipSource
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Invocation
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import kotlin.experimental.and
import kotlin.experimental.inv

/** create : eastar 20220307 */
class OkHttp3Logger : Interceptor {
    companion object {
        fun initializer(context: Context) {
            LOG_FILE = File(context.filesDir, "okhttp3/${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.md").also { it.parentFile?.mkdirs(); it.createNewFile() }
            assetManager = context.assets
            FILE_DIR = context.filesDir
        }

        private lateinit var FILE_DIR: File

        private lateinit var assetManager: AssetManager


        var LOG = true
        private var LOG_FILE: File? = null

        private var REQUEST_LOG = android.util.Log.DEBUG
        private var RESPONSE_LOG = android.util.Log.INFO
        private var RESPONSE_ERROR_LOG = android.util.Log.WARN


        var REQUEST_LOCATOR = true
        var REQUEST_SIMPLE = true
        var REQUEST_COOKIE = false
        var REQUEST_HEADER = false
        var REQUEST_BODY = false
        var RESPONSE_LOCATOR = false
        var RESPONSE_SIMPLE = true
        var RESPONSE_COOKIE = false
        var RESPONSE_HEADER = false
        var RESPONSE_BODY = false
        var RESPONSE_BODY_1000 = false
        var REQUEST_FILE = true
        var RESPONSE_FILE = true


        private const val COOKIE = "Cookie"
        private const val SET_COOKIE = "Set-Cookie"
        private const val TAG = "okhttp"

        //release때 오류가 날수있도록 open 한다
        const val HEADER_NO_LOG = "HEADER_NO_LOG"
        const val HEADER_HAS_LOG = "HEADER_HAS_LOG"
        const val HEADER_DUMMY = "HEADER_DUMMY"

        private const val LF = "\n"
        private const val LOG_LENGTH = 3600
        private const val PREFIX = "``"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val dummy = request.header(HEADER_DUMMY)
        if (!dummy.isNullOrBlank()) {
            val responseBodyText = assetManager.open(dummy).reader().use { it.readText() }
            responseLog(RESPONSE_LOG, "$TAG:dummy", responseBodyText.take(LOG_LENGTH))
            return Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK").body(responseBodyText.toResponseBody("application/json; charset=UTF-8".toMediaType())).build()
        }


        //log header remove
        val isNoLog = request.header(HEADER_NO_LOG) != null
        request = request.newBuilder().removeHeader(HEADER_NO_LOG).build()

        //log header remove
        val hasLogLevel = request.header(HEADER_HAS_LOG)
        request = request.newBuilder().removeHeader(HEADER_HAS_LOG).build()

        if (isNoLog) return chain.proceed(request)
        if (!LOG) return chain.proceed(request)

        val reqLocator: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("REQUEST_LOCATOR") ?: REQUEST_LOCATOR
        val reqSimple: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("REQUEST_SIMPLE") ?: REQUEST_SIMPLE
        val reqCookie: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("REQUEST_COOKIE") ?: REQUEST_COOKIE
        val reqHeader: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("REQUEST_HEADER") ?: REQUEST_HEADER
        val reqBody: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("REQUEST_BODY") ?: REQUEST_BODY
        val reqFile: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("REQUEST_FILE") ?: REQUEST_FILE
        val resLocator: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("RESPONSE_LOCATOR") ?: RESPONSE_LOCATOR
        val resSimple: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("RESPONSE_SIMPLE") ?: RESPONSE_SIMPLE
        val resCookie: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("RESPONSE_COOKIE") ?: RESPONSE_COOKIE
        val resHeader: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("RESPONSE_HEADER") ?: RESPONSE_HEADER
        val resBody: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("RESPONSE_BODY") ?: RESPONSE_BODY
        val resBody1000: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("RESPONSE_BODY_1000") ?: RESPONSE_BODY_1000
        val resFile: Boolean = hasLogLevel?.takeIf { it.isNotBlank() }?.contains("RESPONSE_FILE") ?: RESPONSE_FILE


        if (reqLocator) callerLog(request)
        if (reqSimple) requestLog(TAG, "${request.method}:${request.url.adj} ${request.bodyString(true).take(1000)}")
        if (reqCookie) requestLog("$TAG:c", request.headers.cookieString())
        if (reqHeader) requestLog("$TAG:h", request.headers.headerString())
        if (reqBody) requestLog(TAG, request.bodyString())
        if (reqFile) flog("### ${request.method}:${request.url.adj}\n${request.mdString()}")
        val startNs = System.currentTimeMillis()
        return kotlin.runCatching {
            chain.proceed(request)
        }.onSuccess { res ->
            val tookMs = System.currentTimeMillis() - startNs
            val isTextResponse = res.body?.contentType()?.subtype.toString().lowercase().let { it == "json" || it == "xml" }

            val priority = res.isSuccessful.infoOrWarn
            if (resLocator) callerLog(request)
            if (resSimple) responseLog(priority, TAG, "${res.code} ${res.message} (${tookMs}ms) ${request.method}:${res.request.url.adj}  ${res.bodyString(true).take(1000)}")
            if (resCookie) responseLog(priority, "$TAG:c", res.headers.cookieString())
            if (resHeader) responseLog(priority, "$TAG:h", res.headers.headerString())
            if (resBody) responseLog(priority, TAG, res.bodyString())
            if (resBody1000) responseLog(priority, TAG, res.bodyString().take(1000))
            if (resFile) flog(">> ${request.method}:${request.url.adj}\n${res.mdString()}")
            if (resFile && isTextResponse) flog(res.bodyString() + ",", request.toFile())
        }.onFailure {
            callerLog(request)
            responseErrorLog(TAG, it.message + "\n${request.method}:${request.url.adj} ${request.bodyString(true)}")
            if (resFile) flog(">> ${request.method}:${request.url.adj}\n${request.mdString()}\n${android.util.Log.getStackTraceString(it)}\n")
        }.getOrThrow()
    }

    private fun Request.toFile(): File {
        val filename = method + url.encodedPath.replace("\\d".toRegex(), "").replace("/", "").replace(":", "").replace("=", "").replace("?", "").replace("&", "").replace("%", "").take(256 - "okhttp3/.json".length)

        val file = File(FILE_DIR, "okhttp3/$filename.json")
        file.parentFile?.mkdirs()
        if (!file.exists()) {
            file.createNewFile()
            file.writeText("[")
        }
        return file
    }

    private fun flog(text: String, file: File? = LOG_FILE) = CoroutineScope(Dispatchers.IO).launch {
        appendTextActor().send(text to file)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun CoroutineScope.appendTextActor(): SendChannel<Pair<String, File?>> = actor {
        for ((text, file) in channel) {
            file?.appendText(text)
        }
    }

    private fun callerLog(request: Request) {
        request.tag(Invocation::class.java)?.method()?.run {
            val tag = "$TAG:s"
            val ext = if (declaringClass.getAnnotation(Metadata::class.java) == null) "java" else "kt"
            val locator = "(${declaringClass.simpleName}.$ext:0)"
            val msg: String = makeMessage(locator, name)
            println(REQUEST_LOG, tag, msg)
        }
    }

    private fun requestLog(tag: String, vararg args: Any?) {
        if (!LOG) return
        val msg: String = makeMessage(*args)
        println(REQUEST_LOG, tag, msg)
    }

    private fun responseLog(priority: Int, tag: String, vararg args: Any?) {
        val msg: String = makeMessage(*args)
        println(priority, tag, msg)
    }

    private fun responseErrorLog(tag: String, vararg args: Any?) {
        val msg: String = makeMessage(*args)
        println(RESPONSE_ERROR_LOG, tag, msg)
    }


    private fun println(priority: Int, tag: String, msg: String): Int = msg.split(LF).flatMap { it.splitSafe(LOG_LENGTH) }.let {
        when (it.size) {
            0 -> android.util.Log.println(priority, tag, PREFIX)
            1 -> android.util.Log.println(priority, tag, PREFIX + it[0])
            else -> it.sumOf { log -> android.util.Log.println(priority, tag, PREFIX + log) }
        }
    }

    private fun makeMessage(vararg args: Any?): String = args.joinToString { it.toString().jsonPretty }

    private fun Headers.headerString(): String = filterNot { it.first.equals(SET_COOKIE, true) || it.first.equals(COOKIE, true) }.joinToString("\n") { it.first + ": " + it.second }

    private fun Headers.cookieString(): String = filter { it.first.equals(SET_COOKIE, true) || it.first.equals(COOKIE, true) }.joinToString("\n") { it.first + ": " + it.second }

    private val Headers.mimetype: String?
        get() {
            val contentType = get("content-type")
            contentType ?: return null
            return contentType.trim().split(';').firstOrNull()?.substringAfterLast("/")
        }

    private fun Request.bodyString(minify: Boolean = false): String {
        val body = body
        body ?: return ""

        if (bodyHasUnknownEncoding(headers)) return ""

        val buffer = Buffer()
        body.writeTo(buffer)

        return if (isPlaintext(buffer)) {
            val charset = body.contentType()?.charset(UTF_8) ?: UTF_8
            val bodyString = if (body.contentLength() != 0L) buffer.clone().readString(charset) else ""
            return bodyString.pretty(headers.mimetype, minify)
        } else "BODY_BINARY:[${body.contentLength()}]"
    }

    //    replace("\r","").replace("\n","").replace("\t","")
    private fun Response.bodyString(minify: Boolean = false): String {
        val body = body
        body ?: return ""

        if (bodyHasUnknownEncoding(headers)) return ""

        val source = body.source()
        source.request(Long.MAX_VALUE) // Buffer the entire body.
        var buffer = source.buffer

        if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
            var gzippedResponseBody: GzipSource? = null
            try {
                gzippedResponseBody = GzipSource(buffer.clone())
                buffer = Buffer()
                buffer.writeAll(gzippedResponseBody)
            } finally {
                gzippedResponseBody?.close()
            }
        }

        val charset = body.contentType()?.charset(UTF_8) ?: UTF_8
        val bodyString = if (body.contentLength() != 0L) buffer.clone().readString(charset) else ""
        return bodyString.pretty(headers.mimetype, minify)
    }

    private fun String.pretty(mimetype: String?, minify: Boolean = false): String = when {
        mimetype.equals("xml", true) && isNotBlank() -> if (minify) xmlPretty.trim().replace("\n", "").replace(">\\s+<".toRegex(), "><")
        else xmlPretty

        mimetype.equals("json", true) && isNotBlank() -> if (minify) jsonMinify
        else jsonPretty

        else -> this
    }


    private fun Request.mdString(): String {
        val contentType = headers.mimetype
        val bodyString = bodyString()
        return when {
            contentType.equals("xml", true) && bodyString.isNotBlank() -> "```xml\n$bodyString\n```\n"

            contentType.equals("json", true) && bodyString.isNotBlank() -> "```json\n$bodyString\n```\n"

            else -> ""
        }
    }

    private fun Response.mdString(): String {
        val contentType = headers.mimetype
        val bodyString = bodyString()
        return when {
            contentType.equals("xml", true) && bodyString.isNotBlank() -> "```xml\n$bodyString\n```\n"

            contentType.equals("json", true) && bodyString.isNotBlank() -> "```json\n$bodyString\n```\n"

            else -> ""
        }
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"]
        return (contentEncoding != null && !contentEncoding.equals("identity", ignoreCase = true) && !contentEncoding.equals("gzip", ignoreCase = true))
    }

    private fun isPlaintext(buffer: Buffer): Boolean = kotlin.runCatching {
        val prefix = Buffer()
        val byteCount = if (buffer.size < 64) buffer.size else 64
        buffer.copyTo(prefix, 0, byteCount)
        for (i in 0..15) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return false
            }
        }
        return true
    }.getOrDefault(false)

    private val Boolean?.infoOrWarn: Int
        get() = if (this == true) RESPONSE_LOG else RESPONSE_ERROR_LOG
}

private val HttpUrl.adj
    get(): String {
        val dropQuery = setOf("uId", "tag", "md", "msgpad")
        return Uri.parse(toString())
            .buildUpon()
            .apply {
                clearQuery()
                queryParameterNames
                    .filterNot { dropQuery.contains(it) }
                    .forEach {
                        appendQueryParameter(it, queryParameter(it))
                    }
            }.toString()
    }


private val String.xmlPretty
    get() = kotlin.runCatching {
        val transformer: Transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        val result = StreamResult(StringWriter())
        val xmlInput: Source = StreamSource(StringReader(this))
        transformer.transform(xmlInput, result)
        result.writer.toString()
    }.onFailure {
        android.util.Log.getStackTraceString(it)
    }.getOrDefault(this)

private val String.jsonPretty
    get() = kotlin.runCatching {
        JSONObject(trim()).toString(2)
    }.recoverCatching {
        JSONArray(trim()).toString(2)
    }.getOrDefault(this)

private val String.jsonMinify
    get() = kotlin.runCatching {
        JSONObject(trim()).toString()
    }.recoverCatching {
        JSONArray(trim()).toString()
    }.getOrDefault(this)

private val String.width get() = toByteArray(Charset.forName("euc-kr")).size

private fun String.takeLastSafe(width: Int): String {
    var text: String = takeLast(width)
    while (text.width != width) {
        text = if (text.width > width) text.drop(1)
        else ".".repeat(text.width - width) + text
    }
    return text
}

private fun ByteArray.takeSafe(lengthByte: Int, startOffset: Int): String {
    if (size <= startOffset) return ""

    //앞에서 문자중간을 건너뜀
    var offset = startOffset
    while (size > offset && get(offset) and 0b1100_0000.toByte() == 0b1000_0000.toByte()) offset++

    //문자열 길이가 짧은경우 끝까지
    if (size <= offset + lengthByte) return String(this, offset, size - offset)

    //char 중간이 아니면 거기까지
    if (get(offset + lengthByte) and 0b1100_0000.toByte() != 0b1000_0000.toByte()) return String(this, offset, lengthByte)

    //char 중간이거나 끝이면 앞으로 땡김
    var position = offset + lengthByte
    while (get(--position) and 0b1100_0000.toByte() == 0b1000_0000.toByte()) Unit

    val charByteMoveCount = offset + lengthByte - position
    val charByteLength = get(position).inv().countLeadingZeroBits()

    return if (charByteLength == charByteMoveCount)
    //char 끝이면 거기까지
        String(this, offset, lengthByte)
    else
    //char 중간이면 뒤에버림
        String(this, offset, position - offset)
}

private fun String.splitSafe(lengthByte: Int): List<String> {
    require(lengthByte >= 3) { "min split length getter then 3" }
    val textByteArray = toByteArray()
    if (textByteArray.size <= lengthByte) return listOf(this)

    val tokens = mutableListOf<String>()
    var startOffset = 0
    while (startOffset + lengthByte < textByteArray.size) {
        val token = textByteArray.takeSafe(lengthByte, startOffset)
        tokens += token
        startOffset += token.toByteArray().size
    }
    tokens += String(textByteArray, startOffset, textByteArray.size - startOffset)
    return tokens
}
