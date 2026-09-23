package com.serranofp.builder.lambda

public fun <T, E> build(block: E.() -> Unit): T {
    throw IllegalArgumentException("should never be called at runtime")
}
