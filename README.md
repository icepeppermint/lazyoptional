# LazyOptional

> Try using the lazy Java Optional

[![build](https://github.com/icepeppermint/lazyoptional/actions/workflows/gradle.yml/badge.svg?branch=develop)](https://github.com/icepeppermint/lazyoptional/actions/workflows/gradle.yml)
<a href="https://github.com/icepeppermint/lazyoptional/contributors"><img src="https://img.shields.io/github/contributors/icepeppermint/lazyoptional.svg"></a>
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

_LazyOptional_ is a super lightweight Optional implementation that supports lazy evaluation. You can very easily use LazyOptional as a complement to Java Optional.

## Why do we need LazyOptional?

Java Optional is a tool that helps with null-safety, and unlike modern languages where it is included in the language spec itself, it is added as a library format to ensure backward compatibility.

The usage of the Java Optional class is not difficult, so let's skip it and look at the problem directly.

```java
Object nullValue = null;
Optional.ofNullable(nullValue)
        .map(v -> throw new IllegalStateException());
```

What would be the result of running the above code? An `IllegalStateException` exception occurs.

Then what about this?

```java
Object nullValue = null;
Stream.of(nullValue)
      .map(v -> throw new IllegalStateException());
```

No exception occurs. Nothing happens. This is because the map operator in Java Stream is not a terminal operation. Java Stream enables declarative programming through lazy evaluation.

Then what about Java Optional? Generally, operations like filter, map, flatMap, distinct, sorted, peek, limit, skip are intermediate operations. However, Java Optional behaves like terminal operations for these. One thing to note here is that it is not a problem in itself to behave eagerly or lazily in certain situations or not. Being lazy is not always good. Conversely, being eager is not always bad. The problem is that it does not behave as we thought, that is, it does not behave sensibly.

Let's dive into the LazyOptional.

## Getting started

Let's create an empty LazyOptional and run it.
```java
LazyOptional.empty();
```
The above code does nothing. Then what about the code below?
```java
Object nullValue = null;
LazyOptional.ofNullable(nullValue)
            .map(v -> throw new IllegalStateException());
```
Likewise, nothing happens. That is, `IllegalStateException` does not occur.
This is because LazyOptional supports lazy evaluation.

Then, does LazyOptional work well with the existing Java Optional?

```java
// Java Optional -> LazyOptional
LazyOptional.from(Optional.of(1));

// LazyOptional -> Java Optional
LazyOptional.of(1).optional();
```

What about Java Optional -> Java Stream? In Java 8, to do this, you had to manually extract the value inside Java Optional and pass that value when creating Java Stream, which was cumbersome.

This was added in Java 9, and LazyOptional also supports it.
```java
// Java 8's Optional -> Java Stream
Stream.of(Optional.of(1).orElseThrow(...));

// Java 9's Optional -> Java Stream
Optional.of(1).stream();

// LazyOptional -> Java Stream
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

## Contributors
See [the complete list of our contributors](https://github.com/icepeppermint/lazyoptional/contributors).

<a href="https://github.com/icepeppermint/lazyoptional/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=icepeppermint/lazyoptional" />
</a>

## License
```
  MIT License

  Copyright (c) 2024 icepeppermint

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
```
See [LICENSE](LICENSE) for more details.
