package io.github.rift.serializer;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractPacket implements Packet {

    private String replyTo;

    protected AbstractPacket() {}

    @Override
    public String getReplyTo() {
        return replyTo;
    }

    @Override
    public void setReplyTo(@NotNull String target) {
        this.replyTo = target;
    }
}
