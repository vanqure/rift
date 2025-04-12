package dev.esoterik.rift.lock;

public final class DistributedLockException extends IllegalStateException {

  public DistributedLockException(final String message) {
    super(message);
  }

  public DistributedLockException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
