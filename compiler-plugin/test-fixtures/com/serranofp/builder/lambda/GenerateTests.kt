package com.serranofp.builder.lambda

import com.serranofp.builder.lambda.runners.AbstractJsBoxTest
import com.serranofp.builder.lambda.runners.AbstractJsDiagnosticTest
import com.serranofp.builder.lambda.runners.AbstractJvmBoxTest
import com.serranofp.builder.lambda.runners.AbstractJvmDiagnosticTest
import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testsRoot = args[0], testDataRoot = args[1]) {
            testClass<AbstractJvmDiagnosticTest> {
                model("diagnostics")
            }
            testClass<AbstractJsDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractJvmBoxTest> {
                model("box")
            }
            testClass<AbstractJsBoxTest> {
                model("box")
            }
        }
    }
}
