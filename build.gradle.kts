// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    //https://developer.android.com/develop/ui/compose/compiler
    alias(libs.plugins.compose.compiler) apply false

    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.hilt) apply false

    alias(libs.plugins.androidx.navigation) apply false

    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    alias(libs.plugins.android.junit5) apply false
}
