# LazyOptional

> Try using the lazy Java Optional

_LazyOptional_ is a super lightweight Optional implementation that supports lazy evaluation. You can very easily use LazyOptional as a complement to Java Optional.

## Why do we need LazyOptional?

Java Optional is a tool that helps with null-safety, and unlike modern languages where it is included in the language spec itself, it is added as a library format to ensure backward compatibility.

The usage of the Optional class is not difficult, so let's skip it and look at the problem directly.

```java
final Object nullValue = null;
Optional.ofNullable(nullValue)
        .map(v -> throw new IllegalStateException());
```

What would be the result of running the above code? An `IllegalStateException` exception occurs.

Then what about this?

```java
final Object nullValue = null;
Stream.of(nullValue)
      .map(v -> throw new IllegalStateException());
```

No exception occurs. Nothing happens. This is because the map operator in Stream is not a terminal operation. Stream enables declarative programming through lazy evaluation.

Then what about Java Optional? Generally, operations like filter, map, flatMap, distinct, sorted, peek, limit, skip are intermediate operations. However, Java Optional behaves like terminal operations for these. One thing to note here is that it is not a problem in itself to behave eagerly or lazily in certain situations or not. Being lazy is not always good. Conversely, being eager is not always bad. The problem is that it does not behave as we thought, that is, it does not behave sensibly.

Let's dive into the LazyOptional.

## Getting started

Let's create an empty LazyOptional and run it.
```java
LazyOptional.empty();
```
The above code does nothing. Then what about the code below?
```java
final Object nullValue = null;
LazyOptional.ofNullable(nullValue)
            .map(v -> throw new IllegalStateException());
```
Likewise, nothing happens. That is, `IllegalStateException` does not occur.
This is because LazyOptional supports lazy evaluation.

Then, does LazyOptional work well with the existing Optional?

```java
// Optional -> LazyOptional
LazyOptional.from(Optional.of(1));

// LazyOptional -> Optional
LazyOptional.of(1).optional();
```

What about Optional -> Stream? In Java 8, to do this, you had to manually extract the value inside Optional and pass that value when creating Stream, which was cumbersome.

This was added in Java 9, and LazyOptional also supports it.
```java
// Java 8's Optional -> Stream
Stream.of(Optional.of(1).orElseThrow(...));

// Java 9's Optional -> Stream
Optional.of(1).stream();

// LazyOptional -> Stream
LazyOptional.of(1).stream();
```

Java 9's Optional also added `or()`. LazyOptional also supports this.
```java
// Java 9's Optional.or()
Optional.of(1).or(() -> Optional.of(2));

// LazyOptional.or()
LazyOptional.of(1).or(() -> LazyOptional.of(2));
```

And it also supports most operations that exist in Java Optional.