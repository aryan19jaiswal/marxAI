package com.marxAI.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link Resume} entity's builder, getters, and setters. */
class ResumeTest {

    @Test
    void builderPopulatesAllFields() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Instant uploadedAt = Instant.now();

        Resume resume = Resume.builder()
                .id(UUID.randomUUID())
                .user(user)
                .s3Key("resumes/john-doe.pdf")
                .parsedData("{\"name\":\"John Doe\"}")
                .atsScore(87)
                .feedback("{\"summary\":\"Strong resume\"}")
                .uploadedAt(uploadedAt)
                .build();

        assertThat(resume.getUser()).isEqualTo(user);
        assertThat(resume.getS3Key()).isEqualTo("resumes/john-doe.pdf");
        assertThat(resume.getParsedData()).isEqualTo("{\"name\":\"John Doe\"}");
        assertThat(resume.getAtsScore()).isEqualTo(87);
        assertThat(resume.getFeedback()).isEqualTo("{\"summary\":\"Strong resume\"}");
        assertThat(resume.getUploadedAt()).isEqualTo(uploadedAt);
    }

    @Test
    void settersMutateFields() {
        Resume resume = new Resume();
        resume.setAtsScore(95);

        assertThat(resume.getAtsScore()).isEqualTo(95);
    }
}
