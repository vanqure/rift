package dev.esoterik.rift.codec.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jetbrains.annotations.NotNull;

public final class JacksonSerializerFactory {

  private JacksonSerializerFactory() {}

  public static @NotNull JacksonSerializer create(final @NotNull ObjectMapper objectMapper) {
    return new JacksonSerializer(objectMapper);
  }

  public static @NotNull JacksonSerializer create() {
    final ObjectMapper mapper = createObjectMapper();
    return create(mapper);
  }

  public static @NotNull JacksonSerializer create(final @NotNull ClassLoader classLoader) {
    final ObjectMapper mapper = createObjectMapper();
    mapper.getTypeFactory().withClassLoader(classLoader);
    return create(mapper);
  }

  private static @NotNull ObjectMapper createObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setVisibility(
        mapper
            .getSerializationConfig()
            .getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    mapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    mapper.registerSubtypes(JacksonSerializable.class);
    return mapper;
  }
}
