package com.hospital.transfer.repository;

import com.hospital.transfer.entity.TransferApplication;
import com.hospital.transfer.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferApplicationRepository extends JpaRepository<TransferApplication, Long> {

    List<TransferApplication> findByPatientAdmissionNumber(String admissionNumber);

    List<TransferApplication> findByStatus(TransferStatus status);

    List<TransferApplication> findByStatusIn(List<TransferStatus> statuses);

    List<TransferApplication> findByTargetDepartmentAndStatus(String department, TransferStatus status);

    List<TransferApplication> findByCurrentDepartmentAndStatus(String department, TransferStatus status);

    List<TransferApplication> findByStatusOrderByPriorityScoreDesc(TransferStatus status);

    List<TransferApplication> findByStatusInOrderByPriorityScoreDesc(List<TransferStatus> statuses);
}
