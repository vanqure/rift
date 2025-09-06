package io.github.rift.serializer.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rift.serializer.Serializer;
import io.github.rift.serializer.SerializerException;
import java.io.IOException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link Serializer} using Jackson's {@link ObjectMapper} for JSON serialization
 * and deserialization.
 *
 * <p>This serializer handles converting {@link JacksonSerializable} objects to and from JSON
 * strings. It wraps any Jackson exceptions in {@link SerializerException} for consistent error
 * handling.
 */
public final class JacksonSerializer implements Serializer {

    private final @NotNull ObjectMapper objectMapper;

    /**
     * Creates a new Jackson-based serializer.
     *
     * @param objectMapper The Jackson ObjectMapper to use for serialization/deserialization
     * @throws NullPointerException if objectMapper is null
     */
    @Contract(pure = true)
    JacksonSerializer(@NotNull ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes an object into a JSON string.
     *
     * @param value The object to serialize
     * @return JSON string representation of the object, or null if input is null
     * @throws SerializerException if serialization fails
     */
    @Override
    public @Nullable String serializeRaw(@Nullable Object value) throws SerializerException {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new SerializerException(
                    "Could not encode %s into json payload, because of unexpected exception."
                            .formatted(value.getClass()),
                    exception);
        }
    }

    /**
     * Deserializes a JSON payload into an object.
     *
     * @param payload The JSON payload to deserialize
     * @return The deserialized object, or null if input is null
     * @throws SerializerException if deserialization fails
     */
    @Override
    public <V> @Nullable V deserializeRaw(@Nullable String payload) throws SerializerException {
        if (payload == null) {
            return null;
        }

        try {
            //noinspection unchecked
            return (V) objectMapper.readValue(payload, Object.class);
        } catch (IOException exception) {
            throw new SerializerException(
                    "Could not decode payload into instance, because of unexpected exception.", exception);
        }
    }

    /**
     * Serializes a Serializable object into a JSON string.
     *
     * @param value The object to serialize
     * @param <V>   The type of the object
     * @return JSON string representation of the object, or null if input is null
     * @throws SerializerException if serialization fails
     */
    @Override
    @Contract("null -> null; !null -> !null")
    public @Nullable <V> String serialize(@Nullable V value) throws SerializerException {
        return serializeRaw(value);
    }

    /**
     * Deserializes a Serializable string into a Representable object.
     *
     * @param payload The JSON string to deserialize
     * @param <V>     The type of the object
     * @return The deserialized object, or null if input is null
     * @throws SerializerException if deserialization fails
     */
    @Override
    @Contract("null -> null; !null -> !null")
    public @Nullable <V> V deserialize(@Nullable String payload) throws SerializerException {
        if (payload == null) {
            return null;
        }

        try {
            //noinspection unchecked
            return (V) objectMapper.readValue(payload, JacksonSerializable.class);
        } catch (IOException exception) {
            throw new SerializerException(
                    "Could not decode payload into instance, because of unexpected exception.", exception);
        }
    }
}
