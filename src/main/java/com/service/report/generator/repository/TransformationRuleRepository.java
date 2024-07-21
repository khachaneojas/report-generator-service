package com.service.report.generator.repository;

import com.service.report.generator.entity.TransformationRuleModel;
import com.service.report.generator.tag.FieldName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransformationRuleRepository extends JpaRepository<TransformationRuleModel, Long> {

    List<TransformationRuleModel> findByFieldNameIn(List<FieldName> fieldNameList);

}
