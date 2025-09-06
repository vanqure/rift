package io.github.rift.map;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.Stream;

public interface RiftMap<S extends Serializable, F, V extends S> {

    boolean set(F field, V value);

    V get(F field);

    boolean del(F field);

    Stream<F> fields();

    Stream<V> values();

    Map<F, V> entries();

    long size();
}
