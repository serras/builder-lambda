// LANGUAGE: +CompanionBlocksAndExtensions

package foo.bar

import com.serranofp.builder.lambda.build

data class A(val length: Int, val title: String, val size: Int) {
    companion {
        fun builder(length: Int): Builder = Builder(length)
    }

    class Builder(val length: Int) {
        private var _title: String = ""
        private var _size: Int = 0

        fun title(title: String): Builder { _title = title ; return this }
        fun size(size: Int): Builder { _size = size ; return this }
        fun build(): A = A(length, _title, _size)
    }
}

fun box1(condition: Boolean): A {
    val s = build<A, *> {
        length = 3
        title = if (condition) "hello" else "bye"
    }
    return s
}

fun box2(condition: Boolean): A {
    val s = build<A, *> {
        length = 3
        if (condition) { title = "hello" }
    }
    return s
}

fun box(): String {
    val s = box1(true)
    if (s.length != 3 || s.title != "hello") return "FAIL"

    val t = box2(true)
    if (t.length != 3 || t.title != "hello") return "FAIL"

    return "OK"
}