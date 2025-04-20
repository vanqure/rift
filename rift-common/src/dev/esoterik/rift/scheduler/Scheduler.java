package dev.esoterik.rift.scheduler;

import java.time.Duration;
import org.jetbrains.annotations.NotNull;

public abstract class Scheduler {

    public abstract @NotNull ScheduledTask schedule(@NotNull Runnable task, @NotNull Duration duration);
}
