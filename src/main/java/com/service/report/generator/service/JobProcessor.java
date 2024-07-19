package com.service.report.generator.service;

import com.service.report.generator.entity.JobModel;
import com.service.report.generator.entity.RegistryModel;
import com.service.report.generator.properties.amqp.AMQPConfigProperties;
import com.service.report.generator.repository.JobRepository;
import com.service.report.generator.repository.RegistryRepository;
import com.service.report.generator.tag.DeviceAddressType;
import com.service.report.generator.tag.JobStatus;
import com.service.report.generator.tag.JobType;
import com.service.report.generator.utility.DeviceIdentityWizard;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class JobProcessor {

    private final AMQPConfigProperties amqpConfigProperties;
    private final DeviceIdentityWizard deviceIdentity;
    private final RabbitTemplate rabbitTemplate;
    private final RegistryRepository registryRepository;
    private final JobRepository jobRepository;

    boolean isJobAttemptsNotExceeded(int attempts) {
        return attempts < amqpConfigProperties.getRetryLimit();
    }



    boolean isJobReadyToRun(JobModel model) {

        Instant lastRanAt = model.getLastRanAt();
        Instant now = Instant.now();

        boolean retryDelayExceeded;
        if(null == lastRanAt){
            retryDelayExceeded = true;
        }else{
            retryDelayExceeded = (model.getLastRanAt().toEpochMilli() + amqpConfigProperties.getRetryDelay()) < now.toEpochMilli();
        }


        return retryDelayExceeded && isExecuteAtBeforeOrEqualToNow(model::getExecuteAt);
    }



    ImmutablePair<Long, JobType> enqueueJob(JobModel model) {
        String routingKey = switch (model.getJobType()) {
            case REPORT_GENERATOR -> amqpConfigProperties.getRoutingKey().getStandard();
        };

        model.setStatus(JobStatus.RUNNING);
        model.setLastRanAt(Instant.now());
        RegistryModel instance = getInstance();
        model.setLastRanBy(instance.getMacAddress());
        JobModel job = jobRepository.saveAndFlush(model);

        if (null == routingKey) {
            return null;
        }

        rabbitTemplate.convertAndSend(amqpConfigProperties.getExchange(), routingKey, job.getId());
        return ImmutablePair.of(job.getId(), job.getJobType());
    }




    /**
     * Retrieves the instance from the registry based on the current device's MAC address.
     * This method is transactional with serializable isolation level and required propagation.
     * It retrieves the instance from the registry repository based on the current device's MAC address.
     * If no instance is found for the current device's MAC address, a new instance is created
     * using the current device's IP and MAC addresses.
     * @return The instance from the registry based on the current device's MAC address.
     */
    public RegistryModel getInstance() {
        // Retrieve the current device's IP and MAC addresses.
        String currentInstanceIpAddress = deviceIdentity.getDeviceAddress(DeviceAddressType.IP);
        String currentInstanceMacAddress = deviceIdentity.getDeviceAddress(DeviceAddressType.MAC);
        // Check if an instance exists for the current device's MAC address.
        // If found, return the first instance from the list of instances.
        // Otherwise, create a new instance using the current device's IP and MAC addresses.
        return Optional.of(currentInstanceMacAddress)
                .map(registryRepository::findByMacAddress)
                .orElse(RegistryModel.builder()
                        .ipAddress(currentInstanceIpAddress)
                        .macAddress(currentInstanceMacAddress)
                        .build()
                );
    }



    private boolean isExecuteAtBeforeOrEqualToNow(Supplier<Instant> specifiedExecuteAt) {
        LocalDateTime executeAt = specifiedExecuteAt.get().atZone(ZoneId.of("UTC")).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return executeAt.isBefore(now) || executeAt.isEqual(now);
    }



}
