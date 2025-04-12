package dev.esoterik.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.esoterik.rift.codec.jackson.AbstractJacksonPacket;

public class ExampleResponse extends AbstractJacksonPacket {

  private String playerLocation;

  @JsonCreator
  private ExampleResponse() {}

  public ExampleResponse(final String playerLocation) {
    this.playerLocation = playerLocation;
  }

  public String getPlayerLocation() {
    return playerLocation;
  }

  @Override
  public String toString() {
    return "ExampleResponse{" + "playerLocation='" + playerLocation + '\'' + '}';
  }
}
