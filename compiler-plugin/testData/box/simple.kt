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
        fun build(): A = TODO()
    }
}

fun test() {
    val s = build<A, *> {
        length = 3
        title = "hello"
    }
}