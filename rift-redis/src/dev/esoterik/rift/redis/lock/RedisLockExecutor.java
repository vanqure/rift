package dev.esoterik.rift.redis.lock;

import dev.esoterik.rift.lock.DistributedLockException;
import dev.esoterik.rift.lock.RetryingException;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

record RedisLockExecutor(Duration delay, Duration until, int tries) {

  <T> T supply(final Supplier<T> supplier) throws RetryingException {
    return supply(supplier, 0, Duration.ZERO);
  }

  private <T> T supply(
      final Supplier<T> supplier, final int retryCount, final Duration backoffDelay)
      throws RetryingException {
    if (tries != -1 && retryCount >= tries) {
      throw new RetryingException(retryCount);
    }

    try {
      if (retryCount > 0) {
        Thread.sleep(backoffDelay.toMillis());
      }

      return supplier.get();
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    } catch (final Exception exception) {
      if (exception instanceof DistributedLockException
          || exception.getCause() instanceof DistributedLockException) {
        return supply(supplier, retryCount + 1, calculateBackoffDelay(retryCount + 1));
      }

      if (exception instanceof final RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException(exception);
    }
  }

  void execute(final Runnable action) throws RetryingException {
    execute(action, 0, Duration.ZERO);
  }

  private void execute(final Runnable action, final int retryCount, final Duration backoffDelay)
      throws RetryingException {
    if (tries != -1 && retryCount >= tries) {
      throw new RetryingException(retryCount);
    }

    try {
      if (retryCount > 0) {
        Thread.sleep(backoffDelay.toMillis());
      }

      action.run();
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    } catch (final Exception exception) {
      if (exception instanceof DistributedLockException
          || exception.getCause() instanceof DistributedLockException) {
        execute(action, retryCount + 1, calculateBackoffDelay(retryCount + 1));
        return;
      }

      if (exception instanceof final RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException(exception);
    }
  }

  private Duration calculateBackoffDelay(final int retryCount) {
    final long exponentialDelayMillis =
        Math.min(delay.toMillis() * (1L << retryCount), until.toMillis());
    final long randomPart =
        exponentialDelayMillis / 2
            + ThreadLocalRandom.current().nextInt((int) (exponentialDelayMillis / 2));
    return Duration.ofMillis(randomPart);
  }
}
