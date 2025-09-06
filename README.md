## Rift

(not designed for production use, an old project of mine)

Rift is a library that provides distributed caching, locking, and synchronized map storage using a unified API.

### Why Rift?

- **Distributed Caching:** A unified interface supporting various implementations (e.g., AsyncCaffeine, Caffeine, or a simple ConcurrentHashMap).
- **Distributed Locking:** A fail-safe mechanism using watchers and executors, supporting retries and proper exception handling.
- **Cached Map:** A synchronized map that leverages local caches combined with a distributed store (similar to Redisson's local cached map).
- **RPC and Packet Communication:** Asynchronous request/response messaging via a `PacketBroker` using the `CompletableFuture` API for non-blocking remote procedure calls (RPC).


### Get started

You can build dependency and append it to your local .m2 directory, by using: `./gradlew publishToMavenLocal`

### Using Rift:

Go to the [Rift demo](/rift-demo) module for complete examples.

---

![Visitor Count](https://visitor-badge.laobi.icu/badge?page_id=vanqure.rift)
