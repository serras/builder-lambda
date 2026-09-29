// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

package foo.bar

import com.serranofp.builder.lambda.build

data class A(val length: Int, val title: String, val size: Int) {
    companion {
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var _length: Int? = null
        private var _outputDir: String? = null
        private var _title: String = ""
        private var _size: Int = 0

        fun setLength(length: Int): Builder { _length = length ; return this }
        fun setOutputDir(outputDir: String): Builder { _outputDir = outputDir ; return this }
        fun optTitle(title: String): Builder { _title = title ; return this }
        fun optSize(size: Int): Builder { _size = size ; return this }
        fun build(): A = A(_length!!, _title, _size)
    }
}

fun box(): String {
    val t = build<A, *> {
        length = 3
        outputDir = "./this"
    }
    val u = build<A, *> {
        outputDir = "./this"
        length = 3
    }
    val s = <!CONSTRUCTOR_ARGS_MISSING!>build<!><A, *> {
        length = 3
        title = "hello"
        <!CONSTRUCTOR_ARG_GO_FIRST!>length<!> = 5
    }
    return if (s.length == 3 && s.title == "hello") "OK" else "FAIL"
}
