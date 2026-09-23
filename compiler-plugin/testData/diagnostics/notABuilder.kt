// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

package foo.bar

import com.serranofp.builder.lambda.build

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
    val x = build<<!NOT_A_BUILDER!>A<!>, *> { }
    val s = build<<!NOT_A_BUILDER!>B<!>, *> {
        length = 3
        title = "hello"
    }
}
