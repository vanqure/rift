package io.github.rift.serializer;

import io.github.wisp.event.Event;
import java.io.Serializable;
import org.jetbrains.annotations.NotNull;

public interface Packet extends Event, Serializable {

    String getReplyTo();

    void setReplyTo(@NotNull String target);
}
