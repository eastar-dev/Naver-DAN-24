@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kapt)
}

apply(from = "../../android.gradle")
apply(from = "../../hilt.gradle")
android {
    namespace = "dev.eastar.naverdan24.network"
}
//android
dependencies {
    implementation(libs.androidx.startup.runtime)
}

//3th
dependencies {
    implementation(libs.okhttp)
    api(libs.retrofit)

    //https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json-jvm
    api(libs.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.converter)
}

//module
dependencies {
}

//etc
dependencies {
    debugImplementation("com.sealwu.jsontokotlin:library:3.7.4")
}

//delete
dependencies {
}
