package com.hospital.transfer.service;

import com.hospital.transfer.dto.BedQueryResponse;
import com.hospital.transfer.entity.Bed;
import com.hospital.transfer.enums.BedType;
import com.hospital.transfer.enums.IsolationType;
import com.hospital.transfer.exception.BusinessException;
import com.hospital.transfer.repository.BedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BedService {

    private static final Logger log = LoggerFactory.getLogger(BedService.class);

    private final BedRepository bedRepository;

    public BedService(BedRepository bedRepository) {
        this.bedRepository = bedRepository;
    }

    @Transactional
    public Bed createBed(Bed bed) {
        if (bedRepository.findByBedNumber(bed.getBedNumber()).isPresent()) {
            throw new BusinessException("床位编号已存在: " + bed.getBedNumber());
        }
        return bedRepository.save(bed);
    }

    @Transactional
    public Bed updateBed(Long id, Bed update) {
        Bed bed = bedRepository.findById(id)
                .orElseThrow(() -> new BusinessException("床位不存在: " + id));
        if (update.getDepartment() != null) bed.setDepartment(update.getDepartment());
        if (update.getBedType() != null) bed.setBedType(update.getBedType());
        if (update.getIsolationType() != null) bed.setIsolationType(update.getIsolationType());
        if (update.getEnabled() != null) bed.setEnabled(update.getEnabled());
        return bedRepository.save(bed);
    }

    public Bed getBed(Long id) {
        return bedRepository.findById(id)
                .orElseThrow(() -> new BusinessException("床位不存在: " + id));
    }

    public List<BedQueryResponse> getAllBeds() {
        return bedRepository.findAll().stream()
                .map(this::toQueryResponse)
                .collect(Collectors.toList());
    }

    public List<BedQueryResponse> getAvailableBedsByDepartment(String department) {
        return bedRepository.findByDepartmentAndOccupiedFalseAndEnabledTrue(department).stream()
                .map(this::toQueryResponse)
                .collect(Collectors.toList());
    }

    public List<BedQueryResponse> getAvailableBedsByDepartmentAndType(String department, BedType bedType) {
        return bedRepository.findByDepartmentAndBedTypeAndOccupiedFalseAndEnabledTrue(department, bedType).stream()
                .map(this::toQueryResponse)
                .collect(Collectors.toList());
    }

    public List<BedQueryResponse> getAvailableBedsByDepartmentAndIsolation(String department, IsolationType isolationType) {
        return bedRepository.findByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue(department, isolationType).stream()
                .map(this::toQueryResponse)
                .collect(Collectors.toList());
    }

    public List<BedQueryResponse> getAllAvailableBeds() {
        return bedRepository.findByOccupiedFalseAndEnabledTrue().stream()
                .map(this::toQueryResponse)
                .collect(Collectors.toList());
    }

    public long countAvailableBeds(String department) {
        return bedRepository.countByDepartmentAndOccupiedFalseAndEnabledTrue(department);
    }

    public long countAvailableBedsByType(String department, BedType bedType) {
        return bedRepository.countByDepartmentAndBedTypeAndOccupiedFalseAndEnabledTrue(department, bedType);
    }

    public long countAllAvailableBeds() {
        return bedRepository.countByOccupiedFalseAndEnabledTrue();
    }

    public long countAvailableBedsByIsolation(String department, IsolationType isolationType) {
        return bedRepository.countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue(department, isolationType);
    }

    public long countAvailableBedsOnlyByType(BedType bedType) {
        return bedRepository.countByBedTypeAndOccupiedFalseAndEnabledTrue(bedType);
    }

    public long countAvailableBedsOnlyByIsolation(IsolationType isolationType) {
        return bedRepository.countByIsolationTypeAndOccupiedFalseAndEnabledTrue(isolationType);
    }

    @Transactional
    public Bed occupyBed(Long bedId, String admissionNumber, Long transferApplicationId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new BusinessException("床位不存在: " + bedId));
        if (!bed.isAvailable()) {
            throw new BusinessException("床位不可用: " + bed.getBedNumber());
        }
        bed.setOccupied(true);
        bed.setOccupiedByAdmissionNumber(admissionNumber);
        bed.setCurrentTransferApplicationId(transferApplicationId);
        bed.setOccupiedTime(java.time.LocalDateTime.now());
        return bedRepository.save(bed);
    }

    @Transactional
    public Bed releaseBed(Long bedId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new BusinessException("床位不存在: " + bedId));
        bed.setOccupied(false);
        bed.setOccupiedByAdmissionNumber(null);
        bed.setCurrentTransferApplicationId(null);
        bed.setOccupiedTime(null);
        return bedRepository.save(bed);
    }

    @Transactional
    public Bed confirmAdmission(Long bedId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new BusinessException("床位不存在: " + bedId));
        if (!bed.getOccupied()) {
            throw new BusinessException("床位未被占用，无法确认入科: " + bed.getBedNumber());
        }
        log.info("床位 {} 入科确认，患者住院号: {}", bed.getBedNumber(), bed.getOccupiedByAdmissionNumber());
        return bed;
    }

    public Bed findBestMatchBed(String department, BedType bedType, IsolationType isolationType) {
        List<Bed> candidates;
        if (isolationType != IsolationType.NONE) {
            candidates = bedRepository.findByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue(department, isolationType);
            if (!candidates.isEmpty()) {
                for (Bed bed : candidates) {
                    if (bed.getBedType() == bedType) {
                        return bed;
                    }
                }
                return candidates.get(0);
            }
            log.warn("目标科室 {} 未找到匹配隔离类型 {} 的床位", department, isolationType);
            return null;
        } else {
            candidates = bedRepository.findByDepartmentAndBedTypeAndOccupiedFalseAndEnabledTrue(department, bedType);
            if (!candidates.isEmpty()) {
                for (Bed bed : candidates) {
                    if (bed.getIsolationType() == IsolationType.NONE) {
                        return bed;
                    }
                }
            }
        }
        candidates = bedRepository.findByDepartmentAndOccupiedFalseAndEnabledTrue(department);
        for (Bed bed : candidates) {
            if (bed.getIsolationType() == IsolationType.NONE) {
                return bed;
            }
        }
        return candidates.isEmpty() ? null : null;
    }

    private BedQueryResponse toQueryResponse(Bed bed) {
        return BedQueryResponse.builder()
                .id(bed.getId())
                .bedNumber(bed.getBedNumber())
                .department(bed.getDepartment())
                .bedType(bed.getBedType())
                .isolationType(bed.getIsolationType())
                .occupied(bed.getOccupied())
                .occupiedByAdmissionNumber(bed.getOccupiedByAdmissionNumber())
                .build();
    }
}
