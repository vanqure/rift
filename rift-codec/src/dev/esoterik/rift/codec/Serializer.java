package dev.esoterik.rift.codec;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface Serializer {

    /**
     * Serializes a Serializable object into a payload string.
     *
     * @param value The object to serialize
     * @param <V> The type of the object
     * @return payload string representation of the object, or null if input is null
     * @throws SerializerException if serialization fails
     */
    @Nullable
    @Contract("null -> null; !null -> !null")
    default <V> String serialize(@Nullable V value) throws SerializerException {
        return serializeRaw(value);
    }
    /**
     * Deserializes a string payload into a Serializable object.
     *
     * @param payload The string payload to deserialize
     * @param <V> The type of the object
     * @return The deserialized object, or null if input is null
     * @throws SerializerException if deserialization fails
     */
    @Nullable
    @Contract("null -> null; !null -> !null")
    default <V> V deserialize(@Nullable String payload) throws SerializerException {
        return deserializeRaw(payload);
    }
    /**
     * Serializes an object into a payload string.
     *
     * @param value The object to serialize
     * @return payload string representation of the object, or null if input is null
     * @throws SerializerException if serialization fails
     */
    @Nullable
    @Contract("null -> null; !null -> !null")
    String serializeRaw(@Nullable Object value) throws SerializerException;
    /**
     * Deserializes a string payload into an object.
     *
     * @param payload The string payload to deserialize
     * @return The deserialized object, or null if input is null
     * @throws SerializerException if deserialization fails
     */
    @Nullable
    @Contract("null -> null; !null -> !null")
    <V> V deserializeRaw(@Nullable String payload) throws SerializerException;
}
