package com.naver.cafe.toml_updater

import dev.eastar.common.getProjectRoot
import java.io.File
import java.time.LocalDate
import java.time.temporal.IsoFields
import kotlin.time.measureTime

fun main() {
    measureTime {
        VersionNameUpdater().updateVersionName()
    }.let {
        println("완료 시간 : $it")
    }
}

class VersionNameUpdater {
    companion object {
        // 'versions.toml' 파일의 경로를 정의합니다.
        private val projectRoot = getProjectRoot()
        val TOML_FILE = "$projectRoot/gradle/libs.versions.toml"
    }

    fun updateVersionName() {
        val versionText = File(TOML_FILE).readText()

        var previousYearWeek = ""


        val s = versionText.indexOf("versionName = ")
        val e = versionText.indexOf("\n", s)
        val line = versionText.substring(s, e)

        val parts = line.split("=")
        if (parts.size == 2) {
            val currentVersionName = parts[1].trim().trim('\"')
            val versionParts = currentVersionName.split(".")
            if (versionParts.size == 3) {
                previousYearWeek = versionParts[0] + "." + versionParts[1]
            }

            val currentYearWeek = getCurrentYearWeek()
            val newVersionName = if (currentYearWeek != previousYearWeek) {
                "$currentYearWeek.0"
            } else {
                currentYearWeek + "." + versionParts[2]
            }
            val adjLine = "versionName = \"$newVersionName\""
            File(TOML_FILE).writeText(versionText.replaceRange(s, e, adjLine))
        }
    }

    private fun getCurrentYearWeek(): String {
        val currentDate = LocalDate.now()
        val year = currentDate.year % 100
        val week = currentDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "%02d.%02d".format(year, week)
    }
//private fun getCurrentYearWeek(): String {
//    val currentDate = LocalDate.now()
//    return currentDate.format(DateTimeFormatter.ofPattern("yy.Mdd"))
//}
}

