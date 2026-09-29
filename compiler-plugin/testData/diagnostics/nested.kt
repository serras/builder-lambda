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
        fun thing(thing: B): Builder = this
        fun build(): A = TODO()
    }
}

class B {
    companion {
        fun builder(size: Int): Builder = TODO()
    }

    class Builder(val size: Int) {
        fun author(author: String): Builder = this
        fun build(): B = TODO()
    }
}

fun test() {
    val r = build<A, *> {
        length = 3
        thing = <!CONSTRUCTOR_ARGS_MISSING!>build<!><B, *> {
            author = "me"
            <!DSL_SCOPE_VIOLATION!>title<!> = "nested"
        }
    }
}
