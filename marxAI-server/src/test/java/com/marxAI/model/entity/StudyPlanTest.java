package com.marxAI.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link StudyPlan} entity's builder, defaults, and setters. */
class StudyPlanTest {

    @Test
    void builderPopulatesAllFields() {
        User user = User.builder().id(UUID.randomUUID()).build();
        LocalDate start = LocalDate.of(2026, 6, 21);
        LocalDate end = start.plusDays(30);
        Instant createdAt = Instant.now();

        StudyPlan plan = StudyPlan.builder()
                .id(UUID.randomUUID())
                .user(user)
                .startDate(start)
                .endDate(end)
                .planJson("{\"days\":[]}")
                .status("ACTIVE")
                .createdAt(createdAt)
                .build();

        assertThat(plan.getUser()).isEqualTo(user);
        assertThat(plan.getStartDate()).isEqualTo(start);
        assertThat(plan.getEndDate()).isEqualTo(end);
        assertThat(plan.getPlanJson()).isEqualTo("{\"days\":[]}");
        assertThat(plan.getStatus()).isEqualTo("ACTIVE");
        assertThat(plan.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void builderDefaultsStatusWhenUnset() {
        StudyPlan plan = StudyPlan.builder().build();

        assertThat(plan.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void settersMutateFields() {
        StudyPlan plan = new StudyPlan();
        plan.setStatus("COMPLETED");

        assertThat(plan.getStatus()).isEqualTo("COMPLETED");
    }
}
