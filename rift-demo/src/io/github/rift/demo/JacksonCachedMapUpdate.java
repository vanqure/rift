package io.github.rift.demo;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.rift.serializer.jackson.AbstractJacksonPacket;
import io.github.rift.map.CachedMapUpdate;

public class JacksonCachedMapUpdate extends AbstractJacksonPacket implements CachedMapUpdate {

    private String key;
    private String value;

    @JsonCreator
    private JacksonCachedMapUpdate() {}

    public JacksonCachedMapUpdate(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }
}
