@file:Suppress("UnstableApiUsage", "DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

apply(from = "../../android.gradle")
apply(from = "../../compose.gradle")
apply(from = "../../hilt.gradle")

android {
    namespace = "dev.eastar.naverdan24.__package__"
}
//android
dependencies {
}

//3th
dependencies {
}

//module
dependencies {
    //implementation(project("## 여기에 TEST할 모듈을 넣으세요##"))
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
