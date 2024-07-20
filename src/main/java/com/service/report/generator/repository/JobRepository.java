package com.service.report.generator.repository;

import com.service.report.generator.entity.JobModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRepository extends JpaRepository<JobModel, Long> {

    Boolean existsByJobUid(String jobId);
    Optional<JobModel> findByJobUid(String jobId);

}
