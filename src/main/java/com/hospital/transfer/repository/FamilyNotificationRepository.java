package com.hospital.transfer.repository;

import com.hospital.transfer.entity.FamilyNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyNotificationRepository extends JpaRepository<FamilyNotification, Long> {

    Optional<FamilyNotification> findByTransferApplicationId(Long transferApplicationId);

    List<FamilyNotification> findByPatientAdmissionNumber(String admissionNumber);

    List<FamilyNotification> findByNotifiedFalse();
}
