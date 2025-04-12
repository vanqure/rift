package dev.esoterik.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import dev.esoterik.rift.codec.jackson.AbstractJacksonPacket;
import dev.esoterik.rift.map.CachedMapUpdate;

public class JacksonCachedMapUpdate extends AbstractJacksonPacket implements CachedMapUpdate {

  private String key;
  private String value;

  @JsonCreator
  private JacksonCachedMapUpdate() {}

  public JacksonCachedMapUpdate(final String key, final String value) {
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
