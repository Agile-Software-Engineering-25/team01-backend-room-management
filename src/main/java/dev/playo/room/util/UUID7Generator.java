package dev.playo.room.util;

import jakarta.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.uuid.UuidValueGenerator;

public class UUID7Generator implements UuidValueGenerator {

  private static final Random RANDOM = new SecureRandom();
  private static final VarHandle BYTE_ARRAY_LONG_VIEW_HANDLE =
    MethodHandles.byteArrayViewVarHandle(Long.TYPE.arrayType(), ByteOrder.BIG_ENDIAN);

  public static @Nonnull UUID generateUuid() {
    // get the random data for the second uuid component
    var bytes = new byte[10];
    RANDOM.nextBytes(bytes);
    var randA = ((bytes[0] & 0xFF) << 8) + (bytes[1] & 0xFF);
    var randB = (long) BYTE_ARRAY_LONG_VIEW_HANDLE.get(bytes, 2);

    // encode the timestamp and random components into an uuid
    var timestamp = System.currentTimeMillis();
    var rawMsb = (timestamp << 16) | randA;
    var msb = (rawMsb & 0xFFFFFFFFFFFF0FFFL) | 0x0000000000007000L; // set version to 7
    var lsb = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;  // set variant to DCE 1.1
    return new UUID(msb, lsb);
  }

  @Override
  public @Nonnull UUID generateUuid(@Nonnull SharedSessionContractImplementor session) {
    return generateUuid();
  }
}
