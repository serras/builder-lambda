package com.serranofp.builder.lambda

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data object BuilderLambdaIds {
    val PACKAGE = FqName.fromSegments(["com", "serranofp", "builder", "lambda"])

    val BUILD_FUNCTION_NAME = Name.identifier("build")
    val BUILD_FUNCTION_ID = CallableId(PACKAGE, BUILD_FUNCTION_NAME)
    val BUILD_FUNCTION_FQNAME = PACKAGE.child(BUILD_FUNCTION_NAME)
}