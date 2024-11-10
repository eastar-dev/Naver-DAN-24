package dev.eastar.common

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.FileVisitOption.FOLLOW_LINKS
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolutePathString
import kotlin.io.path.name

private const val MODULE_ROOT_FILE = "build.gradle.kts"
private const val PROJECT_ROOT_FILE = "gradlew"


fun exitProcess(error: String): Nothing {
    println(error)
    kotlin.system.exitProcess(error.hashCode())
}

fun getCurrentExecutablePath(): String {
    val path = Paths.get("")
    //println("현재 작업 디렉토리 1: ${System.getProperty("user.dir")}")
    //println("현재 작업 디렉토리 2: ${path.toAbsolutePath()}")
    return path.toAbsolutePath().toString()
}

fun getProjectRoot(): String {
    val thisPath = getCurrentExecutablePath()
    return findParentFolderWithFile(thisPath, PROJECT_ROOT_FILE) ?: exitProcess("projectRoot not found")
}

fun getModules(): List<String> {
    val projectRoot = getProjectRoot()
    return findFoldersWithFile(projectRoot, MODULE_ROOT_FILE).filterNot { it == projectRoot }
}

fun readFromClipboard(): String? {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    return try {
        clipboard.getData(DataFlavor.stringFlavor) as? String
    } catch (e: Exception) {
        null
    }
}

fun readFirstLineFromFile(): String? {
    val projectRoot = getProjectRoot()
    return BufferedReader(FileReader("$projectRoot/release/shell/module_mover_last_target.txt")).use { reader ->
        reader.readLine()
    }
}

fun selectModule(): String {
    val modules = getModules()
    val projectRoot = getProjectRoot()

    modules.mapIndexed { index, module ->
        println("${index + 1}. $module")
    }
    val input = readlnOrNull()?.toIntOrNull() ?: exitProcess("input not found")
    if (input in 1..modules.size) {
        val selectedModule = modules[input - 1]
        BufferedWriter(FileWriter("${projectRoot}/release/shell/module_mover_last_target.txt")).use { writer ->
            writer.write(selectedModule)
        }
        return selectedModule
    } else {
        exitProcess("input warn")
    }
}


@Suppress("SameParameterValue")
fun findParentFolderWithFile(startingFile: String, targetFileName: String): String? {
    var currentFile: File? = File(startingFile)
    while (currentFile != null) {
        if (currentFile.isDirectory) {
            val targetFilePath = File(currentFile, targetFileName).toPath()
            if (Files.exists(targetFilePath)) {
                return currentFile.absolutePath
            }
        }

        currentFile = currentFile.parentFile
    }

    return null
}

fun findFirstOrNullEndsWithFile(startingFile: String, targetFileName: String): File? {
    return findFile(startingFile, targetFileName, Int.MAX_VALUE) {
        toString().endsWith(it.toString())
    }.firstOrNull()?.let { File(it) }
}

fun findFoldersWithFile(startingFolder: String, targetFileName: String, maxDepth: Int = 3): List<String> {
    return findFile(startingFolder, targetFileName, maxDepth).map { File(it).parentFile.absolutePath }.distinct().sorted()
}


fun findFile(startingFolder: String, targetFileName: String, maxDepth: Int = Int.MAX_VALUE, comparator: Path.(Path) -> Boolean = { this.fileName == it.fileName }): List<String> {
    val targetFile = File(targetFileName)

    val resultFiles = mutableListOf<String>()
    val visitor = object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes) = when (dir.name) {
            ".git", ".idea", ".github", ".gradle", "build", "release" -> FileVisitResult.SKIP_SUBTREE
            else -> FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (file.comparator(targetFile.toPath())) {
                resultFiles.add(file.absolutePathString())
                return FileVisitResult.SKIP_SUBTREE
            }
            return FileVisitResult.CONTINUE
        }
    }

    val startingPath = Paths.get(startingFolder)
    Files.walkFileTree(startingPath, setOf(FOLLOW_LINKS), maxDepth, visitor)

    return resultFiles.toList()
}


fun move(filesAndFolders: List<Pair<String, String>>) {
    filesAndFolders.forEach { (source, target) ->
        move(source, target)
    }
}

fun moveFiles(filesAndFolders: List<Pair<String, String>>) {
    filesAndFolders.forEach { (source, target) ->
        kotlin.runCatching {
            val sourceFileOrFolder = File(source)
            val targetFileOrFolder = File(target)
            targetFileOrFolder.parentFile.mkdirs()
            Files.copy(sourceFileOrFolder.toPath(), targetFileOrFolder.toPath(), StandardCopyOption.REPLACE_EXISTING)
            if (!sourceFileOrFolder.delete()) println("삭제 실패: $sourceFileOrFolder")
        }.onSuccess {
            println("성공: $source -> $target")
        }.onFailure {
            println("*실패: $source -> $target")
        }
    }
}

fun move(pair: Pair<String, String>) = move(pair.first, pair.second)
fun move(source: String, target: String) {
    runCatching {
        val sourceFileOrFolder = File(source)
        val targetFileOrFolder = File(target)
        targetFileOrFolder.parentFile.mkdirs()
        Files.copy(sourceFileOrFolder.toPath(), targetFileOrFolder.toPath(), StandardCopyOption.REPLACE_EXISTING)
        if (!sourceFileOrFolder.delete()) println("삭제 실패: $sourceFileOrFolder")
    }.onSuccess {
        println("성공: $source -> $target")
    }.onFailure {
        println("*실패: $source -> $target")
    }
}
