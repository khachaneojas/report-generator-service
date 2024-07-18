package com.service.report.generator.repository;

import com.service.report.generator.entity.FileDataModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileDataRepository extends JpaRepository<FileDataModel, Long> {

}
