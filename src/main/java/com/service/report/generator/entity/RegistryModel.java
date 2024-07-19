package com.service.report.generator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "registry")
public class RegistryModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reg_id")
    private Long id;

    @Column(name = "reg_last_update_received", nullable = false)
    private Instant lastUpdateReceived;

    @Column(name = "reg_mac_address", nullable = false, length = 40)
    private String macAddress;

    @Column(name = "reg_ip_address", nullable = false, length = 40)
    private String ipAddress;

//    @Column(name = "reg_instance_id", nullable = false)
//    private int instance;

}
