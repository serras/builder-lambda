// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

package foo.bar

import com.serranofp.builder.lambda.builder
import com.serranofp.builder.lambda.with

class A { }

class B {
    companion {
        fun builder(length: Int): Builder = TODO()
    }

    class Builder(val length: Int) {
        fun title(title: String): Builder = this
        fun size(size: Int): Builder = this
    }
}

fun test() {
    val x = builder<<!NOT_A_BUILDER!>A<!>>().with { }
    val s = builder<<!NOT_A_BUILDER!>B<!>>().with {
        length = 3
        title = "hello"
    }
}
