package com.service.report.generator.annotation;

import com.service.report.generator.tag.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditor {
    UserRole[] allowedRoles() default {};
    boolean log() default true;
    boolean auditJwt() default true;
}
