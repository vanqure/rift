package io.github.rift.demo;

import io.github.rift.RiftClient;
import io.github.rift.cache.AsyncCaffeineCacheProvider;
import io.github.rift.codec.jackson.JacksonPacket;
import io.github.rift.codec.jackson.JacksonSerializable;
import io.github.rift.map.CachedMap;
import java.util.UUID;

public class ExampleCachedMap {

    private final CachedMap<JacksonSerializable, JacksonCachedMapUpdate, UUID, ExamplePojo> redisCachedMap;

    public ExampleCachedMap(RiftClient<JacksonSerializable, JacksonPacket> riftClient) {
        this.redisCachedMap =
                riftClient.getCachedMap("my_map_name", new AsyncCaffeineCacheProvider<>(), JacksonCachedMapUpdate::new);
        test();
    }

    private void test() {
        UUID uniqueId = UUID.randomUUID();
        ExamplePojo value = new ExamplePojo(uniqueId);
        value.getData().put("health", "20");
        System.out.println("current data: " + value.getData());
        redisCachedMap.set(uniqueId, value);

        ExamplePojo fetched = redisCachedMap.get(uniqueId);
        System.out.println("fetched data: " + fetched.getData());

        redisCachedMap.del(uniqueId);

        redisCachedMap.keys().forEach(System.out::println);
        redisCachedMap.values().forEach(System.out::println);
    }
}
