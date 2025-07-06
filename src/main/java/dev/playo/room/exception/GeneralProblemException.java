package dev.playo.room.exception;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Stackless exception that gets rewritten to a problem detail based on the given status and description.
 */
@Getter
public class GeneralProblemException extends RuntimeException {

  private final HttpStatus status;
  private final String description;

  public GeneralProblemException(@Nonnull HttpStatus status, @Nonnull String description) {
    this.status = status;
    this.description = description;
  }

  @Override
  public @Nonnull Throwable fillInStackTrace() {
    return this;
  }

  @Override
  public @Nonnull Throwable initCause(@Nullable Throwable cause) {
    return this;
  }
}
