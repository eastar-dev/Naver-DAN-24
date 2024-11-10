package com.naver.cafe.toml_updater

import dev.eastar.common.getProjectRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.min
import kotlin.time.measureTime

fun main() {
    runBlocking(Dispatchers.IO) {
        measureTime {
            TomlUpdater().run()
        }.let {
            println("완료 시간 : $it")
        }
    }
}


class TomlUpdater {
    companion object {
        // 'versions.toml' 파일의 경로를 정의합니다.
        private val projectRoot = getProjectRoot()
        val TOML_FILE = "$projectRoot/gradle/libs.versions.toml"

        //const val GOOGLE = "https://maven.google.com/"
        const val GOOGLE = "https://dl.google.com/dl/android/maven2/"

        //https://mvnrepository.com/repos/central
        const val MAVEN = "https://repo1.maven.org/maven2/"

        //버전 체크를 건너뛸 경우 아래 문자열로 시작하는 주석이 붙으면 버전 체크를 건너뜁니다.
        const val SKIP_VERSION_CHECK = "#skip-version-check"

        enum class ArtifactLevel {
            dev, alpha, beta, rc, stable;//[default]
        }
    }

    suspend fun run() {
        val tomlFile = TOML_FILE

        val tomlListAll = parseToml(tomlFile)

        val tomlList = tomlListAll
            .groupBy { it.name }
            .values
            .map { it.first() }
        //debug///////////////////////////////////////////////////////
        //.let {
        //    listOf(
        //        TomlInfo(
        //            "androidx-navigation",
        //            "androidx.navigation:navigation-ui-ktx",
        //            "2.5.1",
        //        )
        //    )
        //}
        /////////////////////////////////////////////////////////
        println("========================================================================")
        println("toml 업데이트 대상항목 ${tomlListAll.size}개")
        println("------------------------------------------------------------------------")
        tomlListAll.onEach { println("${it.module}:${it.version}") }
        println("========================================================================")
        println("toml 업데이트 대상항목 ${tomlList.size}개 (중복제거)")
        println("------------------------------------------------------------------------")
        tomlList.onEach { println("${it.module}:${it.version}") }
        println("========================================================================")


        //예외처리사항추가
        val tomlListAdjust = tomlList.map {
            if (it.name == "androidGradlePlugin") {
                //curl https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml
                it.copy(module = "com.android.tools.build:gradle")
            } else if (it.name == "ksp") {
                //curl https://repo1.maven.org/maven2/com/google/devtools/ksp/symbol-processing-gradle-plugin/maven-metadata.xml
                it.copy(module = "com.google.devtools.ksp:symbol-processing-gradle-plugin")
            } else if (it.name == "google-firebase-crashlytics") {
                //https://firebase.google.com/docs/android/troubleshooting-faq?_gl=1*11i4v13*_up*MQ..*_ga*MTA2MzU0MTAzMy4xNzE0Njk3NzUy*_ga_CW55HF8NVT*MTcxNDY5Nzc1MS4xLjAuMTcxNDY5Nzc2Mi4wLjAuMA..#add-plugins-using-buildscript-syntax
                //curl https://dl.google.com/dl/android/maven2/com/google/firebase/firebase-crashlytics-gradle/maven-metadata.xml
                it.copy(module = "com.google.firebase:firebase-crashlytics-gradle")
            } else {
                it
            }
        }

        coroutineScope {
            val tomlListUpdateDeferred = tomlListAdjust
                .map { toml: TomlInfo ->
                    async {
                        val googleVersion = async {
                            runCatching {
                                getVersion(toml.googleUrl, toml.artifactLevel)
                            }.getOrNull()
                        }

                        val mavenVersion = async {
                            runCatching {
                                getVersion(toml.mavenUrl, toml.artifactLevel)
                            }.getOrNull()
                        }

                        //println(coroutineContext)
                        //println(coroutineContext[ContinuationInterceptor]?.javaClass?.name )
                        //println(coroutineContext[ContinuationInterceptor] )
                        //println(((coroutineContext[ContinuationInterceptor] as? ExecutorCoroutineDispatcher)?.executor as ThreadPoolExecutor)?.activeCount)
                        toml.copy(newVersion = getMaxVersion(googleVersion.await(), mavenVersion.await()))
                    }
                }

            val tomlListUpdate = tomlListUpdateDeferred.map { it.await() }

            tomlListUpdate.also {
                //println(it)
                println("========================================================================")
                println("버전을 찾을수 없어서 제외된 항목")
                println("------------------------------------------------------------------------")
            }.filterNot {
                if (it.newVersion == null) println("${it.module}:${it.version} -> ${it.newVersion}")
                it.newVersion == null
            }.also {
                println("========================================================================")
                println("이전버전과 동일해서 제외된 항목")
                println("------------------------------------------------------------------------")
            }.filterNot {
                if (it.version == it.newVersion) println("${it.module}:${it.version} -> ${it.newVersion}")
                it.version == it.newVersion
            }.also {
                println("========================================================================")
                println("업데이트 할 항목")
                println("------------------------------------------------------------------------")
            }.onEach {
                println("${it.module}:${it.version} -> ${it.newVersion}")
            }.also {
                println("========================================================================")
                println("업데이트")
                println("------------------------------------------------------------------------")
            }.fold(File(tomlFile).readText()) { tomlContent, toml ->
                //println("\n${toml.ref} = \"${toml.version}\"")
                //println("\n${toml.ref} = \"${toml.newVersion}\"")
                if (toml.skipVersionCheck) {
                    println("${toml.name} = ${toml.version} -> ${toml.newVersion} $SKIP_VERSION_CHECK")
                    tomlContent
                } else {
                    println("${toml.name} = ${toml.version} -> ${toml.newVersion}")
                    tomlContent.replace("\n${toml.name} = \"${toml.version}\"", "\n${toml.name} = \"${toml.newVersion}\"")
                }
            }.also {
                println("========================================================================")
                println("업데이트 완료")
                File(tomlFile).writeText(it)
            }
            println("========================================================================")
        }
    }


    private val TomlInfo.googleUrl get() = GOOGLE + module.replace(":", "/").replace('.', '/') + "/maven-metadata.xml"
    private val TomlInfo.mavenUrl get() = MAVEN + module.replace(":", "/").replace('.', '/') + "/maven-metadata.xml"

    /**
     * 숫자로 이루어진 부분만 비교한다.
     */
    private fun getMaxVersion(version1: String?, version2: String?): String? {
        if (version1 == null || version2 == null)
            return version1 ?: version2

        val version1List = version1.split(".", "-", "_")
        val version2List = version2.split(".", "-", "_")

        return (0..<min(version1List.size, version2List.size)).map {
            version1List[it].toIntOrNull() to version2List[it].toIntOrNull()
        }.fold(0) { acc, pair ->
            if (acc == 0) {
                when {
                    pair.first == null && pair.second == null -> 0
                    pair.first != null && pair.second == null -> 1
                    pair.first == null && pair.second != null -> -1
                    pair.first!! > pair.second!! -> 1
                    pair.first!! < pair.second!! -> -1
                    else -> 0
                }
            } else
                acc
        }.let {
            when (it) {
                1 -> version1
                -1 -> version2
                else -> version1
            }
        }
    }

    data class TomlInfo(
        val name: String?,
        val module: String = "",
        val version: String?,
        val newVersion: String? = null,
        val skipVersionCheck: Boolean = false,
        val artifactLevel: ArtifactLevel = ArtifactLevel.stable,
    ) {
        override fun toString(): String {
            return name.logPad(30) + " " + skipVersionCheck.logPad() + artifactLevel.name.logPad(6) + " " + module
        }
    }

    @Suppress("SameParameterValue", "UNUSED_DESTRUCTURED_PARAMETER_ENTRY")
    private fun parseToml(path: String): List<TomlInfo> {
        val lines = File(path).readLines()

        val versionsTomlInfo = lines
            .asSequence()
            .withIndex()
            .dropWhile { (index, line) ->
                line != "[versions]"
            }.drop(1)
            .takeWhile { (index, it) ->
                !it.startsWith("[")
            }.filterNot { (index, it) ->
                it.startsWith("#") || it.isBlank() || it.startsWith("[")
            }.map { (index, it) ->
                val (name, version) = it.split('=', ' ', '"').filterNot { it.isBlank() }
                //println("name : $name, version : $version")
                //name to (version to (lines.getOrNull(index - 1)?.startsWith(SKIP_VERSION_CHECK)))
                val beforeLine = lines.getOrNull(index - 1)
                TomlInfo(
                    name = name,
                    version = version,
                    skipVersionCheck = beforeLine?.contains("(?<!#)${SKIP_VERSION_CHECK}".toRegex()) ?: false,
                    artifactLevel = beforeLine?.let { beforeLine ->
                        ArtifactLevel.entries.mapNotNull { artifactLevel ->
                            if (beforeLine.contains("(?<!#)#${artifactLevel.name}".toRegex())) artifactLevel else null
                        }
                    }?.lastOrNull() ?: ArtifactLevel.stable
                )
            }
            .associateBy { it.name }

        val librariesTomlInfoList = lines
            .asSequence()
            .dropWhile {
                it != "[libraries]"
            }.drop(1)
            .takeWhile {
                !it.startsWith("[")
            }
            .filterNot {
                it.startsWith("#") || it.isBlank() || it.startsWith("[")
            }
            .mapNotNull {
                val libraries = it.split('=', ' ', '{', '}', ',', '"')
                    .filterNot { it.isBlank() }.drop(1)

                val module = libraries.get(1)
                val name = libraries.getOrNull(3)
                //println("module : $module, name : $name")
                versionsTomlInfo[name]?.copy(
                    module = module
                )
            }.toList()

        val pluginsTomlInfoList = lines
            .asSequence()
            .dropWhile {
                it != "[plugins]"
            }.drop(1)
            .takeWhile {
                !it.startsWith("[")
            }
            .filterNot {
                it.startsWith("#") || it.isBlank() || it.startsWith("[")
            }
            .mapNotNull {
                val plugins = it.split('=', ' ', '{', '}', ',', '"')
                    .filterNot { it.isBlank() }.drop(1)

                val module = plugins.get(1)
                val name = plugins.getOrNull(3)
                //println("module : $module, name : $name")
                versionsTomlInfo[name]?.copy(
                    module = module
                )
            }.toList()

        return (librariesTomlInfoList + pluginsTomlInfoList)
        //.onEach {
        //    println(it)
        //}
    }

    private fun getVersion(url: String, artifactLevel: ArtifactLevel = ArtifactLevel.stable): String {
        println("최신 버전을 확인 중:$url")
        return DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(URL(url).openConnection().getInputStream())
            .run {
                documentElement.normalize()

                //전체버전확인
                val nodes = getElementsByTagName("version")
                val lastVersion = generateSequence(0) { it + 1 }
                    .take(nodes.length)
                    .map { nodes.item(it).textContent }
                    .last {
                        when (artifactLevel) {
                            ArtifactLevel.dev -> true
                            ArtifactLevel.alpha -> it.lowercase().contains("(dev)".toRegex()).not()
                            ArtifactLevel.beta -> it.lowercase().contains("(dev|alpha)".toRegex()).not()
                            ArtifactLevel.rc -> it.lowercase().contains("(dev|alpha|beta)".toRegex()).not()
                            ArtifactLevel.stable -> it.lowercase().contains("(dev|alpha|beta|rc)".toRegex()).not()
                        }
                    }

                //val lastUpdated = getElementsByTagName("lastUpdated").item(0).textContent
                //val latest = getElementsByTagName("latest").item(0).textContent
                //println("${lastUpdated.take(8)}  $lastVersion $latest")
                lastVersion
            }
    }
}


private val String.width get() = toByteArray(Charset.forName("euc-kr")).size

fun String?.logPad(width: Int = 20, padChar: Char = ' '): String {
    val s = this ?: ""
    val length = s.length + width - s.width
    return s.padEnd(length, padChar).take(length)
}

fun String?.logPadStart(width: Int = 20, padChar: Char = ' '): String {
    val s = this ?: ""
    val length = s.length + width - s.width
    return s.padStart(length).takeLast(length)
}

fun Number?.logPad(width: Int = 3, padChar: Char = ' ') =
    (this?.toString() ?: "").logPadStart(width, padChar)

fun Boolean?.logPad() =
    (this?.toString() ?: "").logPad(6, ' ')

fun Number?.logPadEnd(width: Int = 8, padChar: Char = ' ') =
    (this?.toString() ?: "").logPad(width, padChar)
