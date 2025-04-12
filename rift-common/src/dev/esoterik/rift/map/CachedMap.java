package dev.esoterik.rift.map;

import java.io.Serializable;
import java.util.stream.Stream;

public interface CachedMap<S extends Serializable, P extends CachedMapUpdate, F, V extends S> {

  void set(F field, V value);

  V get(F field);

  void del(F field);

  Iterable<F> keys();

  Stream<V> values();

  long size();
}
