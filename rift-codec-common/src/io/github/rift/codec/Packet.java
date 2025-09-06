package io.github.rift.codec;

import java.io.Serializable;
import org.jetbrains.annotations.NotNull;

public interface Packet extends Serializable {

    String source();

    void source(@NotNull String source);

    String target();

    Packet pointAt(@NotNull Packet request);
}
