package com.service.report.generator.entity;

import com.service.report.generator.tag.FieldName;
import com.service.report.generator.tag.OperationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transform_rule")
public class TransformationRuleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tfm_id")
    private Long id;

    @Column(name = "tfm_exp",nullable = false)
    private String transformationExpression;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tfm_data", columnDefinition = "json", nullable = false)
    private String transformationData;

    @Column(name = "tfm_opt",nullable = false)
    private OperationType operationType;

    @Column(name = "tfm_field", nullable = false)
    private FieldName fieldName;

}
