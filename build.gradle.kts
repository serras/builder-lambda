plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.node.gradle) apply false
    alias(libs.plugins.buildconfig) apply false
    alias(libs.plugins.publish) apply false
}

allprojects {
    group = "com.serranofp"
    version = (property("version") as? String).let { version ->
        if (version == null || version == "unspecified") "0.1.0-SNAPSHOT"
        else version
    }
}
