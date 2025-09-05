## Rift

Rift is a modular, backend-agnostic library that provides distributed caching, locking, and synchronized map storage using a unified API. Although the current implementation is based on Redis, the design anticipates additional backends (e.g., NATS) in future releases.

- **Distributed Caching:** A unified interface supporting various implementations (e.g., AsyncCaffeine, Caffeine, or a simple ConcurrentHashMap).
- **Distributed Locking:** A fail-safe mechanism using watchers and executors, supporting retries and proper exception handling.
- **Cached Map:** A synchronized map that leverages local caches combined with a distributed store (similar to Redisson's local cached map).
- **RPC and Packet Communication:** Asynchronous request/response messaging via a `PacketBroker` using the `CompletableFuture` API for non-blocking remote procedure calls (RPC).

---

## Components

### CacheProvider

The `CacheProvider<K, V>` interface defines a simple caching API that includes:

- **Basic Operations:** `get`, `put`, `remove`, `clear`
- **Bulk Queries:** `keys`, `values`, `entries`
- **Implementations:**
  - `AsyncCaffeineCacheProvider`
  - `CaffeineCacheProvider`
  - `DefaultCacheProvider` (backed by `ConcurrentHashMap`)

### DistributedLock

The `DistributedLock` interface manages distributed lock operations and supports the following methods:

- **`execute(Runnable task)`**  
  Executes a task within the lock.  
  _Example:_  
  ```java
  DistributedLock lock = riftClient.getLock("resource_key");
  lock.execute(() -> {
      // Critical section code.
  });
  ```
  
- **`supply(Supplier<T> supplier)`**  
  Executes a supplier within the locking context and returns its result. Overloaded versions take the current thread's ID.  
  _Example:_  
  ```java
  String result = lock.supply(() -> {
      // Perform work and return a result.
      return "Computed Result";
  });
  ```
  
- **`tryOnce(Runnable task)`**  
  Attempts to execute the provided task once, without any retries.  
  _Example:_  
  ```java
  boolean success = lock.tryOnce(() -> {
      // Try to execute once; check success.
  });
  ```
  
- **Force Release:**  
  `forceRelease()` immediately releases the lock even if errors occur.

In the current Redis-based implementation, a `RedisLockWatcher` and a `RedisLockExecutor` manage retries and lock lifetimes. Exceeding the maximum number of retries triggers a `RetryingException`.

### CachedMap

The `CachedMap<S extends Serializable, P extends CachedMapUpdate, F, V extends S>` provides a synchronized view of distributed data with local caching. It functions similarly to Redisson's local cached map by:

- **Synchronizing Operations:** `set`, `get`, `del`
- **Providing Iterators:** `keys`, `values`, and `size`
- **Broadcasting Updates:** Updates are published via a `PacketBroker` to keep all nodes in sync.

*Example usage:*
```java
CachedMap<JacksonSerializable, JacksonCachedMapUpdate, UUID, ExamplePojo> cachedMap =
    riftClient.getCachedMap("my_map_name", new AsyncCaffeineCacheProvider<>(), JacksonCachedMapUpdate::new);
cachedMap.set(uuid, new ExamplePojo(uuid));
ExamplePojo value = cachedMap.get(uuid);
cachedMap.del(uuid);
```

### RiftMap

The `RiftMap<S extends Serializable, F, V extends S>` interface is an abstraction for a namespaced key-field-value store. It is obtained via:
```java
<F, V extends S> @NotNull RiftMap<S, F, V> getMap(@NotNull String key);
```
This method, available through `RiftClient`, returns a backend-agnostic map instance. Note that `RiftMap` operations are synchronous.

### KeyValue

The `KeyValue` interface wraps basic Redis key-value operations, such as:

- **Setting Values:** With or without TTL, and conditional options (NX semantics).
- **Retrieving/Deleting:** Basic CRUD operations.
- **Atomic Operations:** Increment and decrement counters.

This interface is primarily used by the distributed locks but is available for general-purpose usage as well.

### Packet Communication

Rift uses a `PacketBroker<P extends Packet>` for both pub/sub messaging and asynchronous RPC-like communication.

- **Publish/Subscribe:**  
  Methods such as `publish` and `subscribe` broadcast messages across nodes.
  
- **Asynchronous RPC:**  
  RPC calls return a `CompletableFuture` that must be handled with both result and error management. Always use `orTimeout` and `whenComplete` to ensure proper handling.

### Scheduler

The `Scheduler` interface (with its default, `StandaloneScheduler`) is used currently only for lock watching system (it's prolonging etc.).
If you're using system like Spigot or other minecraft related framework, you can implement your own `Scheduler` via using BukkitScheduler#runTaskTimerAsynchronously and calculation of duration to ticks to avoid the creation of next ExecutorService.

---

## RPC and Timeout Handling

For RPC operations (which use `CompletableFuture`):

1. **Timeouts:**  
   Apply `orTimeout` to guard against hanging calls.  
   _Example:_
   ```java
   riftClient.<ExampleResponse>request("example-topic", new ExampleRequest("playerId"))
       .orTimeout(5, TimeUnit.SECONDS)
       .whenComplete((response, throwable) -> {
           if (throwable != null) {
               // Handle error (timeout or other issues)
           } else {
               // Process the response
           }
       });
   ```

2. **Error Management:**  
   Use `whenComplete` (instead of `thenAccept`) to process both successful results and exceptions.

*Note:* RPC timeout handling applies to asynchronous request/response patterns, not to synchronous `RiftMap` or `CachedMap` operations.

---

## Usage Examples

### Distributed Locking

```java
DistributedLock lock = riftClient.getLock("resource_key");

// Standard execution of a task
lock.execute(() -> {
    // Execute your critical section code here.
});

// Using supply() to obtain a result
String result = lock.supply(() -> {
    // Compute and return a result.
    return "Computed Result";
});

// Attempt to execute only once
boolean executedOnce = lock.tryOnce(() -> {
    // Attempt single execution.
});
```

### Working with a Cached Map

```java
// Obtain a cached map through RiftClient (synchronous API)
CachedMap<JacksonSerializable, JacksonCachedMapUpdate, UUID, ExamplePojo> cachedMap =
    riftClient.getCachedMap("my_map_name", new AsyncCaffeineCacheProvider<>(), JacksonCachedMapUpdate::new);

UUID key = UUID.randomUUID();
ExamplePojo value = new ExamplePojo(key);
cachedMap.set(key, value);

ExamplePojo fetched = cachedMap.get(key);
System.out.println("Fetched: " + fetched);

cachedMap.del(key);
```

### RPC Calls with Timeout Handling

```java
riftClient.request("example-topic", new ExampleRequest("playerId"))
    .orTimeout(5, TimeUnit.SECONDS)
    .whenComplete((response, throwable) -> {
        if (throwable != null) {
            System.err.println("RPC call failed: " + throwable.getMessage());
        } else {
            System.out.println("Received response: " + response);
        }
    });
```

### Example Packet and Subscriber

Below is an example of an implementation for a packet and its subscriber.  
This demonstrates how to create custom packet types and register a subscriber to handle incoming packets using the `@PacketHandler` annotation.

#### Example Packet

```java
// ExamplePacket.java
public class ExamplePacket extends AbstractJacksonPacket {
  
    private String exampleField;

    @JsonCreator
    private ExamplePacket() {}

    public ExamplePacket(final String exampleField) {
        this.exampleField = exampleField;
    }

    public String getExampleField() {
        return exampleField;
    }

    @Override
    public String toString() {
        return "ExamplePacket{" +
               "exampleField='" + exampleField + '\'' +
               '}';
    }
}
```

#### Example Request & Response Packets

```java
// ExampleRequest.java
public class ExampleRequest extends AbstractJacksonPacket {

    private String playerId;

    @JsonCreator
    private ExampleRequest() {}

    public ExampleRequest(final String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }

    @Override
    public String toString() {
        return "ExampleRequest{" +
               "playerId='" + playerId + '\'' +
               '}';
    }
}

// ExampleResponse.java
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
        return "ExampleResponse{" +
               "playerLocation='" + playerLocation + '\'' +
               '}';
    }
}
```

#### Example Subscriber

```java
// ExampleSubscriber.java
public record ExampleSubscriber(String topic, AtomicBoolean received) implements PacketSubscriber {

    @Override
    public String topic() {
        return topic;
    }

    @PacketHandler
    public void handle(final ExamplePacket packet) {
        received.set(true);
        System.out.println("Received packet: " + packet);
    }

    @PacketHandler
    public ExampleResponse handle(final ExampleRequest request) {
        if (request.getPlayerId() == null) {
            return null; // Do not process if the condition is not met.
        }
        return new ExampleResponse(request.getPlayerId() + "'s location is 0 3 3");
    }
}
```

In the above example, the subscriber registers two handler methods:
- One for a broadcast-type packet (`ExamplePacket`).
- One for RPC requests (`ExampleRequest`) that returns a corresponding response (`ExampleResponse`).

---

## License

[LICENSE](LICENSE)

---

![Visitor Count](https://visitor-badge.laobi.icu/badge?page_id=vanqure.rift)
