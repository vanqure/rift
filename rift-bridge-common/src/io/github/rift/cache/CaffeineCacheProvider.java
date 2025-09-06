package io.github.rift.cache;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toList;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class CaffeineCacheProvider<K, V> implements CacheProvider<K, V> {

    private final Cache<K, V> cache;

    public CaffeineCacheProvider(Cache<K, V> cache) {
        this.cache = cache;
    }

    public CaffeineCacheProvider() {
        this(Caffeine.newBuilder().build());
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
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
        return unmodifiableCollection(cache.asMap().values());
    }

    @Override
    public Iterable<Entry<K, V>> entries() {
        return cache.asMap().entrySet().stream()
                .map(entry -> new DefaultEntry<>(entry.getKey(), entry.getValue()))
                .collect(toList());
    }
}
