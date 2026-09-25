# Buider Lambda Kotlin plug-in

> Java builders, the way Kotliners like them

> [!WARNING]
> The plug-in is not yet published in Maven Central or Gradle Marketplace.

The [builder pattern](https://projectlombok.org/features/Builder) is very common in the Java world.
However, using them in Kotlin usually leads to non-idiomatic code.

```kotlin
Config.builder()
      .hostname("localhost")
      .port(8080)
      .build()
```

Using this compiler plug-in, you get a much nicer syntax,

```kotlin
build<Config, *> {  // alas, the * is needed
    hostname = "localhost"
    port = 8080
}
```

Although the plug-in requires a supporting library for the `build` function,
this is a _compile-only_ dependency. The plug-in rewrites the call using `build`
to a sequence of calls on the builder. In other words, you write the code
in the second snippet, and get code generates as in the first one.

**Required arguments.**
Arguments required for the initial call to `build` are turned into _required_
arguments. Those required arguments must be present, and must  be given at 
the very beginning of the block, before anything else is set.

**Singular for collections.**
When the type of a property is a collection type, builders sometimes
provide [singular methods](https://projectlombok.org/features/Builder#singular)
for more quickly adding a value. The plug-in is aware of this pattern,
and exposes those as functions, instead of as setters.

```java
// 'authors' is a List<String>
Book.builder().title("Wow!").author("me").author("you").build()
```

```kotlin
build<Book, *> {
    title = "Wow!"
    author("me")
    author("you")
}
```