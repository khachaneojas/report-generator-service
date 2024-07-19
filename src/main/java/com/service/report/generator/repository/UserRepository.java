package com.service.report.generator.repository;

import com.service.report.generator.entity.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserModel, Long> {

    Boolean existsByUserUid(String userId);
    UserModel findByUserUid(String userUid);
    UserModel findByEmail(String email);

}
