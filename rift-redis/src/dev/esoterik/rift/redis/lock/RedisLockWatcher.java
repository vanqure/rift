package dev.esoterik.rift.redis.lock;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.logging.Level.SEVERE;

import dev.esoterik.rift.lock.DistributedLockException;
import dev.esoterik.rift.redis.map.KeyValue;
import dev.esoterik.rift.scheduler.ScheduledTask;
import dev.esoterik.rift.scheduler.Scheduler;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

final class RedisLockWatcher {

  private static final Logger logger = Logger.getLogger(RedisLockWatcher.class.getSimpleName());

  private final Scheduler scheduler;
  private final String identity;
  private final String key;
  private final Duration until;
  private final KeyValue keyValue;
  private final AtomicReference<ScheduledTask> scheduledTask = new AtomicReference<>();
  private final AtomicBoolean acquired = new AtomicBoolean(false);

  private final Map<Long, Integer> threadLockCount = new ConcurrentHashMap<>();
  private final AtomicInteger totalLockCount = new AtomicInteger(0);
  private final AtomicLong ownerThreadId = new AtomicLong(0);

  RedisLockWatcher(
      final Scheduler scheduler,
      final String identity,
      final String key,
      final Duration until,
      final KeyValue keyValue) {
    this.scheduler = scheduler;
    this.identity = identity;
    this.key = key;
    this.until = until;
    this.keyValue = keyValue;
  }

  void acquireOrThrow(final long currentThreadId) throws DistributedLockException {
    if (!acquire(currentThreadId)) {
      throw new DistributedLockException("Lock is already held by another process.");
    }
  }

  boolean acquire(final long currentThreadId) {
    // Check if current thread already holds the lock (reentrant case)
    if (isHeldByCurrentThread(currentThreadId)) {
      // Increment lock count for this thread
      final int count =
          threadLockCount.compute(
              currentThreadId, (id, lockCount) -> lockCount == null ? 1 : lockCount + 1);
      totalLockCount.incrementAndGet();
      logger.info(
          "Thread %s reacquired lock %s (count: %s)".formatted(currentThreadId, key, count));
      return true;
    }

    // Try to acquire the distributed lock
    final boolean result = keyValue.set(key, identity, until, true);
    if (result) {
      acquired.set(true);
      ownerThreadId.set(currentThreadId);
      threadLockCount.put(currentThreadId, 1);
      totalLockCount.set(1);
    }
    return result;
  }

  void release() {
    release(Thread.currentThread().getId());
  }

  void release(final long currentThreadId) {
    // Only the thread that acquired the lock can release it
    if (!isHeldByCurrentThread(currentThreadId)) {
      logger.warning(
          "Thread %s attempted to release lock %s held by thread %s"
              .formatted(currentThreadId, key, ownerThreadId.get()));
      return;
    }

    // Decrement lock count for this thread
    final int newCount =
        threadLockCount.compute(currentThreadId, (id, count) -> count == null ? 0 : count - 1);
    final int totalCount = totalLockCount.decrementAndGet();

    // If this thread has no more locks, remove it of the map
    if (newCount <= 0) {
      threadLockCount.remove(currentThreadId);
    }

    // If total count reaches zero, actually release the distributed lock
    if (totalCount <= 0 && acquired.getAndSet(false)) {
      keyValue.del(key);
      ownerThreadId.set(0); // Reset owner thread ID
    }
  }

  boolean isHeldByCurrentThread(final long currentThreadId) {
    return currentThreadId == ownerThreadId.get() && acquired.get();
  }

  int getLockCount() {
    return totalLockCount.get();
  }

  long getOwnerThreadId() {
    return ownerThreadId.get();
  }

  void startWatching() {
    stopWatching();

    scheduledTask.set(
        scheduler.schedule(
            () -> {
              try {
                if (acquired.get()) {
                  keyValue.ttl(key, now().plus(until));
                }
              } catch (final Exception exception) {
                logger.log(
                    SEVERE, "An error occurred while watching lock %s.".formatted(key), exception);
              }
            },
            ofSeconds(1L)));
  }

  void stopWatching() {
    Optional.ofNullable(scheduledTask.getAndSet(null)).ifPresent(ScheduledTask::cancel);
  }

  public void forceRelease() {
    synchronized (this) {
      stopWatching();
      acquired.set(false);
      keyValue.del(key);
      ownerThreadId.set(0);
      threadLockCount.clear();
      totalLockCount.set(0);
    }
  }
}
