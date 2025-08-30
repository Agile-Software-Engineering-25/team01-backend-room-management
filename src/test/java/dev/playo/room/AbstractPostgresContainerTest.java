package dev.playo.room;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public abstract class AbstractPostgresContainerTest {

  // Erstellt einen statischen PostgreSQL-Container, der nur einmal startet
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

  static {
    postgres.start();
  }

  // Konfiguriert die Datenbankverbindung dynamisch zur Laufzeit
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }
}
