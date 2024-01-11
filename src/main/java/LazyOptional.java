import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An Optional implementation that supports laziness.
 */
@FunctionalInterface
public interface LazyOptional<T> {

    Container<T> container();

    static <T> LazyOptional<T> of(T value) {
        requireNonNull(value, "value");
        return () -> Container.wrap(() -> value);
    }

    static <T> LazyOptional<T> ofNullable(T value) {
        return value == null ? empty() : of(value);
    }

    static <T> LazyOptional<T> empty() {
        return () -> Container.wrap(() -> null);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> LazyOptional<T> from(Optional<T> optional) {
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

    default LazyOptional<T> filter(Predicate<? super T> predicate) {
        requireNonNull(predicate, "predicate");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            return value == null || !predicate.test(value) ? null : value;
        });
    }

    default <R> LazyOptional<R> map(Function<? super T, ? extends R> mapper) {
        requireNonNull(mapper, "mapper");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            return value == null ? null : mapper.apply(value);
        });
    }

    default <R> LazyOptional<R> flatMap(Function<? super T, LazyOptional<R>> mapper) {
        requireNonNull(mapper, "mapper");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            return value == null ? null : mapper.apply(value).container().get();
        });
    }

    default <X extends Throwable> LazyOptional<T> throwIf(Predicate<? super T> predicate,
                                                          Supplier<? extends X> exceptionSupplier) {
        requireNonNull(predicate, "predicate");
        requireNonNull(exceptionSupplier, "exceptionSupplier");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            return predicate.test(value) ? rethrow(exceptionSupplier.get()) : value;
        });
    }

    default LazyOptional<T> or(Supplier<? extends LazyOptional<T>> supplier) {
        requireNonNull(supplier, "supplier");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            if (value == null) {
                final LazyOptional<T> other = supplier.get();
                return other.container().get();
            } else {
                return value;
            }
        });
    }

    default <U, R> LazyOptional<R> zip(LazyOptional<? extends U> other,
                                       BiFunction<? super T, ? super U, R> zipper) {
        return zip(this, other, zipper);
    }

    static <A, B, R> LazyOptional<R> zip(LazyOptional<? extends A> lazyOptionalA,
                                         LazyOptional<? extends B> lazyOptionalB,
                                         BiFunction<? super A, ? super B, R> zipper) {
        requireNonNull(lazyOptionalA, "lazyOptionalA");
        requireNonNull(lazyOptionalB, "lazyOptionalB");
        requireNonNull(zipper, "zipper");
        return () -> Container.wrap(() -> {
            final A valueA = lazyOptionalA.container().get();
            final B valueB = lazyOptionalB.container().get();
            return valueA == null || valueB == null ? null : zipper.apply(valueA, valueB);
        });
    }

    default T orElse(T other) {
        final T value = container().get();
        return value == null ? other : value;
    }

    default T orElseGet(Supplier<? extends T> other) {
        requireNonNull(other, "other");
        final T value = container().get();
        return value == null ? other.get() : value;
    }

    default T orElseThrow() {
        return orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) {
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

    default void ifPresent(Consumer<? super T> consumer) {
        requireNonNull(consumer, "consumer");
        final T value = container().get();
        if (value != null) {
            consumer.accept(value);
        }
    }

    default void ifPresentOrElse(Consumer<? super T> consumer, Runnable other) {
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
    interface Container<T> {

        Supplier<T> supplier();

        default T get() {
            return supplier().get();
        }

        static <U> Container<U> wrap(Supplier<U> supplier) {
            requireNonNull(supplier, "supplier");
            return () -> supplier;
        }
    }

    static <R> R rethrow(Throwable e) {
        return LazyOptional.typeErasure(e);
    }

    @SuppressWarnings("unchecked")
    static <R, T extends Throwable> R typeErasure(Throwable e) throws T {
        throw (T) e;
    }
}
