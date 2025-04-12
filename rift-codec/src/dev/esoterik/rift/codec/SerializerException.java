package dev.esoterik.rift.codec;

/*
 * Thrown when a serializer encounters an error during serialization or deserialization.
 */
public final class SerializerException extends IllegalStateException {

  public SerializerException(final String message) {
    super(message);
  }

  public SerializerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
