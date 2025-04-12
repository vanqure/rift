package dev.esoterik.rift.cache;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.CompletableFuture;

public class AsyncCaffeineCacheProvider<K, V> implements CacheProvider<K, V> {

  private final AsyncCache<K, V> cache;

  public AsyncCaffeineCacheProvider(final AsyncCache<K, V> cache) {
    this.cache = cache;
  }

  public AsyncCaffeineCacheProvider() {
    this(Caffeine.newBuilder().buildAsync());
  }

  @Override
  public V get(final K key) {
    final CompletableFuture<V> future = cache.getIfPresent(key);
    if (future == null) {
      return null;
    }
    return future.join();
  }

  @Override
  public void put(final K key, final V value) {
    cache.put(key, completedFuture(value));
  }

  @Override
  public void remove(final K key) {
    cache.synchronous().invalidate(key);
  }

  @Override
  public void clear() {
    cache.synchronous().invalidateAll();
  }

  @Override
  public boolean containsKey(final K key) {
    return get(key) != null;
  }

  @Override
  public boolean isEmpty() {
    return cache.asMap().isEmpty();
  }

  @Override
  public int size() {
    return cache.asMap().size();
  }

  @Override
  public Iterable<K> keys() {
    return unmodifiableSet(cache.asMap().keySet());
  }

  @Override
  public Iterable<V> values() {
    return unmodifiableCollection(cache.synchronous().asMap().values());
  }

  @Override
  public Iterable<Entry<K, V>> entries() {
    return cache.synchronous().asMap().entrySet().stream()
        .map(entry -> new DefaultEntry<>(entry.getKey(), entry.getValue()))
        .collect(toList());
  }
}
