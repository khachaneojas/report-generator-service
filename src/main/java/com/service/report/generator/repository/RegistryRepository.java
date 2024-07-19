package com.service.report.generator.repository;

import com.service.report.generator.entity.RegistryModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistryRepository extends JpaRepository<RegistryModel, Long> {

    RegistryModel findByMacAddress(String macAddress);

}
