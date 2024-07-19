package com.service.report.generator.repository;

import com.service.report.generator.entity.JobModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<JobModel, Long> {

    Boolean existsByJobUid(String jobId);
    JobModel findByJobUid(String jobId);

}
