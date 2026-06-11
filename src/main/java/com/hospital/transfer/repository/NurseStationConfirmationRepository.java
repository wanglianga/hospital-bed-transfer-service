package com.hospital.transfer.repository;

import com.hospital.transfer.entity.NurseStationConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NurseStationConfirmationRepository extends JpaRepository<NurseStationConfirmation, Long> {

    Optional<NurseStationConfirmation> findByTransferApplicationId(Long transferApplicationId);

    List<NurseStationConfirmation> findByTargetDepartment(String targetDepartment);
}
