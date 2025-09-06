package io.github.rift.redis.lock;

import io.github.rift.lock.DistributedLock;
import io.github.rift.lock.RetryingException;
import io.github.rift.redis.map.KeyValue;
import io.github.rift.scheduler.Scheduler;
import java.time.Duration;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RedisLock implements DistributedLock {

    private final RedisLockWatcher watcher;
    private final RedisLockExecutor executor;

    private RedisLock(
            Scheduler scheduler,
            String key,
            String identity,
            Duration delay,
            Duration until,
            int tries,
            KeyValue keyValue) {
        this.watcher = new RedisLockWatcher(scheduler, identity, key, until, keyValue);
        this.executor = new RedisLockExecutor(delay, until, tries);
    }

    public static DistributedLock create(
            Scheduler scheduler,
            String key,
            String identity,
            Duration delay,
            Duration until,
            int tries,
            KeyValue keyValue) {
        return new RedisLock(scheduler, key, identity, delay, until, tries, keyValue);
    }

    @Override
    public <T> @Nullable T supply(long currentThreadId, @NotNull Supplier<@Nullable T> supplier)
            throws RetryingException {
        return executor.supply(() -> {
            boolean acquired = false;
            try {
                watcher.acquireOrThrow(currentThreadId);
                acquired = true;

                watcher.startWatching();
                try {
                    return supplier.get();
                } finally {
                    watcher.stopWatching();
                }
            } finally {
                if (acquired) {
                    watcher.release(currentThreadId);
                }
            }
        });
    }

    @Override
    public void execute(long currentThreadId, @NotNull Runnable task) throws RetryingException {
        executor.execute(() -> {
            boolean acquired = false;
            try {
                watcher.acquireOrThrow(currentThreadId);
                acquired = true;

                watcher.startWatching();
                try {
                    task.run();
                } finally {
                    watcher.stopWatching();
                }
            } finally {
                if (acquired) {
                    watcher.release(currentThreadId);
                }
            }
        });
    }

    @Override
    public boolean tryOnce(long currentThreadId, @NotNull Runnable task) throws RetryingException {
        return executor.supply(() -> {
            if (!watcher.acquire(currentThreadId)) {
                return false;
            }

            try {
                watcher.startWatching();
                try {
                    task.run();
                    return true;
                } finally {
                    watcher.stopWatching();
                }
            } finally {
                watcher.release(currentThreadId);
            }
        });
    }

    @Override
    public void forceRelease() {
        watcher.forceRelease();
    }
}
