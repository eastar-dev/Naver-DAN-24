@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION")

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("com.github.johnrengelman.shadow") version "7.1.2" // Shadow plugin 추가
}

apply(from = "../../java.gradle")

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.4")
    implementation(libs.kotlinx.serialization)
}

application {
    mainClass.set("com.naver.cafe.deploy.ServerKt")  // 메인 클래스 설정
}

// shadowJar 설정
tasks {
    shadowJar {
        manifest {
            attributes["Main-Class"] = "com.naver.cafe.deploy.ServerKt" // 실행할 메인 클래스
        }
        archiveFileName.set("deploy_server.jar") // JAR 파일 이름 설정
        mergeServiceFiles() // 서비스 파일 병합
    }
}

// 기존 jar 태스크를 shadowJar로 대체
tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.naver.cafe.deploy.ServerKt"  // 실행할 메인 클래스
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

//projectRoot run
// ./gradlew deployServer
// ./gradlew :tool:deploy_server:deployServer
tasks.register<JavaExec>("deployServer") {
    group = "_tool"
    description = "Deploy server for apk"

    //sourceSets.forEach { println(it) }

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.naver.cafe.deploy.ServerKt")
}
