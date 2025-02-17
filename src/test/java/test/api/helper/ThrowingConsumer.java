package test.api.helper;

import java.util.function.Consumer;


@FunctionalInterface
public interface ThrowingConsumer<T>
        extends Consumer<T> {

    @Override
    default void accept(final T value) {

        try {

            acceptThrows(value);

        } catch (final Exception e) {

            throw new RuntimeException(e);
        }
    }

    void acceptThrows(T elem)
            throws Exception;
}
