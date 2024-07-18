package com.service.report.generator.entity;

import com.service.report.generator.tag.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_schedule_timing")
public class JobScheduleTimingModel extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sdl_id")
    private Long id;

    @Column(name = "sdl_type")
    private JobType jobType;

    @Column(name = "sdl_time")
    private LocalTime scheduleTime;

}
