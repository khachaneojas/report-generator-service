package com.service.report.generator.repository;

import com.service.report.generator.entity.UserRoleModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleModel, Long> {
}
