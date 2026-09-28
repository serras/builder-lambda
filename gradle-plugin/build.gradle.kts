plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.gradle.plugin)
    id("com.gradle.plugin-publish") version "2.2.1"
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
    testImplementation(libs.kotlin.test.junit5)
}

buildConfig {
    packageName(project.group.toString())

    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"com.serranofp.builder.lambda\"")

    val pluginProject = project(":builder-lambda-compiler-plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${pluginProject.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${pluginProject.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${pluginProject.version}\"")

    val annotationsProject = project(":builder-lambda-support-lib")
    buildConfigField(
        type = "String",
        name = "ANNOTATIONS_LIBRARY_COORDINATES",
        expression = "\"${annotationsProject.group}:${annotationsProject.name}:${annotationsProject.version}\""
    )
}

gradlePlugin {
    website = "https://serranofp.com"
    vcsUrl = "https://github.com/serras/builder-lambda"

    plugins {
        create("BuilderLambdaPlugin") {
            id = "com.serranofp.builder.lambda"
            displayName = "Builder Lambda Kotlin plug-in"
            description = "Java builders, the way Kotliners like them"
            implementationClass = "com.serranofp.builder.lambda.BuilderLambdaGradlePlugin"
            tags = setOf("builder", "kotlin")
        }
    }
}

if (project.findProperty("onlyLocal")?.toString()?.toBooleanStrict() == true) {
    publishing {
        repositories {
            maven {
                name = "localPluginRepository"
                url = uri("${rootProject.projectDir.absolutePath}/build/local-plugin-repository")
            }
        }
    }
}
