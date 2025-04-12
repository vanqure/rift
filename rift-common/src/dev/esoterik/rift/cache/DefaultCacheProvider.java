package dev.esoterik.rift.cache;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultCacheProvider<K, V> implements CacheProvider<K, V> {

  private final Map<K, V> cache = new ConcurrentHashMap<>();

  @Override
  public V get(final K key) {
    return cache.get(key);
  }

  @Override
  public void put(final K key, final V value) {
    cache.put(key, value);
  }

  @Override
  public void remove(final K key) {
    cache.remove(key);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  @Override
  public boolean containsKey(final K key) {
    return cache.containsKey(key);
  }

  @Override
  public boolean isEmpty() {
    return cache.isEmpty();
  }

  @Override
  public int size() {
    return cache.size();
  }

  @Override
  public Iterable<K> keys() {
    return unmodifiableSet(cache.keySet());
  }

  @Override
  public Iterable<V> values() {
    return unmodifiableCollection(cache.values());
  }

  @Override
  public Iterable<Entry<K, V>> entries() {
    return cache.entrySet().stream()
        .map(entry -> new DefaultEntry<>(entry.getKey(), entry.getValue()))
        .collect(toList());
  }
}
