// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

package foo.bar

import com.serranofp.builder.lambda.build

class A {
    companion {
        fun builder(length: Int): Builder = TODO()
    }

    class Builder(val length: Int) {
        fun title(title: String): Builder = this
        fun build(): A = TODO()
    }
}

fun Any.f() { }

fun test(condition: Boolean) {
    val r = <!CONSTRUCTOR_ARGS_MISSING!>build<!><A, *> {
        if (condition) { title = "hello" }
        while (condition) {
          <!CONSTRUCTOR_ARG_GO_FIRST!>length<!> = <!BUILDER_CANNOT_BE_READ!>title<!>.length
        }
    }
    val s = <!CONSTRUCTOR_ARGS_MISSING!>build<!><A, *> {
        if (condition) { <!CONSTRUCTOR_ARG_GO_FIRST!>length<!> = 3 }
        title = "hello"
    }
}
