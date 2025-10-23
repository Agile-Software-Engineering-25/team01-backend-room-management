package dev.playo.room.student;

import dev.playo.room.student.dto.StudentGroupResponse;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class StudentGroupClient {

  private final String groupServiceBaseUrl;
  private final RestTemplate restTemplate;

  public StudentGroupClient(@Value("${room.client.group.url:https://sau-portal.de/team-11-api/api/v1}") String groupServiceBaseUrl) {
    this.groupServiceBaseUrl = groupServiceBaseUrl;
    this.restTemplate = new RestTemplate();
  }

  @Cacheable(cacheNames = "student-groups", key = "#name")
  public @Nullable StudentGroupResponse getStudentGroupByName(String name) {
    var url = String.format("%s/group/%s?withDetails=false", this.groupServiceBaseUrl, name);
    try {
      var response = this.restTemplate.getForEntity(url, StudentGroupResponse.class);
      return response.getBody();
    } catch (Exception exception) {
      log.warn("Failed to fetch student group '{}' from URL '{}': {}", name, url, exception.getMessage());
      return null;
    }
  }
}
