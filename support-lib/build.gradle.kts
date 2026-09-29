@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import groovy.util.Node
import groovy.util.NodeList

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.publish)
}

kotlin {
    explicitApi()

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js().nodejs()

    jvm()

    linuxArm64()
    linuxX64()

    macosArm64()

    mingwX64()

    tvosArm64()
    tvosSimulatorArm64()

    wasmJs().nodejs()
    wasmWasi().nodejs()

    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()

    compilerOptions {
        freeCompilerArgs.add("-Xreturn-value-checker=full")
    }
}

if (project.findProperty("onlyLocal")?.toString()?.toBooleanStrict() != true) {
    mavenPublishing {
        publishToMavenCentral(automaticRelease = true)
        signAllPublications()
    }
} else {
    publishing {
        repositories {
            maven {
                name = "localPluginRepository"
                url = uri("${rootProject.projectDir.absolutePath}/build/local-plugin-repository")
            }
        }
    }
}

afterEvaluate {
    val publications = extensions.findByType(PublishingExtension::class.java)?.publications ?: return@afterEvaluate
    val platformPublication: MavenPublication? = publications.findByName("jvm") as? MavenPublication

    if (platformPublication != null) {
        lateinit var platformXml: XmlProvider
        platformPublication.pom?.withXml { platformXml = this }

        (publications.findByName("kotlinMultiplatform") as? MavenPublication)?.run {
            // replace pom
            pom.withXml {
                val xmlProvider = this
                val root = xmlProvider.asNode()
                // Remove the original content and add the content from the platform POM:
                root.children().toList().forEach { root.remove(it as Node) }
                platformXml.asNode().children().forEach { root.append(it as Node) }

                // Adjust the self artifact ID, as it should match the root module's coordinates:
                ((root.get("artifactId") as NodeList).get(0) as Node).setValue(artifactId)

                // Set packaging to POM to indicate that there's no artifact:
                root.appendNode("packaging", "pom")

                // Remove the original platform dependencies and add a single dependency on the platform
                // module:
                val dependencies = (root.get("dependencies") as NodeList).get(0) as Node
                dependencies.children().toList().forEach { dependencies.remove(it as Node) }
                val singleDependency = dependencies.appendNode("dependency")
                singleDependency.appendNode("groupId", platformPublication.groupId)
                singleDependency.appendNode("artifactId", platformPublication.artifactId)
                singleDependency.appendNode("version", platformPublication.version)
                singleDependency.appendNode("scope", "compile")
            }
        }

        tasks
            .matching { it.name == "generatePomFileForKotlinMultiplatformPublication" }
            .configureEach {
                dependsOn(
                    "generatePomFileFor${platformPublication.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}Publication"
                )
            }
    }
}
