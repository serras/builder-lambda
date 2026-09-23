// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

package foo.bar

import com.serranofp.builder.lambda.builder
import com.serranofp.builder.lambda.with

class A {
    companion {
        fun builder(length: Int): Builder = TODO()
    }

    class Builder(val length: Int) {
        fun title(title: String): Builder = this
        fun size(size: Int): Builder = this
    }
}

fun test() {
    val s = builder<A>().with {
        length = 3
        title = "hello"
        title <!ASSIGNMENT_TYPE_MISMATCH!>=<!> false
        <!UNRESOLVED_REFERENCE!>other<!> = 3
    }
}
