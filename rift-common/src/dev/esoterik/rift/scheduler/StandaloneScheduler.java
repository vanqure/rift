package dev.esoterik.rift.scheduler;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public final class StandaloneScheduler extends Scheduler implements Closeable {

    private final ScheduledExecutorService scheduledExecutorService;

    private StandaloneScheduler(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public static Scheduler create(@NotNull ScheduledExecutorService scheduledExecutorService) {
        return new StandaloneScheduler(scheduledExecutorService);
    }

    public static Scheduler create() {
        return create(new ScheduledThreadPoolExecutor(0));
    }

    @Override
    public @NotNull ScheduledTask schedule(@NotNull Runnable task, @NotNull Duration duration) {
        ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(
                task, duration.toMillis(), duration.toMillis(), TimeUnit.MILLISECONDS);
        return new ScheduledTask() {
            @Override
            public void cancel() {
                future.cancel(false);
            }
        };
    }

    @Override
    public void close() {
        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException exception) {
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
