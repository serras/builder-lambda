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
        fun title(title: Int): Builder { _title = title.toString() ; return this }
        fun size(size: Int): Builder { _size = size ; return this }
        fun build(): A = A(length, _title, _size)
    }
}

fun box(): String {
    val s = build<A, *> {
        length = 3
        title("hello")
        title(1)
    }
    return if (s.length == 3 && s.title == "1") "OK" else "FAIL"
}