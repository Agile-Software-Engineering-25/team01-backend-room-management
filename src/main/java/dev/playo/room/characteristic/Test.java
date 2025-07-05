package dev.playo.room.characteristic;

import dev.playo.generated.roommanagement.model.CharacteristicValue;
import jakarta.persistence.Embeddable;

@Embeddable
public class Test implements CharacteristicValue {

  private String value;
}
