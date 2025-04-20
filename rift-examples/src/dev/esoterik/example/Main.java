package dev.esoterik.example;

import dev.esoterik.rift.RiftClient;
import dev.esoterik.rift.codec.Serializer;
import dev.esoterik.rift.codec.jackson.JacksonPacket;
import dev.esoterik.rift.codec.jackson.JacksonSerializable;
import dev.esoterik.rift.codec.jackson.JacksonSerializerFactory;
import dev.esoterik.rift.redis.RedisRiftClient;
import dev.esoterik.rift.scheduler.Scheduler;
import dev.esoterik.rift.scheduler.StandaloneScheduler;
import io.lettuce.core.RedisClient;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static void main(String[] args) {
        Serializer serializer = JacksonSerializerFactory.create();
        Scheduler scheduler = StandaloneScheduler.create();
        RiftClient<JacksonSerializable, JacksonPacket> riftClient =
                RedisRiftClient.create(serializer, scheduler, RedisClient.create("redis://localhost:6379"));

        AtomicBoolean received = new AtomicBoolean(false);
        final String topic = "example-topic";
        riftClient.subscribe(new ExampleSubscriber(topic, received));
        riftClient.publish(topic, new ExamplePacket("some message here"));

        while (!received.get()) {
            try {
                Thread.sleep(1000);
                System.out.println("Waiting for message...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Message received!");

        try {
            riftClient
                    .request(topic, new ExampleRequest("niczek301"))
                    .orTimeout(5, TimeUnit.SECONDS)
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            System.out.println("Error: " + error.getMessage());
                        } else {
                            System.out.println("Response: " + response);
                        }
                    })
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        new ExampleCachedMap(riftClient);

        try {
            riftClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
