@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.kotlin.jvm)
}

apply(from = "../../java.gradle")
//android
dependencies {
    implementation(libs.kotlinx.coroutines.core)
}

//3th
dependencies {
}

//module
dependencies {
    implementation(project(":tool:common"))
}

//naver
dependencies {
}

//etc
dependencies {
}

//delete
dependencies {
}

tasks.register<JavaExec>("updateToml") {
    group = "_tool"
    description = "Update the TOML configuration file"

    //sourceSets.forEach { println(it) }

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.naver.cafe.toml_updater.TomlUpdaterKt")
}

tasks.register<JavaExec>("updateVersionName") {
    group = "_tool"
    description = "Update the VersionName in TOML configuration file"

    //sourceSets.forEach { println(it) }

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.naver.cafe.toml_updater.VersionNameUpdaterKt")
}
