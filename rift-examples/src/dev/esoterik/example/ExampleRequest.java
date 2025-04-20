package dev.esoterik.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.esoterik.rift.codec.jackson.AbstractJacksonPacket;

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
