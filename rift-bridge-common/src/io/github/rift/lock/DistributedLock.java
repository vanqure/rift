package io.github.rift.lock;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DistributedLock {

    default <T> @Nullable T supply(@NotNull Supplier<T> supplier) throws RetryingException {
        return supply(Thread.currentThread().getId(), supplier);
    }

    default void execute(@NotNull Runnable task) throws RetryingException {
        execute(Thread.currentThread().getId(), task);
    }

    default boolean tryOnce(@NotNull Runnable task) throws RetryingException {
        return tryOnce(Thread.currentThread().getId(), task);
    }

    <T> @Nullable T supply(long currentThreadId, @NotNull Supplier<@Nullable T> supplier) throws RetryingException;

    void execute(long currentThreadId, @NotNull Runnable task) throws RetryingException;

    boolean tryOnce(long currentThreadId, @NotNull Runnable task) throws RetryingException;

    void forceRelease();
}
