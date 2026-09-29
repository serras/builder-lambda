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
        fun size(size: Int): Builder = this
        fun authors(authors: MutableList<String>): Builder = this
        fun build(): A = TODO()
    }
}

fun Any.f() { }

fun test() {
    val r = build<A, *> {
        title = "hello"
        <!CONSTRUCTOR_ARG_GO_FIRST!>length<!> = <!BUILDER_CANNOT_BE_READ!>title<!>.length
        <!BUILDER_CANNOT_BE_READ!>authors<!>[0] = "me"
    }
    val s = build<A, *> {
        length = 3
        title = "hello"
        title <!ASSIGNMENT_TYPE_MISMATCH!>=<!> false
        <!UNRESOLVED_REFERENCE!>other<!> = 3
    }
    val t = <!CONSTRUCTOR_ARGS_MISSING!>build<!><A, *> {
        title = "hello"
        title <!ASSIGNMENT_TYPE_MISMATCH!>=<!> false
        <!UNRESOLVED_REFERENCE!>other<!> = 3
    }
    val u = build<A, *>(<!MUST_USE_LAMBDA!>Any::f<!>)
}
