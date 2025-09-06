package io.github.rift.demo;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.rift.serializer.jackson.AbstractJacksonPacket;

public class ExampleRequest extends AbstractJacksonPacket {

    private String playerId;

    @JsonCreator
    private ExampleRequest() {}

    public ExampleRequest(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    @Override
    public String toString() {
        return "ExampleRequest{" + "playerId='" + playerId + '\'' + '}';
    }
}
