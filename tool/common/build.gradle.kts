//plugins {
//    id("java-library")
//    alias(libs.plugins.kotlin.jvm)
//}
//
//java {
//    sourceCompatibility = JavaVersion.VERSION_17
//    targetCompatibility = JavaVersion.VERSION_17
//}
plugins {
    alias(libs.plugins.kotlin.jvm)
}

apply(from = "../../java.gradle")
