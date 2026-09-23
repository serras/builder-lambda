package com.serranofp.builder.lambda

public interface Builder<out T> {
    public fun build(): T
}

public fun <T> builder(): Builder<T> {
    throw IllegalArgumentException("should never be called at runtime")
}

public fun <T, E: Builder<T>> E.with(block: E.() -> Unit): T {
    block(this)
    return this.build()
}
