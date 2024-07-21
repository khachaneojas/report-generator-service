package com.service.report.generator.repository;

import com.service.report.generator.entity.DeviceRegistryModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistryRepository extends JpaRepository<DeviceRegistryModel, Long> {

    DeviceRegistryModel findByMacAddress(String macAddress);

}
