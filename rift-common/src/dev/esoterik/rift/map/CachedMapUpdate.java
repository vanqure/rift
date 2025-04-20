package dev.esoterik.rift.map;

import dev.esoterik.rift.codec.Packet;

public interface CachedMapUpdate extends Packet {

    String getKey();

    String getValue();
}
