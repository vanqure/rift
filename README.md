## Rift (*szczelina*)

A high-performance, extensible messaging and caching framework designed for distributed systems. It
brings seamless synchronization, intelligent caching, and fine-grained control over messaging,
locking, and state management. With built-in support for Redis and powerful abstraction layers, Rift
is the perfect solution for real-time applications requiring low latency, atomic operations, and
smooth scaling.

### Intent

*dew* was created to allow for developers to create modular events, which would work across many
platforms.

### Get started

- **Pub/Sub Messaging**: Rift allows you to create efficient and reactive messaging systems with
  built-in publish-subscribe capabilities. Whether you're sending notifications, events, or data to
  multiple consumers, Rift guarantees the seamless delivery of messages across your distributed
  architecture.

- **Distributed Locking**: Manage critical sections and ensure safe concurrent access with powerful
  distributed locks. Every Redis-backed map in Rift can be locked and unlocked programmatically,
  ensuring atomic operations with minimal contention.

- **Flexible Caching Strategies**:
    - **RedisCachedMap**: A high-performance cache with configurable codecs to store and retrieve
      complex objects, enhancing performance without unnecessary network roundtrips to Redis.
    - **RedisExpiringCachedMap**: Like the RedisCachedMap but with automatic expiration after a
      specified period (e.g., 15 seconds). Perfect for transient data that doesn’t need to persist
      longer than necessary.
    - **RedisKeyValue**: A straightforward key-value store that allows atomic operations with
      options like `onlyifnotexists` and time-to-live, perfect for settings and flags.
    - **RedisMap**: A raw, no-frills string-to-string map for situations where you need simple Redis
      access without caching or complex operations.

- **Atomic Operations**: Perform atomic operations on Redis data structures, with strategies for *
  *INVALIDATE** or **FETCH** ensuring data consistency and the ability to refresh data when needed.

- **Advanced Message Handlers**: Rift allows you to easily define message handlers to listen to
  incoming packets and trigger actions based on your custom business logic. By using
  `@PacketHandler`, you can efficiently listen to different topics (e.g., "queue") and perform
  complex actions like updating caches, sending requests, and more.

### Sample Usage

```java
final class QueueHandler implements PacketSubscriber {

  @Override
  public String topic() {
    return "queue";
  }

  @PacketHandler
  void handle(final EnqueuePacket packet) {
    final UUID uniqueId = packet.getUniqueId();
    System.out.println("Enqueueing entity with ID: " + uniqueId);
  }

  @PacketHandler
  void handle(final KillPacket packet) {
    final UUID uniqueId = packet.getUniqueId();
    System.out.println("Killing entity with ID: " + uniqueId);
  }

  @PacketHandler
  ExampleResponse handle(final ExampleRequest request) {
    if (request.getPlayerId() == null) {
      return null; // condition not met, do not process, just remember to handle future's throwable
      // from a timeout!
    }
    return (ExampleResponse) new ExampleResponse(request.getPlayerId() + "'s location is 0 3 3").pointAt(request);
  }
}
```

### Locking & Safety

Each of the Redis maps in Rift comes with its own set of locks. By calling the `#getLock(key)`
method on any of the maps, you gain full control over the lock lifecycle, ensuring that your
operations are thread-safe and consistent across distributed systems.

- **Queue Handling**: As illustrated in the sample, you can listen for packets (such as
  `EnqueuePacket`) and trigger specific actions like adding entries to queues, ensuring operations
  are always handled atomically.
- **Connection Management**: Integration with the proxy server ensures you can handle connection
  requests and maintain flow control seamlessly.

### Customizable Strategies

Rift’s flexible design allows you to define and customize strategies for cache invalidation and data
fetching:

- **INVALIDATE**: Automatically clear the cache when the underlying data changes.
- **FETCH**: Always fetch fresh data, ensuring the latest updates are available without relying on
  stale cache.

### Conclusion

Rift is designed for those building highly concurrent, distributed systems with complex caching,
messaging, and state management needs. With its focus on Redis-backed solutions, atomic operations,
distributed locks, and reactive message handling, Rift is your go-to framework for high-performance
systems that scale gracefully.

Whether you're building a gaming platform, a messaging service, or an event-driven application, Rift
delivers the tools you need to ensure speed, consistency, and reliability across all your
distributed components.
