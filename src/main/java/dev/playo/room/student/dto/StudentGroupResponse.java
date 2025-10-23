package dev.playo.room.student.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;

public record StudentGroupResponse(@NonNull String name, @JsonProperty("students_count") int studentsCount) {

}
