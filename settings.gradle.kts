pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "builder-lambda"

include("builder-lambda-compiler-plugin")
project(":builder-lambda-compiler-plugin").projectDir = file("compiler-plugin")

include("builder-lambda-plugin")
project(":builder-lambda-plugin").projectDir = file("gradle-plugin")

include("builder-lambda-support-lib")
project(":builder-lambda-support-lib").projectDir = file("support-lib")
