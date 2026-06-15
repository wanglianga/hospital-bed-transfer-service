package com.hospital.transfer.repository;

import com.hospital.transfer.entity.Bed;
import com.hospital.transfer.enums.BedType;
import com.hospital.transfer.enums.IsolationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BedRepository extends JpaRepository<Bed, Long> {

    List<Bed> findByDepartmentAndOccupiedFalseAndEnabledTrue(String department);

    List<Bed> findByDepartmentAndBedTypeAndOccupiedFalseAndEnabledTrue(String department, BedType bedType);

    List<Bed> findByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue(String department, IsolationType isolationType);

    List<Bed> findByOccupiedFalseAndEnabledTrue();

    List<Bed> findByDepartment(String department);

    List<Bed> findByOccupiedTrue();

    Optional<Bed> findByBedNumber(String bedNumber);

    long countByDepartmentAndOccupiedFalseAndEnabledTrue(String department);

    long countByDepartmentAndBedTypeAndOccupiedFalseAndEnabledTrue(String department, BedType bedType);

    long countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue(String department, IsolationType isolationType);

    long countByOccupiedFalseAndEnabledTrue();

    long countByBedTypeAndOccupiedFalseAndEnabledTrue(BedType bedType);

    long countByIsolationTypeAndOccupiedFalseAndEnabledTrue(IsolationType isolationType);

    List<Bed> findByDepartmentAndRoomNumberAndOccupiedTrue(String department, String roomNumber);

    List<Bed> findByDepartmentAndWardNumberAndOccupiedTrue(String department, String wardNumber);

    List<Bed> findByDepartmentAndWardNumberAndOccupiedFalseAndEnabledTrue(String department, String wardNumber);

    long countByDepartmentAndWardNumberAndOccupiedFalseAndEnabledTrue(String department, String wardNumber);
}
