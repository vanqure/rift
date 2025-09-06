package io.github.rift.codec;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractPacket implements Packet {

    private String source;
    private String target;

    protected AbstractPacket() {}

    @Override
    public String source() {
        return source;
    }

    @Override
    public void source(@NotNull String source) {
        this.source = source;
    }

    @Override
    public String target() {
        return target;
    }

    @Override
    public Packet pointAt(@NotNull Packet request) {
        this.target = request.source();
        return this;
    }
}
