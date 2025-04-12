package dev.esoterik.example;

import dev.esoterik.rift.RiftClient;
import dev.esoterik.rift.cache.AsyncCaffeineCacheProvider;
import dev.esoterik.rift.codec.jackson.JacksonPacket;
import dev.esoterik.rift.codec.jackson.JacksonSerializable;
import dev.esoterik.rift.map.CachedMap;
import java.util.UUID;

public class ExampleCachedMap {

  private final CachedMap<JacksonSerializable, JacksonCachedMapUpdate, UUID, ExamplePojo>
      redisCachedMap;

  public ExampleCachedMap(final RiftClient<JacksonSerializable, JacksonPacket> riftClient) {
    this.redisCachedMap =
        riftClient.getCachedMap(
            "my_map_name", new AsyncCaffeineCacheProvider<>(), JacksonCachedMapUpdate::new);
    test();
  }

  private void test() {
    final UUID uniqueId = UUID.randomUUID();
    final ExamplePojo value = new ExamplePojo(uniqueId);
    value.getData().put("health", "20");
    System.out.println("current data: " + value.getData());
    redisCachedMap.set(uniqueId, value);

    final ExamplePojo fetched = redisCachedMap.get(uniqueId);
    System.out.println("fetched data: " + fetched.getData());

    redisCachedMap.del(uniqueId);

    redisCachedMap.keys().forEach(System.out::println);
    redisCachedMap.values().forEach(System.out::println);
  }
}
