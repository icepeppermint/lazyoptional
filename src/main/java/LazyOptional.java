import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@FunctionalInterface
public interface LazyOptional<T> {

    Lazy<T> container();

    static <T> LazyOptional<T> of(final T value) {
        requireNonNull(value, "value");

        return () -> Lazy.lazy(() -> value);
    }

    static <T> LazyOptional<T> ofNullable(final T value) {
        return value == null ? empty() : of(value);
    }

    static <T> LazyOptional<T> empty() {
        return () -> Lazy.lazy(() -> null);
    }

    static <T> LazyOptional<T> fromOptional(final Optional<T> optional) {
        requireNonNull(optional, "optional");

        return optional.map(LazyOptional::ofNullable).orElse(empty());
    }

    default Optional<T> optional() {
        final T value = container().get();
        return value == null ? Optional.empty() : Optional.of(value);
    }

    default Stream<T> stream() {
        final T value = container().get();
        return value == null ? Stream.empty() : Stream.of(value);
    }

    default LazyOptional<T> filter(final Predicate<? super T> predicate) {
        requireNonNull(predicate, "predicate");

        return () -> Lazy.lazy(() -> {
            final T value = container().get();
            return value == null || !predicate.test(value) ? null : value;
        });
    }

    default <R> LazyOptional<R> map(final Function<? super T, ? extends R> mapper) {
        requireNonNull(mapper, "mapper");

        return () -> Lazy.lazy(() -> {
            final T value = container().get();
            return value == null ? null : mapper.apply(value);
        });
    }

    default <R> LazyOptional<R> flatMap(final Function<? super T, LazyOptional<R>> mapper) {
        requireNonNull(mapper, "mapper");

        return () -> Lazy.lazy(() -> {
            final T value = container().get();
            return value == null ? null : mapper.apply(value).container().get();
        });
    }

    default <X extends Throwable> LazyOptional<T> throwIf(
            final Predicate<? super T> predicate,
            final Supplier<? extends X> exceptionSupplier) {
        requireNonNull(predicate, "predicate");
        requireNonNull(exceptionSupplier, "exceptionSupplier");

        return () -> Lazy.lazy(() -> {
            final T value = container().get();
            return predicate.test(value) ? rethrow(exceptionSupplier.get()) : value;
        });
    }

    default LazyOptional<T> or(final Supplier<? extends LazyOptional<T>> supplier) {
        requireNonNull(supplier, "supplier");

        return () -> Lazy.lazy(() -> {
            final T value = container().get();
            if (value == null) {
                final LazyOptional<T> other = supplier.get();
                return other.container().get();
            } else {
                return value;
            }
        });
    }

    default <U, R> LazyOptional<R> zip(final LazyOptional<? extends U> other,
            final BiFunction<? super T, ? super U, R> zipper) {
        return zip(this, other, zipper);
    }

    static <A, B, R> LazyOptional<R> zip(
            final LazyOptional<? extends A> lazyOptionalA,
            final LazyOptional<? extends B> lazyOptionalB,
            final BiFunction<? super A, ? super B, R> zipper) {
        requireNonNull(lazyOptionalA, "lazyOptionalA");
        requireNonNull(lazyOptionalB, "lazyOptionalB");
        requireNonNull(zipper, "zipper");

        return () -> Lazy.lazy(() -> {
            final A valueA = lazyOptionalA.container().get();
            final B valueB = lazyOptionalB.container().get();

            return valueA == null || valueB == null ? null : zipper.apply(valueA, valueB);
        });
    }

    default T orElse(final T other) {
        final T value = container().get();
        return value == null ? other : value;
    }

    default T orElseGet(final Supplier<? extends T> other) {
        requireNonNull(other, "other");

        final T value = container().get();
        return value == null ? other.get() : value;
    }

    default T orElseThrow() {
        return orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    default <X extends Throwable> T orElseThrow(final Supplier<? extends X> exceptionSupplier) {
        requireNonNull(exceptionSupplier, "exceptionSupplier");

        final T value = container().get();
        return value == null ? rethrow(exceptionSupplier.get()) : value;
    }

    default T get() {
        final T value = container().get();
        return value == null ? rethrow(new NoSuchElementException("No value present")) : value;
    }

    default boolean isPresent() {
        final T value = container().get();
        return value != null;
    }

    default void ifPresent(final Consumer<? super T> consumer) {
        requireNonNull(consumer, "consumer");

        final T value = container().get();
        if (value != null) {
            consumer.accept(value);
        }
    }

    default void ifPresentOrElse(final Consumer<? super T> consumer, final Runnable other) {
        requireNonNull(consumer, "consumer");
        requireNonNull(other, "other");

        final T value = container().get();
        if (value == null) {
            other.run();
        } else {
            consumer.accept(value);
        }
    }

    @FunctionalInterface
    interface Lazy<T> {

        Supplier<T> supplier();

        default T get() {
            return supplier().get();
        }

        static <U> Lazy<U> lazy(final Supplier<U> supplier) {
            requireNonNull(supplier, "supplier");

            return () -> supplier;
        }
    }

    @SuppressWarnings("all")
    static <R> R rethrow(final Throwable throwable) {
        return LazyOptional.<R, RuntimeException>typeErasure(throwable);
    }

    @SuppressWarnings("unchecked")
    static <R, T extends Throwable> R typeErasure(final Throwable throwable) throws T {
        throw (T) throwable;
    }
}
