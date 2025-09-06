package io.github.rift.serializer;

/*
 * Thrown when a serializer encounters an error during serialization or deserialization.
 */
public final class SerializerException extends IllegalStateException {

    public SerializerException(String message) {
        super(message);
    }

    public SerializerException(String message, Throwable cause) {
        super(message, cause);
    }
}
