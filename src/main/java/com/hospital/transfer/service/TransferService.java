package com.hospital.transfer.service;

import com.hospital.transfer.dto.TransferRequest;
import com.hospital.transfer.dto.TransferResponse;
import com.hospital.transfer.entity.Bed;
import com.hospital.transfer.entity.FamilyNotification;
import com.hospital.transfer.entity.Patient;
import com.hospital.transfer.entity.TransferApplication;
import com.hospital.transfer.enums.*;
import com.hospital.transfer.exception.BusinessException;
import com.hospital.transfer.repository.FamilyNotificationRepository;
import com.hospital.transfer.repository.PatientRepository;
import com.hospital.transfer.repository.TransferApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferApplicationRepository transferRepository;
    private final PatientRepository patientRepository;
    private final BedService bedService;
    private final FamilyNotificationRepository familyNotificationRepository;

    public TransferService(TransferApplicationRepository transferRepository,
                           PatientRepository patientRepository,
                           BedService bedService,
                           FamilyNotificationRepository familyNotificationRepository) {
        this.transferRepository = transferRepository;
        this.patientRepository = patientRepository;
        this.bedService = bedService;
        this.familyNotificationRepository = familyNotificationRepository;
    }

    private static final int OCCUPATION_TIMEOUT_HOURS = 2;

    @Transactional
    public TransferResponse applyTransfer(TransferRequest request) {
        Patient patient = patientRepository.findByAdmissionNumber(request.getPatientAdmissionNumber())
                .orElseThrow(() -> new BusinessException("患者不存在，住院号: " + request.getPatientAdmissionNumber()));

        if (patient.getStatus() == PatientStatus.DECEASED) {
            throw new BusinessException("患者已死亡，无法申请转科");
        }
        if (patient.getStatus() == PatientStatus.DISCHARGING) {
            throw new BusinessException("患者正在出院流程中，无法申请转科");
        }
        if (patient.getCurrentDepartment().equals(request.getTargetDepartment())) {
            throw new BusinessException("目标科室与当前科室相同");
        }

        List<TransferApplication> activeTransfers = transferRepository
                .findByPatientAdmissionNumber(request.getPatientAdmissionNumber()).stream()
                .filter(t -> t.getStatus() != TransferStatus.CANCELLED
                        && t.getStatus() != TransferStatus.ADMISSION_CONFIRMED
                        && t.getStatus() != TransferStatus.TIMEOUT_RELEASED)
                .collect(Collectors.toList());
        if (!activeTransfers.isEmpty()) {
            throw new BusinessException("该患者已有进行中的转科申请");
        }

        int priorityScore = calculatePriorityScore(patient, request);

        PriorityLevel priorityLevel = determinePriorityLevel(priorityScore);

        TransferApplication application = TransferApplication.builder()
                .patientAdmissionNumber(request.getPatientAdmissionNumber())
                .currentDepartment(patient.getCurrentDepartment())
                .targetDepartment(request.getTargetDepartment())
                .requiredBedType(request.getRequiredBedType())
                .isolationRequirement(request.getIsolationRequirement())
                .nursingLevel(request.getNursingLevel())
                .priorityLevel(priorityLevel)
                .priorityScore(priorityScore)
                .diagnosis(request.getDiagnosis() != null ? request.getDiagnosis() : patient.getDiagnosis())
                .reason(request.getReason())
                .applicantDoctorId(request.getApplicantDoctorId())
                .appointmentTime(request.getAppointmentTime())
                .status(TransferStatus.PENDING)
                .build();

        TransferApplication saved = transferRepository.save(application);
        log.info("转科申请已创建，申请ID: {}，患者住院号: {}，优先级: {}，评分: {}",
                saved.getId(), request.getPatientAdmissionNumber(), priorityLevel, priorityScore);

        return attemptAssignBed(saved);
    }

    @Transactional
    public TransferResponse attemptAssignBed(TransferApplication application) {
        Bed bestBed = bedService.findBestMatchBed(
                application.getTargetDepartment(),
                application.getRequiredBedType(),
                application.getIsolationRequirement()
        );

        if (application.getIsolationRequirement() != IsolationType.NONE) {
            if (bestBed == null) {
                application.setStatus(TransferStatus.ISOLATION_REQUIRED);
                application.setRemark("需要 " + application.getIsolationRequirement() + " 隔离床位，目标科室暂无匹配隔离床位，等待安排");
                application.setAssignedBedId(null);
                application.setAssignedBedNumber(null);
                transferRepository.save(application);
                log.warn("转科申请 {} 需要隔离 {}，但目标科室 {} 无匹配隔离床位，保持待安排状态",
                        application.getId(), application.getIsolationRequirement(), application.getTargetDepartment());
                return toResponse(application);
            }
            if (bestBed.getIsolationType() != application.getIsolationRequirement()) {
                application.setStatus(TransferStatus.ISOLATION_REQUIRED);
                application.setRemark("需要 " + application.getIsolationRequirement() + " 隔离床位，已分配床位隔离类型为 "
                        + bestBed.getIsolationType() + "，不匹配，拒绝分配");
                application.setAssignedBedId(null);
                application.setAssignedBedNumber(null);
                transferRepository.save(application);
                log.warn("转科申请 {} 隔离类型不匹配，要求: {}，候选床位: {}({})，拒绝分配",
                        application.getId(), application.getIsolationRequirement(),
                        bestBed.getBedNumber(), bestBed.getIsolationType());
                return toResponse(application);
            }
            log.info("转科申请 {} 隔离床匹配成功: {}({})", application.getId(), bestBed.getBedNumber(), bestBed.getIsolationType());
        }

        if (bestBed == null) {
            application.setStatus(TransferStatus.NO_BED_AVAILABLE);
            application.setRemark("目标科室无可用床位");
            application.setAssignedBedId(null);
            application.setAssignedBedNumber(null);
            transferRepository.save(application);
            log.warn("转科申请 {} 无可用床位，目标科室: {}", application.getId(), application.getTargetDepartment());
            return toResponse(application);
        }

        if (application.getPriorityLevel() == PriorityLevel.EMERGENCY
                || application.getPriorityLevel() == PriorityLevel.URGENT) {
            application.setStatus(TransferStatus.CRITICAL_PRIORITY);
            log.info("转科申请 {} 为重症优先，优先级: {}", application.getId(), application.getPriorityLevel());
        } else {
            application.setStatus(TransferStatus.BED_ASSIGNED);
        }

        bedService.occupyBed(bestBed.getId(), application.getPatientAdmissionNumber(), application.getId());

        application.setAssignedBedId(bestBed.getId());
        application.setAssignedBedNumber(bestBed.getBedNumber());
        application.setAssignedTime(LocalDateTime.now());
        application.setOccupationDeadline(LocalDateTime.now().plusHours(OCCUPATION_TIMEOUT_HOURS));

        if (application.getAppointmentTime() == null) {
            application.setAppointmentTime(LocalDateTime.now().plusHours(1));
        }

        TransferApplication saved = transferRepository.save(application);
        log.info("转科申请 {} 已分配床位: {}，状态: {}，占床截止时间: {}",
                saved.getId(), bestBed.getBedNumber(), saved.getStatus(), saved.getOccupationDeadline());

        createFamilyNotification(saved);

        return toResponse(saved);
    }

    private void createFamilyNotification(TransferApplication application) {
        FamilyNotification notification = FamilyNotification.builder()
                .transferApplicationId(application.getId())
                .patientAdmissionNumber(application.getPatientAdmissionNumber())
                .familyContact("待录入")
                .notificationContent(String.format("患者 %s 已申请从 %s 转至 %s，床位: %s，预约时间: %s",
                        application.getPatientAdmissionNumber(),
                        application.getCurrentDepartment(),
                        application.getTargetDepartment(),
                        application.getAssignedBedNumber(),
                        application.getAppointmentTime()))
                .notified(false)
                .familyAgreed(true)
                .build();
        familyNotificationRepository.save(notification);
    }

    public int calculatePriorityScore(Patient patient, TransferRequest request) {
        int score = 0;

        switch (patient.getStatus()) {
            case CRITICAL: score += 40; break;
            case POSTOPERATIVE: score += 20; break;
            case STABLE: score += 10; break;
            case DISCHARGING: score += 5; break;
            default: break;
        }

        switch (request.getNursingLevel()) {
            case SPECIAL: score += 30; break;
            case LEVEL1: score += 25; break;
            case LEVEL2: score += 15; break;
            case LEVEL3: score += 5; break;
        }

        if (request.getIsolationRequirement() != IsolationType.NONE) {
            score += 15;
        }

        if (request.getRequiredBedType() == BedType.ICU
                || request.getRequiredBedType() == BedType.RESCUE) {
            score += 20;
        }

        return score;
    }

    private PriorityLevel determinePriorityLevel(int score) {
        if (score >= 70) return PriorityLevel.EMERGENCY;
        if (score >= 50) return PriorityLevel.URGENT;
        if (score >= 30) return PriorityLevel.NORMAL;
        return PriorityLevel.LOW;
    }

    public TransferResponse getTransferApplication(Long id) {
        TransferApplication application = transferRepository.findById(id)
                .orElseThrow(() -> new BusinessException("转科申请不存在: " + id));
        return toResponse(application);
    }

    public List<TransferResponse> getTransfersByPatient(String admissionNumber) {
        return transferRepository.findByPatientAdmissionNumber(admissionNumber).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TransferResponse> getTransfersByStatus(TransferStatus status) {
        return transferRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TransferResponse> getPendingTransfers() {
        List<TransferStatus> pendingStatuses = List.of(
                TransferStatus.PENDING,
                TransferStatus.NO_BED_AVAILABLE,
                TransferStatus.BED_ASSIGNED,
                TransferStatus.ISOLATION_REQUIRED,
                TransferStatus.CRITICAL_PRIORITY,
                TransferStatus.OCCUPIED
        );
        return transferRepository.findByStatusInOrderByPriorityScoreDesc(pendingStatuses).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TransferResponse> getTransfersByDepartment(String department) {
        return transferRepository.findByTargetDepartmentAndStatus(department, TransferStatus.BED_ASSIGNED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransferResponse cancelTransfer(Long id, String reason) {
        TransferApplication application = transferRepository.findById(id)
                .orElseThrow(() -> new BusinessException("转科申请不存在: " + id));

        if (application.getStatus() == TransferStatus.ADMISSION_CONFIRMED) {
            throw new BusinessException("已确认入科的转科申请无法取消");
        }
        if (application.getStatus() == TransferStatus.CANCELLED) {
            throw new BusinessException("转科申请已取消");
        }

        if (application.getAssignedBedId() != null) {
            bedService.releaseBed(application.getAssignedBedId());
            log.info("转科申请 {} 取消，释放床位: {}", id, application.getAssignedBedNumber());
        }

        application.setStatus(TransferStatus.CANCELLED);
        application.setCancelTime(LocalDateTime.now());
        application.setCancelReason(reason);
        TransferApplication saved = transferRepository.save(application);
        log.info("转科申请 {} 已取消，原因: {}", id, reason);
        return toResponse(saved);
    }

    @Transactional
    public TransferResponse updatePatientStatus(Long id, PatientStatus newStatus) {
        TransferApplication application = transferRepository.findById(id)
                .orElseThrow(() -> new BusinessException("转科申请不存在: " + id));

        if (application.getStatus() == TransferStatus.CANCELLED
                || application.getStatus() == TransferStatus.ADMISSION_CONFIRMED) {
            throw new BusinessException("转科申请状态不允许更新患者状态");
        }

        if (newStatus == PatientStatus.DECEASED || newStatus == PatientStatus.DISCHARGING) {
            if (application.getAssignedBedId() != null) {
                bedService.releaseBed(application.getAssignedBedId());
            }
            application.setStatus(TransferStatus.PATIENT_STATUS_CHANGED);
            application.setRemark("患者状态变更为: " + newStatus + "，自动取消并释放床位");
        } else {
            int newScore = recalculateScore(application, newStatus);
            application.setPriorityScore(newScore);
            application.setPriorityLevel(determinePriorityLevel(newScore));
        }

        Patient patient = patientRepository.findByAdmissionNumber(application.getPatientAdmissionNumber())
                .orElse(null);
        if (patient != null) {
            patient.setStatus(newStatus);
            patientRepository.save(patient);
        }

        TransferApplication saved = transferRepository.save(application);
        log.info("转科申请 {} 患者状态更新为: {}", id, newStatus);
        return toResponse(saved);
    }

    private int recalculateScore(TransferApplication application, PatientStatus newStatus) {
        int score = 0;
        switch (newStatus) {
            case CRITICAL: score += 40; break;
            case POSTOPERATIVE: score += 20; break;
            case STABLE: score += 10; break;
            case DISCHARGING: score += 5; break;
            default: break;
        }
        switch (application.getNursingLevel()) {
            case SPECIAL: score += 30; break;
            case LEVEL1: score += 25; break;
            case LEVEL2: score += 15; break;
            case LEVEL3: score += 5; break;
        }
        if (application.getIsolationRequirement() != IsolationType.NONE) {
            score += 15;
        }
        if (application.getRequiredBedType() == BedType.ICU
                || application.getRequiredBedType() == BedType.RESCUE) {
            score += 20;
        }
        return score;
    }

    @Transactional
    public void retryPendingTransfers() {
        List<TransferApplication> pendingApplications = transferRepository
                .findByStatusIn(List.of(TransferStatus.NO_BED_AVAILABLE, TransferStatus.ISOLATION_REQUIRED));
        for (TransferApplication app : pendingApplications) {
            attemptAssignBed(app);
        }
    }

    private TransferResponse toResponse(TransferApplication app) {
        return TransferResponse.builder()
                .id(app.getId())
                .patientAdmissionNumber(app.getPatientAdmissionNumber())
                .currentDepartment(app.getCurrentDepartment())
                .targetDepartment(app.getTargetDepartment())
                .requiredBedType(app.getRequiredBedType())
                .isolationRequirement(app.getIsolationRequirement())
                .nursingLevel(app.getNursingLevel())
                .priorityLevel(app.getPriorityLevel())
                .status(app.getStatus())
                .diagnosis(app.getDiagnosis())
                .reason(app.getReason())
                .applicantDoctorId(app.getApplicantDoctorId())
                .applicationTime(app.getApplicationTime())
                .assignedBedId(app.getAssignedBedId())
                .assignedBedNumber(app.getAssignedBedNumber())
                .assignedTime(app.getAssignedTime())
                .appointmentTime(app.getAppointmentTime())
                .occupationDeadline(app.getOccupationDeadline())
                .priorityScore(app.getPriorityScore())
                .remark(app.getRemark())
                .build();
    }
}
