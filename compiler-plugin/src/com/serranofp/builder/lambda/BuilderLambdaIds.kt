package com.serranofp.builder.lambda

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data object BuilderLambdaIds {
    val PACKAGE = FqName.fromSegments(["com","serranofp","builder","lambda"])

    val BUILDER_FUNCTION_ID = CallableId(
        PACKAGE,
        Name.identifier("builder")
    )

    val WITH_FUNCTION_ID = CallableId(
        PACKAGE,
        Name.identifier("with")
    )

    val CLASS_ID = ClassId(
        PACKAGE,
        Name.identifier("Builder")
    )
}