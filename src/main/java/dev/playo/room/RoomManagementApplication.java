package dev.playo.room;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoomManagementApplication {

  public static void main(String[] args) {
    SpringApplication.run(RoomManagementApplication.class, args);
  }

  @PostConstruct
  void started() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
  }

}
