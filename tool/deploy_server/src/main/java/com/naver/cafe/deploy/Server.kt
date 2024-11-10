package com.naver.cafe.deploy

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.system.measureTimeMillis

// 서버 인스턴스를 전역 변수로 저장
lateinit var server: ApplicationEngine
internal val userHome: String = System.getProperty("user.home")
internal val rootDir = File("$userHome/Desktop/deploy")

fun main() {
    println("==================================")
    println("[$timestamp]Starting server...")

    //val item = rootDir.walk()
    //    .filterNot { it.isHidden }
    //    .filterNot {
    //        println("$it : ${it.parentFile.path} == ${rootDir.path}")
    //        it.path == rootDir.path
    //    }
    //    .filter {
    //        !it.isDirectory || it.extension in listOf("apk", "aab", "mapping")
    //    }
    //    .toList()
    //
    //println("==================================")
    //item.forEach {
    //    println(it)
    //}


    server = embeddedServer(Netty) {
        routing {
            listRoot()

            listFolder()
            // GET /live
            live()
            // GET /upload
            uploadForm()
            // POST /upload
            upload()
        }
    }

    println("[$timestamp]Server has started on port ${server.environment.connectors[0].port}")
    server.start(wait = true)

    println("[$timestamp]Server has stopped")
}

private fun Routing.listRoot() {
    get("/") {
        //println("listFolder : root")
        if (call.isJson)
            call.appJson()
        else
            call.respondText(htmlHeader + generateHtml(rootDir) + htmlTail, ContentType.Text.Html)
    }
}

private fun Routing.listFolder() {
    get("/{path...}") {
        val path = call.parameters.getAll("path")?.joinToString(File.separator)
        //println("listFolder : $path")

        val file = File(rootDir, path ?: "")
        if (file.exists()) {
            if (file.isFile) {
                // 파일이 APK 파일인 경우 MIME 타입을 설정하여 응답
                if (file.extension == "apk") {
                    // Content-Disposition 헤더 설정
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.name).toString()
                    )
                    // Content-Type 헤더 설정
                    call.response.header(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                    // 파일 응답

                    measureTimeMillis {
                        call.respondFile(file)
                    }.let { println("[$timestamp]download : [$it] $file") }
                } else {
                    // 일반 파일 응답
                    call.respondFile(file)
                }
            } else {
                call.respondText(htmlHeader + generateHtml(file) + htmlTail, ContentType.Text.Html)
            }
        } else {
            call.respondText("File not found", status = HttpStatusCode.NotFound)
        }
    }
}

fun generateHtml(dir: File): String {
    val files: Array<File> = dir.listFiles() ?: emptyArray()
    return files
        .asSequence()
        .filterNot { it.isHidden }
        .filterNot { it.isDirectory && it.name == "conf" }
        //.filterNot { it.isFile && it.extension !in listOf("apk", "aab", "mapping") }
        .sortedByDescending { file ->
            if (file.isDirectory) {
                file.name.split(".").joinToString("") {
                    it.toIntOrNull()?.let { "%02d".format(it) } ?: "000001"
                } + file.name
            } else {
                "000000" + file.name
            }
        }.joinToString("", "<ul>", "</ul>") { file ->
            val link = file.relativeTo(rootDir).path
            when {
                file.isDirectory -> "<li><a href='$link'>${file.name}</a>${generateHtml(file)}</li>"
                else -> "<li><a href='$link'>${file.name}</a></li>"
            }
        }
}

private fun Routing.live() {
    get("/live") {
        println("live")
        call.respondText("OK", ContentType.Text.Plain)
    }
}

private fun Routing.uploadForm() {
    get("/upload") {
        println("uploadForm")
        val htmlForm = """
                    <html>
                    <body>
                        <h1>File Upload</h1>
                        <form action="/" method="post" enctype="multipart/form-data">
                            <label for="path">Upload Path:</label><br>
                            <input type="file" name="file" required><br><br>
                            <button type="submit">Upload File</button>
                        </form>
                    </body>
                    </html>
                """.trimIndent()
        call.respondText(htmlForm, ContentType.Text.Html)
    }
}


/**
 * path 에서 경로를 받아서 파일을 업로드
 * curl -F "file=@app-stage.apk" http://localhost:8080/debug
 * multipart에 path가 있는 경우 우선
 */
private fun Routing.upload() {
    // 특정 경로로 파일 업로드 엔드포인트
    post("/") {
        //println("upload")
        val multipart = call.receiveMultipart()

        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                // 파일 이름 및 경로 설정
                val fileName = part.originalFileName ?: "unnamed"

                val path = "app-([\\d.]+|[\\d.]+\\-stage|[\\d.]+\\-dev)(?:|-[\\d_]+)\\.(?:apk|aab|mapping)".toRegex().find(fileName)?.let {
                    it.groupValues[1]
                } ?: run {
                    "temp"
                }

                val file = File(rootDir, "$path/$fileName")
                val uploadDir = file.parentFile
                // 경로 검증 및 상위 디렉토리 접근 방지
                if (!uploadDir.startsWith(rootDir.canonicalFile)) {
                    call.respondText("Invalid upload directory", status = HttpStatusCode.BadRequest)
                    return@forEachPart
                }

                // 디렉토리 생성
                if (!uploadDir.exists()) uploadDir.mkdirs()

                if (!uploadDir.exists() || !uploadDir.isDirectory) {
                    call.respondText("Invalid upload directory", status = HttpStatusCode.BadRequest)
                    return@forEachPart
                }

                // 파일 저장
                measureTimeMillis {
                    part.streamProvider().use { inputStream ->
                        file.outputStream().buffered().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }.let { println("[$timestamp]upload : [$it] $file") }
            }
            part.dispose()
        }

        call.respondRedirect("/")
    }
}

val timestamp: String get() = LocalDateTime.now().atZone(ZoneOffset.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString()

private val ApplicationCall.isJson: Boolean
    get() = request.queryParameters["format"] == "json"

@Serializable
data class FileInfo(
    val url: String,
    val size: Long,
    val date: Long,
)

suspend fun ApplicationCall.appJson() {
    println("[$timestamp]Call api")

    val fileList = rootDir.walkTopDown()
        .filter { it.isFile && it.extension in listOf("apk", ".aab", "mapping") }
        .sortedByDescending { it.lastModified() }
        .map {
            FileInfo(
                url = request.local.run { "${scheme}://${serverHost}:${serverPort}" } + it.absolutePath.removePrefix(rootDir.absolutePath),
                size = it.length(),
                date = it.lastModified()
            )
        }.toList()

    respondText(Json { prettyPrint = true }.encodeToString(fileList), ContentType.Application.Json)
}
