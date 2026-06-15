package com.hospital.transfer.service;

import com.hospital.transfer.dto.BedQueryResponse;
import com.hospital.transfer.dto.IsolationRestriction;
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

    public List<BedQueryResponse> getAvailableBedsByWard(String department, String wardNumber) {
        return bedRepository.findByDepartmentAndWardNumberAndOccupiedFalseAndEnabledTrue(department, wardNumber)
                .stream()
                .map(this::toQueryResponse)
                .collect(Collectors.toList());
    }

    public long countAvailableBedsByWard(String department, String wardNumber) {
        return bedRepository.countByDepartmentAndWardNumberAndOccupiedFalseAndEnabledTrue(department, wardNumber);
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

    public IsolationRestriction checkIsolationRestriction(String department, BedType bedType, IsolationType isolationType,
                                                          String diagnosis, String doctorRemark) {
        if (isolationType == IsolationType.NONE) {
            return null;
        }

        int infectionRiskLevel = assessInfectionRisk(isolationType, diagnosis);
        boolean wardSuitable = checkWardCondition(department, isolationType);

        if (!wardSuitable) {
            return IsolationRestriction.builder()
                    .restrictionType("WARD_UNSUITABLE")
                    .message("目标科室 " + department + " 不具备收治 " + isolationType + " 隔离患者的条件")
                    .requiredIsolationType(isolationType)
                    .detail("感染风险等级: " + infectionRiskLevel + "，请转至具备相应隔离条件的科室")
                    .build();
        }

        List<Bed> isolationBeds = bedRepository
                .findByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue(department, isolationType);

        if (isolationBeds.isEmpty()) {
            return IsolationRestriction.builder()
                    .restrictionType("NO_ISOLATION_BED")
                    .message("目标科室 " + department + " 暂无可用的 " + isolationType + " 隔离床位")
                    .requiredIsolationType(isolationType)
                    .detail("感染风险等级: " + infectionRiskLevel + "，需等待隔离床位释放或协调其他科室")
                    .build();
        }

        for (Bed candidateBed : isolationBeds) {
            IsolationRestriction roomConflict = checkRoommateConflict(candidateBed, isolationType);
            if (roomConflict != null) {
                return roomConflict;
            }
        }

        return null;
    }

    private int assessInfectionRisk(IsolationType isolationType, String diagnosis) {
        int riskLevel = 1;
        switch (isolationType) {
            case AIRBORNE:
                riskLevel = 5;
                break;
            case DROPLET:
                riskLevel = 4;
                break;
            case CONTACT:
                riskLevel = 3;
                break;
            case PROTECTIVE:
                riskLevel = 2;
                break;
            default:
                riskLevel = 1;
        }
        if (diagnosis != null) {
            String lowerDiagnosis = diagnosis.toLowerCase();
            if (lowerDiagnosis.contains("tb") || lowerDiagnosis.contains("结核")
                    || lowerDiagnosis.contains("sars") || lowerDiagnosis.contains("covid")) {
                riskLevel = Math.max(riskLevel, 5);
            } else if (lowerDiagnosis.contains("flu") || lowerDiagnosis.contains("流感")
                    || lowerDiagnosis.contains("meningitis") || lowerDiagnosis.contains("脑膜炎")) {
                riskLevel = Math.max(riskLevel, 4);
            } else if (lowerDiagnosis.contains("mdr") || lowerDiagnosis.contains("耐药")
                    || lowerDiagnosis.contains("mrsa") || lowerDiagnosis.contains("vre")) {
                riskLevel = Math.max(riskLevel, 4);
            }
        }
        return riskLevel;
    }

    private boolean checkWardCondition(String department, IsolationType isolationType) {
        if (isolationType == IsolationType.NONE) {
            return true;
        }
        List<Bed> allIsolationBeds = bedRepository.findByDepartment(department).stream()
                .filter(b -> b.getIsolationType() != IsolationType.NONE)
                .collect(Collectors.toList());

        if (allIsolationBeds.isEmpty()) {
            return false;
        }

        if (isolationType == IsolationType.AIRBORNE) {
            return allIsolationBeds.stream()
                    .anyMatch(b -> b.getIsolationType() == IsolationType.AIRBORNE);
        }

        return true;
    }

    private IsolationRestriction checkRoommateConflict(Bed candidateBed, IsolationType patientIsolationType) {
        if (candidateBed.getRoomNumber() == null) {
            return null;
        }
        List<Bed> occupiedBedsInRoom = bedRepository
                .findByDepartmentAndRoomNumberAndOccupiedTrue(candidateBed.getDepartment(), candidateBed.getRoomNumber());
        for (Bed occupiedBed : occupiedBedsInRoom) {
            IsolationType existingIsolation = occupiedBed.getIsolationType();
            if (existingIsolation != IsolationType.NONE && existingIsolation != patientIsolationType) {
                return IsolationRestriction.builder()
                        .restrictionType("ROOM_CONFLICT")
                        .message("房间 " + candidateBed.getRoomNumber() + " 存在不同隔离类型的患者，禁止混住")
                        .requiredIsolationType(patientIsolationType)
                        .detail("现有患者隔离类型: " + existingIsolation + "，申请患者隔离类型: " + patientIsolationType)
                        .build();
            }
            if (existingIsolation == IsolationType.NONE && patientIsolationType != IsolationType.NONE) {
                return IsolationRestriction.builder()
                        .restrictionType("ROOM_CONFLICT")
                        .message("房间 " + candidateBed.getRoomNumber() + " 存在非隔离患者，隔离患者禁止入住")
                        .requiredIsolationType(patientIsolationType)
                        .detail("非隔离患者住院号: " + occupiedBed.getOccupiedByAdmissionNumber())
                        .build();
            }
        }
        return null;
    }

    public Bed findBestMatchBed(String department, BedType bedType, IsolationType isolationType) {
        List<Bed> candidates;
        if (isolationType != IsolationType.NONE) {
            candidates = bedRepository.findByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue(department, isolationType);
            if (!candidates.isEmpty()) {
                for (Bed bed : candidates) {
                    IsolationRestriction conflict = checkRoommateConflict(bed, isolationType);
                    if (conflict != null) {
                        continue;
                    }
                    if (bed.getBedType() == bedType) {
                        return bed;
                    }
                }
                for (Bed bed : candidates) {
                    IsolationRestriction conflict = checkRoommateConflict(bed, isolationType);
                    if (conflict == null) {
                        return bed;
                    }
                }
            }
            log.warn("目标科室 {} 未找到匹配隔离类型 {} 的床位", department, isolationType);
            return null;
        } else {
            candidates = bedRepository.findByDepartmentAndBedTypeAndOccupiedFalseAndEnabledTrue(department, bedType);
            if (!candidates.isEmpty()) {
                for (Bed bed : candidates) {
                    if (bed.getIsolationType() == IsolationType.NONE) {
                        IsolationRestriction conflict = checkRoommateConflict(bed, isolationType);
                        if (conflict == null) {
                            return bed;
                        }
                    }
                }
            }
        }
        candidates = bedRepository.findByDepartmentAndOccupiedFalseAndEnabledTrue(department);
        for (Bed bed : candidates) {
            if (bed.getIsolationType() == IsolationType.NONE) {
                IsolationRestriction conflict = checkRoommateConflict(bed, isolationType);
                if (conflict == null) {
                    return bed;
                }
            }
        }
        return null;
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
                .roomNumber(bed.getRoomNumber())
                .wardNumber(bed.getWardNumber())
                .build();
    }
}
