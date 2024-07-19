package com.service.report.generator.entity;


import com.service.report.generator.tag.JobStatus;
import com.service.report.generator.tag.JobType;
import com.service.report.generator.tag.ScheduleType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;


@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job",
        indexes = @Index(name = "idx_job_uid", columnList = "job_uid")
)
public class JobModel extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long id;

    @Column(name = "job_uid", unique = true, nullable = false)
    private String jobUid;

    @Column(name = "job_name", nullable = false)
    private String name;

    @Column(name = "job_description")
    private String description;

    @Column(name = "job_status", nullable = false)
    private JobStatus status;

    @Column(name = "job_type", nullable = false)
    private JobType jobType;

    @Column(name = "job_attempts", nullable = false)
    private int attempts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_data", columnDefinition = "json", nullable = false)
    private String jsonData;

    @Column(name = "job_last_ran_at")
    private Instant lastRanAt;

    @Column(name = "job_last_ran_by")
    private String lastRanBy;

    @Column(name = "sdl_at", nullable = false)
    private Instant executeAt;

    @Column(name = "sdl_type", nullable = false)
    private ScheduleType scheduleType;

}
