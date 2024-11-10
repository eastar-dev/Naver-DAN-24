@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.kotlin.jvm)
}

apply(from = "../../java.gradle")

dependencies {
    implementation(libs.android.tools.sdk.common)
    implementation(libs.android.tools.common)
    implementation(libs.kotlinpoet)
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("org.ogce:xpp3:1.1.6")

    implementation(project(":tool:common"))
}

