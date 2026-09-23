plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.node.gradle) apply false
    alias(libs.plugins.buildconfig) apply false
}

allprojects {
    group = "com.serranofp"
    version = "0.1.0-SNAPSHOT"
}
