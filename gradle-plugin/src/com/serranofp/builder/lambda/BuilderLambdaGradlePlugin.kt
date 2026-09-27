package com.serranofp.builder.lambda

import com.serranofp.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@Suppress("unused") // Used via reflection.
class BuilderLambdaGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("builderLambdaPlugin", BuilderLambdaGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        kotlinCompilation.defaultSourceSet.dependencies {
            // Add annotations library to all compilations.
            compileOnly(BuildConfig.ANNOTATIONS_LIBRARY_COORDINATES)
        }

        kotlinCompilation.compileTaskProvider.configure {
            // Run this compiler plugin before Compose plugin.
            it.compilerOptions.freeCompilerArgs.add("-Xcompiler-plugin-order=${BuildConfig.KOTLIN_PLUGIN_ID}>androidx.compose.compiler.plugins.kotlin")
            // Run this compiler plugin after Lombok plugin.
            it.compilerOptions.freeCompilerArgs.add("-Xcompiler-plugin-order=org.jetbrains.kotlin.lombok>${BuildConfig.KOTLIN_PLUGIN_ID}")
        }

        return project.provider {
            val extension = project.extensions.getByType(BuilderLambdaGradleExtension::class.java)

            emptyList()
        }
    }
}
