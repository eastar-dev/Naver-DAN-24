package com.naver.cafe.svg2vector

import com.android.ide.common.vectordrawable.Svg2Vector
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.streams.toList
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

/**
 * Created by eastar on 2023/05/27.
 */
@OptIn(ExperimentalTime::class)
fun main() = measureTime {
    svg2Vector()
}.let { println(it) }

private fun svg2Vector(source: Path = Paths.get(""), deleteSource: Boolean = true, getTarget: (File) -> File = ::getTarget) = Files.walk(source)
    .filter { it.toString().endsWith(".svg", true) }
    .toList()
    .onEach {
        val target = getTarget(it.toFile())
        println("$it -> $target")
        Svg2Vector.parseSvgToXml(it, FileOutputStream(target))
    }.forEach {
        if (deleteSource) it.toFile().delete()
    }


private fun getTarget(source: File) = Pattern
    .compile("^[^a-z]*([a-z].*)?")
    .matcher(source.name)
    .replaceAll("$1")
    .replace('-', '_')
    .replace(".svg", ".xml")
    .let { File(source.parentFile, it) }
