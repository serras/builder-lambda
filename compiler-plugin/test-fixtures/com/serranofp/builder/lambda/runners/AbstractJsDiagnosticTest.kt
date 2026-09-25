package com.serranofp.builder.lambda.runners

import com.serranofp.builder.lambda.services.configurePlugin
import org.jetbrains.kotlin.js.test.runners.AbstractJsDiagnosticWithBackendTestBase
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractJsDiagnosticTest : AbstractJsDiagnosticWithBackendTestBase(FirParser.LightTree) {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)
        /*
         * Containers of different directives, which can be used in tests:
         * - ModuleStructureDirectives
         * - LanguageSettingsDirectives
         * - DiagnosticsDirectives
         * - FirDiagnosticsDirectives
         *
         * All of them are located in `org.jetbrains.kotlin.test.directives` package
         */
        defaultDirectives {
            +FirDiagnosticsDirectives.DISABLE_GENERATED_FIR_TAGS
        }

        configurePlugin()
    }
}
