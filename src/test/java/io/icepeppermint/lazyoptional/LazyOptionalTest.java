package io.icepeppermint.lazyoptional;

import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LazyOptionalTest {

    @Test
    void of() {
        LazyOptional.of(1);
        try {
            LazyOptional.<Integer>of(null);
            fail();
        } catch (NullPointerException ignored) {}
    }

    @Test
    void ofNullable() {
        LazyOptional.ofNullable(1);
        LazyOptional.<Integer>ofNullable(null);
    }

    @Test
    void empty() {
        assertThrows(NoSuchElementException.class, () -> LazyOptional.empty().orElseThrow());
    }

    @Test
    void from() {
        assertEquals(1, LazyOptional.from(Optional.of(1)).orElseThrow());
        try {
            LazyOptional.from(Optional.empty()).orElseThrow();
            fail();
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void from_laziness() {
        try {
            Optional.of(1).map(v -> {
                throw new IllegalStateException();
            });
            fail();
        } catch (IllegalStateException ignored) {}

        LazyOptional.from(Optional.of(1)).map(v -> {
            throw new IllegalStateException();
        });

        try {
            LazyOptional.from(Optional.of(1)).map(v -> {
                throw new IllegalStateException();
            }).get();
            fail();
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void optional() {
        assertEquals(1, LazyOptional.of(1).optional().orElseThrow(IllegalStateException::new));
        try {
            LazyOptional.empty().optional().orElseThrow(IllegalStateException::new);
            fail();
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void stream() {
        assertEquals(1L, LazyOptional.of(1).stream().count());
        assertEquals(0L, LazyOptional.empty().stream().count());
    }

    @Test
    void map_filter_flatMap() {
        assertEquals(2, LazyOptional.of(1)
                                    .map(v -> v + 1)
                                    .filter(v -> v % 2 == 0)
                                    .flatMap(LazyOptional::of)
                                    .orElseThrow());
    }

    @Test
    void filter_laziness() {
        LazyOptional.empty().filter(v -> false).filter(v -> false);
        try {
            LazyOptional.empty().filter(v -> false).filter(v -> false).orElseThrow();
            fail();
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void map_laziness() {
        LazyOptional.empty().map(identity()).map(identity());
        try {
            LazyOptional.empty().map(identity()).map(identity()).orElseThrow();
            fail();
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void flatMap_laziness() {
        LazyOptional.empty().flatMap(LazyOptional::of).flatMap(LazyOptional::of);
        try {
            LazyOptional.empty().flatMap(LazyOptional::of).flatMap(LazyOptional::of).orElseThrow();
            fail();
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void throwIf() {
        LazyOptional.empty().throwIf(Objects::isNull, IllegalStateException::new);
        try {
            LazyOptional.empty().throwIf(Objects::isNull, IllegalStateException::new).get();
        } catch (IllegalStateException ignored) {
        } catch (NoSuchElementException ignored) {
            fail();
        }
        assertEquals(2, LazyOptional.of(1)
                                    .map(v -> v + 1)
                                    .throwIf(v -> v < 2, () -> new IllegalStateException("v < 2"))
                                    .map(v -> v)
                                    .orElseThrow());

        try {
            LazyOptional.of(1)
                        .map(v -> v + 1)
                        .throwIf(v -> v <= 2, () -> new IllegalStateException("v <= 2"))
                        .map(v -> v)
                        .orElseThrow();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("v <= 2", e.getMessage());
        } catch (NoSuchElementException ignored) {
            fail();
        }

        try {
            LazyOptional.of(1)
                        .throwIf(v -> v < 1, () -> new IllegalStateException("v < 1"))
                        .map(v -> v + 1)
                        .throwIf(v -> v < 2, () -> new IllegalStateException("v < 2"))
                        .map(v -> v + 1)
                        .throwIf(v -> v < 3, () -> new IllegalStateException("v < 3"))
                        .map(v -> v + 1)
                        .throwIf(v -> v < 10000, () -> new IllegalStateException("v < 10000"))
                        .map(v -> v + 1)
                        .throwIf(v -> v < 20000, () -> new IllegalStateException("v < 20000"))
                        .get();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("v < 10000", e.getMessage());
        } catch (NoSuchElementException ignored) {
            fail();
        }
    }

    @Test
    void throwIf_laziness() {
        LazyOptional.empty()
                    .throwIf(v -> true, IllegalStateException::new)
                    .throwIf(v -> true, IllegalStateException::new);

        try {
            LazyOptional.empty()
                        .throwIf(v -> true, IllegalStateException::new)
                        .throwIf(v -> true, IllegalStateException::new)
                        .get();
            fail();
        } catch (IllegalStateException ignored) {
        } catch (NoSuchElementException ignored) {
            fail();
        }
    }

    @Test
    void or() {
        assertEquals(1, LazyOptional.<Integer>empty().or(() -> LazyOptional.of(1)).orElseThrow());
        assertEquals(1, LazyOptional.of(1).or(() -> LazyOptional.of(2)).orElseThrow());
    }

    @Test
    void or_laziness() {
        LazyOptional.<Integer>empty()
                    .or(LazyOptional::empty)
                    .or(LazyOptional::empty);
        LazyOptional.<Integer>empty()
                    .or(() -> LazyOptional.of(1))
                    .or(() -> LazyOptional.of(1));

        assertEquals(1, LazyOptional.<Integer>empty()
                                    .or(() -> LazyOptional.of(1))
                                    .or(() -> LazyOptional.of(1))
                                    .orElseThrow());

        try {
            LazyOptional.<Integer>empty().or(LazyOptional::empty).or(LazyOptional::empty).orElseThrow();
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void zip() {
        final LazyOptional<Integer> one = LazyOptional.of(1);
        final LazyOptional<Integer> two = LazyOptional.of(2);

        assertEquals(3, one.zip(two, Integer::sum).orElseThrow());
        assertEquals(3, LazyOptional.zip(one, two, Integer::sum).orElseThrow());

        final LazyOptional<Integer> empty = LazyOptional.empty();
        try {
            LazyOptional.zip(one, empty, Integer::sum).orElseThrow();
        } catch (NoSuchElementException ignored) {}

        try {
            LazyOptional.zip(empty, two, Integer::sum).orElseThrow();
        } catch (NoSuchElementException ignored) {}

        try {
            LazyOptional.zip(empty, empty, Integer::sum).orElseThrow();
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void zip_laziness() {
        LazyOptional.empty().zip(LazyOptional.empty(), (a, b) -> a).zip(LazyOptional.empty(), (a, b) -> a);
        LazyOptional.zip(LazyOptional.empty(), LazyOptional.empty(), (a, b) -> a);

        try {
            LazyOptional.empty().zip(LazyOptional.empty(), (a, b) -> a).zip(LazyOptional.empty(), (a, b) -> a)
                        .orElseThrow();
            fail();
        } catch (NoSuchElementException ignored) {}

        try {
            LazyOptional.zip(LazyOptional.empty(), LazyOptional.empty(), (a, b) -> a).orElseThrow();
            fail();
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void isPresent() {
        assertFalse(LazyOptional.empty().isPresent());
        assertTrue(LazyOptional.of(1).isPresent());
    }

    @Test
    void ifPresent() {
        LazyOptional.of(1).ifPresent(System.out::println);
        LazyOptional.empty().ifPresent(v -> fail());
    }

    @Test
    void ifPresentOrElse() {
        LazyOptional.of(1).ifPresentOrElse(System.out::println, Assertions::fail);
        LazyOptional.empty().ifPresentOrElse(System.out::println, () -> assertTrue(true));
    }
}
