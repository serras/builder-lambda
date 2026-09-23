// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

package foo.bar

import com.serranofp.builder.lambda.Builder
import com.serranofp.builder.lambda.builder
import com.serranofp.builder.lambda.with

class A {
    companion {
        fun builder(length: Int): Builder = TODO()
    }

    class Builder(val length: Int) {
        fun title(title: String): Builder = this
        fun size(size: Int): Builder = this
        fun build(): A = TODO()
    }
}

fun Builder<A>.f(): Unit { }

fun test() {
    val b = builder<A>()
    val v = <!WITH_MUST_USE_BUILD!>b<!>.with { }
    val u = builder<A>().with(<!WITH_MUST_USE_LAMBDA!>Builder<A>::f<!>)
    val s = builder<A>().with {
        title = "hello"
        length = 3
    }
}
