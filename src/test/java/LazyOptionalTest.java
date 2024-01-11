import static java.util.function.Function.identity;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class LazyOptionalTest {

    @Test
    public void of() {
        LazyOptional.of(1);
        try {
            LazyOptional.<Integer>of(null);
            fail();
        } catch (final NullPointerException ignored) {}
    }

    @Test
    public void ofNullable() {
        LazyOptional.ofNullable(1);
        LazyOptional.<Integer>ofNullable(null);
    }

    @Test(expected = NoSuchElementException.class)
    public void empty() {
        LazyOptional.empty().orElseThrow();
    }

    @Test
    public void from() {
        assertThat(LazyOptional.from(Optional.of(1)).orElseThrow(), is(1));

        try {
            LazyOptional.from(Optional.empty()).orElseThrow();
            fail();
        } catch (final NoSuchElementException ignored) {}
    }

    @Test
    public void fromOptional_laziness() {
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
        } catch (final IllegalStateException ignored) {}
    }

    @Test
    public void optional() {
        assertThat(LazyOptional.of(1).optional().orElseThrow(IllegalStateException::new), is(1));

        try {
            LazyOptional.empty().optional().orElseThrow(IllegalStateException::new);
            fail();
        } catch (final IllegalStateException ignored) {}
    }

    @Test
    public void stream() {
        assertThat(LazyOptional.of(1).stream().count(), is(1L));
        assertThat(LazyOptional.empty().stream().count(), is(0L));
    }

    @Test
    public void map_filter_flatMap() {
        assertThat(
                LazyOptional.of(1)
                            .map(v -> v + 1)
                            .filter(v -> v % 2 == 0)
                            .flatMap(LazyOptional::of)
                            .orElseThrow(),
                is(2));
    }

    @Test
    public void filter_laziness() {
        LazyOptional.empty().filter(v -> false).filter(v -> false);

        try {
            LazyOptional.empty().filter(v -> false).filter(v -> false).orElseThrow();
            fail();
        } catch (final NoSuchElementException ignored) {}
    }

    @Test
    public void map_laziness() {
        LazyOptional.empty().map(identity()).map(identity());

        try {
            LazyOptional.empty().map(identity()).map(identity()).orElseThrow();
            fail();
        } catch (final NoSuchElementException ignored) {}
    }

    @Test
    public void flatMap_laziness() {
        LazyOptional.empty().flatMap(LazyOptional::of).flatMap(LazyOptional::of);

        try {
            LazyOptional.empty().flatMap(LazyOptional::of).flatMap(LazyOptional::of).orElseThrow();
            fail();
        } catch (final NoSuchElementException ignored) {}
    }

    @Test
    public void throwIf() {
        LazyOptional.empty().throwIf(Objects::isNull, IllegalStateException::new);

        try {
            LazyOptional.empty().throwIf(Objects::isNull, IllegalStateException::new).get();
        } catch (final IllegalStateException ignored) {} catch (final NoSuchElementException ignored) {
            fail();
        }

        assertThat(
                LazyOptional.of(1)
                            .map(v -> v + 1)
                            .throwIf(v -> v < 2, () -> new IllegalStateException("v < 2"))
                            .map(v -> v)
                            .orElseThrow(),
                is(2));

        try {
            LazyOptional.of(1)
                        .map(v -> v + 1)
                        .throwIf(v -> v <= 2, () -> new IllegalStateException("v <= 2"))
                        .map(v -> v)
                        .orElseThrow();
            fail();
        } catch (final IllegalStateException e) {
            assertThat(e.getMessage(), is("v <= 2"));
        } catch (final NoSuchElementException ignored) {
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
        } catch (final IllegalStateException e) {
            assertThat(e.getMessage(), is("v < 10000"));
        } catch (final NoSuchElementException ignored) {
            fail();
        }
    }

    @Test
    public void throwIf_laziness() {
        LazyOptional.empty().throwIf(v -> true, IllegalStateException::new).throwIf(v -> true,
                                                                                    IllegalStateException::new);

        try {
            LazyOptional.empty().throwIf(v -> true, IllegalStateException::new).throwIf(v -> true,
                                                                                        IllegalStateException::new)
                        .get();
            fail();
        } catch (final IllegalStateException ignored) {} catch (final NoSuchElementException ignored) {
            fail();
        }
    }

    @Test
    public void or() {
        assertThat(LazyOptional.<Integer>empty().or(() -> LazyOptional.of(1)).orElseThrow(), is(1));
        assertThat(LazyOptional.of(1).or(() -> LazyOptional.of(2)).orElseThrow(), is(1));
    }

    @Test
    public void or_laziness() {
        LazyOptional.<Integer>empty().or(LazyOptional::empty).or(LazyOptional::empty);
        LazyOptional.<Integer>empty().or(() -> LazyOptional.of(1)).or(() -> LazyOptional.of(1));

        assertThat(LazyOptional.<Integer>empty().or(() -> LazyOptional.of(1)).or(() -> LazyOptional.of(1))
                               .orElseThrow(), is(1));

        try {
            LazyOptional.<Integer>empty().or(LazyOptional::empty).or(LazyOptional::empty).orElseThrow();
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    public void zip() {
        final LazyOptional<Integer> one = LazyOptional.of(1);
        final LazyOptional<Integer> two = LazyOptional.of(2);

        assertThat(one.zip(two, Integer::sum).orElseThrow(), is(3));
        assertThat(LazyOptional.zip(one, two, Integer::sum).orElseThrow(), is(3));

        final LazyOptional<Integer> empty = LazyOptional.empty();
        try {
            LazyOptional.zip(one, empty, Integer::sum).orElseThrow();
        } catch (final NoSuchElementException ignored) {}

        try {
            LazyOptional.zip(empty, two, Integer::sum).orElseThrow();
        } catch (final NoSuchElementException ignored) {}

        try {
            LazyOptional.zip(empty, empty, Integer::sum).orElseThrow();
        } catch (final NoSuchElementException ignored) {}
    }

    @Test
    public void zip_laziness() {
        LazyOptional.empty().zip(LazyOptional.empty(), (a, b) -> a).zip(LazyOptional.empty(), (a, b) -> a);
        LazyOptional.zip(LazyOptional.empty(), LazyOptional.empty(), (a, b) -> a);

        try {
            LazyOptional.empty().zip(LazyOptional.empty(), (a, b) -> a).zip(LazyOptional.empty(), (a, b) -> a)
                        .orElseThrow();
            fail();
        } catch (final NoSuchElementException ignored) {}

        try {
            LazyOptional.zip(LazyOptional.empty(), LazyOptional.empty(), (a, b) -> a).orElseThrow();
            fail();
        } catch (final NoSuchElementException ignored) {}
    }

    @Test
    public void isPresent() {
        assertThat(LazyOptional.empty().isPresent(), is(false));
        assertThat(LazyOptional.of(1).isPresent(), is(true));
    }

    @Test
    public void ifPresent() {
        LazyOptional.of(1).ifPresent(System.out::println);
        LazyOptional.empty().ifPresent(v -> fail());
    }

    @Test
    public void ifPresentOrElse() {
        LazyOptional.of(1).ifPresentOrElse(System.out::println, Assert::fail);
        LazyOptional.empty().ifPresentOrElse(System.out::println, () -> assertThat(1, is(1)));
    }
}
