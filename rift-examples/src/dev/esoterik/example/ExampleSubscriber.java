package dev.esoterik.example;

import dev.esoterik.rift.packet.PacketHandler;
import dev.esoterik.rift.packet.PacketSubscriber;
import java.util.concurrent.atomic.AtomicBoolean;

public record ExampleSubscriber(String topic, AtomicBoolean received) implements PacketSubscriber {

  @PacketHandler
  public void handle(final ExamplePacket packet) {
    received.set(true);
    System.out.println("Received packet: " + packet);
  }

  @PacketHandler
  public ExampleResponse handle(final ExampleRequest request) {
    if (request.getPlayerId() == null) {
      return null; // condition not met, do not process, just remember to handle future's throwable
      // from a timeout!
    }
    return (ExampleResponse)
        new ExampleResponse(request.getPlayerId() + "'s location is 0 3 3").pointAt(request);
  }
}
