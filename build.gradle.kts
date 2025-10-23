import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
  java
  id("org.springframework.boot") version "3.5.6"
  id("io.spring.dependency-management") version "1.1.7"
  id("org.openapi.generator") version "7.14.0"
}

group = "dev.playo"
version = "0.0.1-SNAPSHOT"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

tasks.withType<JavaCompile> {
  dependsOn("openApiGenerate")
}

tasks.withType<BootJar> {
  archiveFileName.set("app.jar")
}

openApiGenerate {
  validateSpec.set(false)
  generatorName.set("spring")
  inputSpec.set("$rootDir/room-management.yaml")
  outputDir.set("${layout.buildDirectory.get()}/generated")
  apiPackage.set("dev.playo.generated.roommanagement.api")
  modelPackage.set("dev.playo.generated.roommanagement.model")
  configOptions.set(mapOf(
    "dateLibrary" to "java8",
    "interfaceOnly" to "true",
    "useSpringBoot3" to "true",
    "useSwaggerUi" to "false",
    "skipDefaultInterface" to "true",
    "annotationLibrary" to "none",
    "documentationProvider" to "source",
    "openApiNullable" to "false"
  ))
}

sourceSets {
  main {
    java {
      srcDir("${layout.buildDirectory.get()}/generated/src/main/java")
    }
  }
}

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")

  compileOnly("org.projectlombok:lombok")
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  developmentOnly("org.springframework.boot:spring-boot-docker-compose")
  runtimeOnly("org.postgresql:postgresql")
  annotationProcessor("org.projectlombok:lombok")

  testImplementation("org.wiremock.integrations:wiremock-spring-boot:3.10.0")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.testcontainers:postgresql:1.19.8")
  testImplementation("org.testcontainers:junit-jupiter:1.19.8")
}

tasks.withType<Test> {
  doFirst {
    if (System.getProperty("jdk.module.illegalAccess") == null) {
      jvmArgs("--illegal-access=permit")
    }
  }

  // Stellt sicher, dass der Mockito-Agent als Java Agent geladen wird.
  jvmArgs("-javaagent:${classpath.find { it.name.contains("byte-buddy-agent") }?.absolutePath}")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
