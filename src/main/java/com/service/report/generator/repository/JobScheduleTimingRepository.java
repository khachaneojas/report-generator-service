package com.service.report.generator.repository;

import com.service.report.generator.entity.JobScheduleTimingModel;
import com.service.report.generator.tag.JobType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobScheduleTimingRepository extends JpaRepository<JobScheduleTimingModel, Long> {

    JobScheduleTimingModel findByJobType(JobType jobType);

}
