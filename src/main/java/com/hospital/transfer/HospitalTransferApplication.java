package com.hospital.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HospitalTransferApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalTransferApplication.class, args);
    }
}
