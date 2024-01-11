package io.icepeppermint.lazyoptional;

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

    /**
     * Returns a newly created {@link LazyOptional}.
     *
     * @param value the value to wrap with {@link LazyOptional}.
     */
    static <T> LazyOptional<T> of(T value) {
        requireNonNull(value, "value");
        return () -> Container.wrap(() -> value);
    }

    /**
     * Returns a newly created {@link LazyOptional}.
     *
     * @param value the nullable value to wrap with {@link LazyOptional}.
     */
    static <T> LazyOptional<T> ofNullable(T value) {
        return value == null ? empty() : of(value);
    }

    /**
     * Returns a newly created empty {@link LazyOptional}.
     */
    static <T> LazyOptional<T> empty() {
        return () -> Container.wrap(() -> null);
    }

    /**
     * Returns a {@link LazyOptional} newly created by {@link Optional}.
     *
     * @param optional the {@link Optional} to wrap with {@link LazyOptional}.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> LazyOptional<T> from(Optional<T> optional) {
        requireNonNull(optional, "optional");
        return optional.map(LazyOptional::ofNullable).orElse(empty());
    }

    /**
     * Returns an {@link Optional} from {@link LazyOptional}.
     */
    default Optional<T> optional() {
        final T value = container().get();
        return value == null ? Optional.empty() : Optional.of(value);
    }

    /**
     * Returns a {@link Stream} from {@link LazyOptional}.
     */
    default Stream<T> stream() {
        final T value = container().get();
        return value == null ? Stream.empty() : Stream.of(value);
    }

    /**
     * Returns a {@link LazyOptional} with a filter operation.
     *
     * @param predicate the predicate to apply to a value, if present.
     */
    default LazyOptional<T> filter(Predicate<? super T> predicate) {
        requireNonNull(predicate, "predicate");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            return value == null || !predicate.test(value) ? null : value;
        });
    }

    /**
     * Returns a {@link LazyOptional} with a map operation.
     *
     * @param mapper the mapping function to apply to a value, if present.
     */
    default <R> LazyOptional<R> map(Function<? super T, ? extends R> mapper) {
        requireNonNull(mapper, "mapper");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            return value == null ? null : mapper.apply(value);
        });
    }

    /**
     * Returns a {@link LazyOptional} with a flatMap operation.
     *
     * @param mapper the mapping function to apply to a value, if present.
     */
    default <R> LazyOptional<R> flatMap(Function<? super T, LazyOptional<R>> mapper) {
        requireNonNull(mapper, "mapper");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            return value == null ? null : mapper.apply(value).container().get();
        });
    }

    /**
     * Returns a {@link LazyOptional} with a throwIf operation.
     *
     * @param predicate the predicate to apply to a value, if present.
     * @param exceptionSupplier the supplier for raising an exception when {@link Predicate#test(T)} is true.
     */
    default <X extends Throwable> LazyOptional<T> throwIf(Predicate<? super T> predicate,
                                                          Supplier<? extends X> exceptionSupplier) {
        requireNonNull(predicate, "predicate");
        requireNonNull(exceptionSupplier, "exceptionSupplier");
        return () -> Container.wrap(() -> {
            final T value = container().get();
            return predicate.test(value) ? rethrow(exceptionSupplier.get()) : value;
        });
    }

    /**
     * Returns a {@link LazyOptional} zipped with another {@link LazyOptional}.
     *
     * @param other the other {@link LazyOptional} to zip with.
     * @param zipper the zipping function to zip two {@link LazyOptional}s.
     */
    default <U, R> LazyOptional<R> zip(LazyOptional<? extends U> other,
                                       BiFunction<? super T, ? super U, R> zipper) {
        return zip(this, other, zipper);
    }

    /**
     * Returns a {@link LazyOptional} zipped by two {@link LazyOptional}.
     *
     * @param o1 the {@link LazyOptional} to zip with others.
     * @param o2 the {@link LazyOptional} to zip with others.
     * @param zipper the zipping function to zip two {@link LazyOptional}s.
     */
    static <A, B, R> LazyOptional<R> zip(LazyOptional<? extends A> o1,
                                         LazyOptional<? extends B> o2,
                                         BiFunction<? super A, ? super B, R> zipper) {
        requireNonNull(o1, "o1");
        requireNonNull(o2, "o2");
        requireNonNull(zipper, "zipper");
        return () -> Container.wrap(() -> {
            final A valueA = o1.container().get();
            final B valueB = o2.container().get();
            return valueA == null || valueB == null ? null : zipper.apply(valueA, valueB);
        });
    }

    /**
     * Returns a {@link LazyOptional} if it presents, otherwise returns an {@link LazyOptional} produced by supplying function.
     *
     * @param supplier the supplying function that produces an {@link LazyOptional} to be returned.
     */
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

    /**
     * Returns the value if it presents, otherwise returns other.
     *
     * @param other the value to be returned, if no value is present. May be null.
     */
    default T orElse(T other) {
        final T value = container().get();
        return value == null ? other : value;
    }

    /**
     * Returns the value if it presents, otherwise returns the result produced by the supplying function.
     *
     * @param other the supplying function that produces a value to be returned.
     */
    default T orElseGet(Supplier<? extends T> other) {
        requireNonNull(other, "other");
        final T value = container().get();
        return value == null ? other.get() : value;
    }

    /**
     * Returns the value if it presents, otherwise throws {@link NoSuchElementException}.
     */
    default T orElseThrow() {
        return orElseThrow(() -> new NoSuchElementException("No value present"));
    }

    /**
     * Returns the value if it presents, otherwise throws an exception produced by the exception supplying function.
     *
     * @param exceptionSupplier the supplying function that produces an exception to be thrown.
     */
    default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) {
        requireNonNull(exceptionSupplier, "exceptionSupplier");
        final T value = container().get();
        return value == null ? rethrow(exceptionSupplier.get()) : value;
    }

    /**
     * Returns the value if it presents, otherwise throws {@link NoSuchElementException}.
     */
    default T get() {
        final T value = container().get();
        return value == null ? rethrow(new NoSuchElementException("No value present")) : value;
    }

    /**
     * Returns whether the value presents.
     */
    default boolean isPresent() {
        final T value = container().get();
        return value != null;
    }

    /**
     * Performs the given action with the value if a value is present, otherwise does nothing.
     */
    default void ifPresent(Consumer<? super T> action) {
        requireNonNull(action, "action");
        final T value = container().get();
        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * Performs the given action with the value if a value is present, otherwise performs the given empty-based action.
     *
     * @param action the action to be performed, if a value is present
     * @param emptyAction the empty-based action to be performed, if no value is present.
     */
    default void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        requireNonNull(action, "action");
        requireNonNull(emptyAction, "emptyAction");
        final T value = container().get();
        if (value == null) {
            emptyAction.run();
        } else {
            action.accept(value);
        }
    }

    Container<T> container();

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
