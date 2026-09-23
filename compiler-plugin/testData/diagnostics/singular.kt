// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

package foo.bar

import com.serranofp.builder.lambda.build

class A {
    companion {
        fun builder(length: Int): Builder = TODO()
    }

    class Builder(val length: Int) {
        fun clearTitles(): Builder = this
        fun titles(titles: List<String>): Builder = this
        fun title(title: String): Builder = titles(listOf(title))
        fun size(size: Int): Builder = this
        fun author(name: String, title: String): Builder = this
        fun build(): A = TODO()
    }
}

fun test() {
    val s = build<A, *> {
        length = 3
        <!FUNCTION_CALL_EXPECTED, NO_VALUE_FOR_PARAMETER, VARIABLE_EXPECTED!>title<!> <!ASSIGNMENT_TYPE_MISMATCH!>=<!> "hello"
        title("hello")
        titles = listOf("hello")
        <!UNRESOLVED_REFERENCE!>clearTitles<!>()
        size = 3
        author("me", "mr.")
    }
}
