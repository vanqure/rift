package io.github.rift.map;

import io.github.rift.codec.Packet;

public interface CachedMapUpdate extends Packet {

    String getKey();

    String getValue();
}
