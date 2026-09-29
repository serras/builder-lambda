// LANGUAGE: +CompanionBlocksAndExtensions +CollectionLiterals

package foo.bar

import com.serranofp.builder.lambda.build

data class A(val length: Int, val title: String, val size: Int, val thing: B?) {
    companion {
        fun builder(length: Int): Builder = Builder(length)
    }

    class Builder(val length: Int) {
        private var _title: String = ""
        private var _size: Int = 0
        private var _thing: B? = null

        fun title(title: String): Builder { _title = title ; return this }
        fun size(size: Int): Builder { _size = size ; return this }
        fun thing(thing: B): Builder { _thing = thing ; return this }
        fun build(): A = A(length, _title, _size, _thing)
    }
}

data class B(val authors: List<String>) {
    companion {
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var _authors: MutableList<String> = mutableListOf()

        fun authors(authors: List<String>): Builder {
            _authors = authors.toMutableList()
            return this
        }
        fun author(author: String): Builder {
            _authors.add(author)
            return this
        }
        fun build(): B = B(_authors)
    }
}

fun box(): String {
    val s = build<A, *> {
        length = 3
        title = "hello"
        thing = build<B, *> {
            author("me")
            author("you")
        }
    }
    return if (s.length == 3 && s.title == "hello" && s.thing?.authors == ["me", "you"]) "OK" else "FAIL"
}
