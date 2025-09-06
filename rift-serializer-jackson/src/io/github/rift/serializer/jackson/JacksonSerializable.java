package io.github.rift.serializer.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface JacksonSerializable extends Serializable {}
