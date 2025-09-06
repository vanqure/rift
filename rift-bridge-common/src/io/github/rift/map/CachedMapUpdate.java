package io.github.rift.map;

import io.github.rift.serializer.Packet;

public interface CachedMapUpdate extends Packet {

    String getKey();

    String getValue();
}
