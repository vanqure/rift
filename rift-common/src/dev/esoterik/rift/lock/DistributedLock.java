package dev.esoterik.rift.lock;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DistributedLock {

  default <T> @Nullable T supply(final @NotNull Supplier<T> supplier) throws RetryingException {
    return supply(Thread.currentThread().getId(), supplier);
  }

  <T> @Nullable T supply(long currentThreadId, @NotNull Supplier<@Nullable T> supplier)
      throws RetryingException;

  default void execute(final @NotNull Runnable task) throws RetryingException {
    execute(Thread.currentThread().getId(), task);
  }

  void execute(long currentThreadId, @NotNull Runnable task) throws RetryingException;

  default boolean tryOnce(final @NotNull Runnable task) throws RetryingException {
    return tryOnce(Thread.currentThread().getId(), task);
  }

  boolean tryOnce(long currentThreadId, @NotNull Runnable task) throws RetryingException;

  void forceRelease();
}
