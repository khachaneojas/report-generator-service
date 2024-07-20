package com.service.report.generator.repository;

import com.service.report.generator.entity.TransformationRuleModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransformationRuleRepository extends JpaRepository<TransformationRuleModel, Long> {
}
