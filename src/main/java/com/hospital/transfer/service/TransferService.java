package com.hospital.transfer.service;

import com.hospital.transfer.dto.IsolationRestriction;
import com.hospital.transfer.dto.PriorityScoreDetail;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        List<PriorityScoreDetail> priorityDetails = calculatePriorityScoreWithDetails(patient, request);
        int priorityScore = priorityDetails.stream().mapToInt(PriorityScoreDetail::getScore).sum();
        PriorityLevel priorityLevel = determinePriorityLevel(priorityScore);

        IsolationRestriction isolationRestriction = bedService.checkIsolationRestriction(
                request.getTargetDepartment(),
                request.getRequiredBedType(),
                request.getIsolationRequirement(),
                request.getDiagnosis() != null ? request.getDiagnosis() : patient.getDiagnosis(),
                request.getReason()
        );

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

        TransferResponse response = attemptAssignBed(saved);
        response.setPriorityScoreDetails(priorityDetails);
        response.setIsolationRestriction(isolationRestriction);

        QueueInfo queueInfo = calculateQueuePosition(saved);
        response.setQueuePosition(queueInfo.position);
        response.setTotalWaiting(queueInfo.totalWaiting);

        return response;
    }

    private static class QueueInfo {
        int position;
        int totalWaiting;

        QueueInfo(int position, int totalWaiting) {
            this.position = position;
            this.totalWaiting = totalWaiting;
        }
    }

    private QueueInfo calculateQueuePosition(TransferApplication application) {
        List<TransferStatus> pendingStatuses = List.of(
                TransferStatus.PENDING,
                TransferStatus.NO_BED_AVAILABLE,
                TransferStatus.BED_ASSIGNED,
                TransferStatus.ISOLATION_REQUIRED,
                TransferStatus.CRITICAL_PRIORITY,
                TransferStatus.OCCUPIED
        );
        List<TransferApplication> allPending = transferRepository
                .findByStatusInOrderByPriorityScoreDesc(pendingStatuses);

        int position = 0;
        for (int i = 0; i < allPending.size(); i++) {
            if (allPending.get(i).getId().equals(application.getId())) {
                position = i + 1;
                break;
            }
        }
        if (position == 0) {
            position = allPending.size() + 1;
        }
        return new QueueInfo(position, allPending.size());
    }

    @Transactional
    public TransferResponse attemptAssignBed(TransferApplication application) {
        IsolationRestriction isolationRestriction = bedService.checkIsolationRestriction(
                application.getTargetDepartment(),
                application.getRequiredBedType(),
                application.getIsolationRequirement(),
                application.getDiagnosis(),
                application.getReason()
        );

        if (isolationRestriction != null) {
            application.setStatus(TransferStatus.ISOLATION_REQUIRED);
            application.setRemark(isolationRestriction.getMessage());
            application.setAssignedBedId(null);
            application.setAssignedBedNumber(null);
            transferRepository.save(application);
            log.warn("转科申请 {} 隔离限制: {} - {}", application.getId(),
                    isolationRestriction.getRestrictionType(), isolationRestriction.getMessage());
            TransferResponse response = toResponse(application);
            response.setIsolationRestriction(isolationRestriction);
            return response;
        }

        Bed bestBed = bedService.findBestMatchBed(
                application.getTargetDepartment(),
                application.getRequiredBedType(),
                application.getIsolationRequirement()
        );

        if (bestBed == null) {
            boolean isCritical = application.getPriorityLevel() == PriorityLevel.EMERGENCY
                    || application.getPriorityLevel() == PriorityLevel.URGENT;

            if (isCritical) {
                Bed preemptedBed = attemptPreemptBed(application);
                if (preemptedBed != null) {
                    bestBed = preemptedBed;
                    application.setStatus(TransferStatus.CRITICAL_PRIORITY);
                    application.setRemark("重症患者插队，已抢占床位 " + preemptedBed.getBedNumber());
                    log.info("转科申请 {} 为重症患者，成功抢占床位 {}", application.getId(), preemptedBed.getBedNumber());
                } else {
                    application.setStatus(TransferStatus.NO_BED_AVAILABLE);
                    application.setRemark("目标科室无可用床位，且无可抢占的低优先级床位，重症患者正在排队中");
                    application.setAssignedBedId(null);
                    application.setAssignedBedNumber(null);
                    transferRepository.save(application);
                    log.warn("转科申请 {} 为重症患者，但无可抢占床位", application.getId());
                    return toResponse(application);
                }
            } else {
                application.setStatus(TransferStatus.NO_BED_AVAILABLE);
                application.setRemark("目标科室无可用床位");
                application.setAssignedBedId(null);
                application.setAssignedBedNumber(null);
                transferRepository.save(application);
                log.warn("转科申请 {} 无可用床位，目标科室: {}", application.getId(), application.getTargetDepartment());
                return toResponse(application);
            }
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

    private Bed attemptPreemptBed(TransferApplication criticalApplication) {
        List<TransferApplication> lowerPriorityAssignments = transferRepository
                .findByTargetDepartmentAndStatus(
                        criticalApplication.getTargetDepartment(),
                        TransferStatus.BED_ASSIGNED
                ).stream()
                .filter(app -> {
                    PriorityLevel appLevel = app.getPriorityLevel();
                    PriorityLevel criticalLevel = criticalApplication.getPriorityLevel();
                    return (criticalLevel == PriorityLevel.EMERGENCY
                            && (appLevel == PriorityLevel.NORMAL || appLevel == PriorityLevel.LOW))
                            || (criticalLevel == PriorityLevel.URGENT && appLevel == PriorityLevel.LOW);
                })
                .sorted((a, b) -> {
                    int scoreCompare = Integer.compare(a.getPriorityScore(), b.getPriorityScore());
                    if (scoreCompare != 0) return scoreCompare;
                    return a.getApplicationTime().compareTo(b.getApplicationTime());
                })
                .collect(Collectors.toList());

        for (TransferApplication lowerApp : lowerPriorityAssignments) {
            if (lowerApp.getAssignedBedId() != null) {
                Bed bedToPreempt = bedService.getBed(lowerApp.getAssignedBedId());

                if (criticalApplication.getIsolationRequirement() != IsolationType.NONE) {
                    if (bedToPreempt.getIsolationType() != criticalApplication.getIsolationRequirement()) {
                        continue;
                    }
                } else {
                    if (bedToPreempt.getIsolationType() != IsolationType.NONE) {
                        continue;
                    }
                }

                if (bedToPreempt.getBedType() != criticalApplication.getRequiredBedType()
                        && criticalApplication.getRequiredBedType() != BedType.GENERAL) {
                    continue;
                }

                bedService.releaseBed(lowerApp.getAssignedBedId());

                lowerApp.setStatus(TransferStatus.NO_BED_AVAILABLE);
                lowerApp.setAssignedBedId(null);
                lowerApp.setAssignedBedNumber(null);
                lowerApp.setOccupationDeadline(null);
                lowerApp.setRemark("床位被重症患者 " + criticalApplication.getPatientAdmissionNumber() + " 抢占，重新排队");
                transferRepository.save(lowerApp);

                log.info("重症患者抢占床位成功: 申请ID {} 抢占了申请ID {} 的床位 {}",
                        criticalApplication.getId(), lowerApp.getId(), bedToPreempt.getBedNumber());

                return bedToPreempt;
            }
        }

        return null;
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
        return calculatePriorityScoreWithDetails(patient, request).stream()
                .mapToInt(PriorityScoreDetail::getScore)
                .sum();
    }

    public List<PriorityScoreDetail> calculatePriorityScoreWithDetails(Patient patient, TransferRequest request) {
        List<PriorityScoreDetail> details = new ArrayList<>();

        int statusScore = 0;
        String statusDesc = "";
        switch (patient.getStatus()) {
            case CRITICAL:
                statusScore = 40;
                statusDesc = "患者病情危重";
                break;
            case POSTOPERATIVE:
                statusScore = 20;
                statusDesc = "患者术后恢复期";
                break;
            case STABLE:
                statusScore = 10;
                statusDesc = "患者病情稳定";
                break;
            case DISCHARGING:
                statusScore = 5;
                statusDesc = "患者即将出院";
                break;
            default:
                break;
        }
        if (statusScore > 0) {
            details.add(PriorityScoreDetail.builder()
                    .factor("PATIENT_STATUS")
                    .score(statusScore)
                    .description(statusDesc)
                    .build());
        }

        int nursingScore = 0;
        String nursingDesc = "";
        switch (request.getNursingLevel()) {
            case SPECIAL:
                nursingScore = 30;
                nursingDesc = "特级护理";
                break;
            case LEVEL1:
                nursingScore = 25;
                nursingDesc = "一级护理";
                break;
            case LEVEL2:
                nursingScore = 15;
                nursingDesc = "二级护理";
                break;
            case LEVEL3:
                nursingScore = 5;
                nursingDesc = "三级护理";
                break;
        }
        details.add(PriorityScoreDetail.builder()
                .factor("NURSING_LEVEL")
                .score(nursingScore)
                .description(nursingDesc)
                .build());

        if (request.getIsolationRequirement() != IsolationType.NONE) {
            details.add(PriorityScoreDetail.builder()
                    .factor("ISOLATION_REQUIRED")
                    .score(15)
                    .description("需要" + request.getIsolationRequirement() + "隔离")
                    .build());
        }

        if (request.getRequiredBedType() == BedType.ICU
                || request.getRequiredBedType() == BedType.RESCUE) {
            details.add(PriorityScoreDetail.builder()
                    .factor("BED_TYPE_CRITICAL")
                    .score(20)
                    .description("需要" + request.getRequiredBedType() + "床位")
                    .build());
        }

        if (request.getAppointmentTime() != null) {
            long hoursSinceApplication = Duration.between(request.getAppointmentTime(), LocalDateTime.now()).toHours();
            if (hoursSinceApplication > 2) {
                int timeScore = Math.min((int) (hoursSinceApplication / 2), 10);
                details.add(PriorityScoreDetail.builder()
                        .factor("WAITING_TIME")
                        .score(timeScore)
                        .description("等待时间超过" + hoursSinceApplication + "小时")
                        .build());
            }
        }

        if (request.getReason() != null) {
            String lowerReason = request.getReason().toLowerCase();
            if (lowerReason.contains("紧急") || lowerReason.contains("urgent")
                    || lowerReason.contains("急诊") || lowerReason.contains("emergency")) {
                details.add(PriorityScoreDetail.builder()
                        .factor("DOCTOR_REMARK")
                        .score(10)
                        .description("医生备注: " + request.getReason())
                        .build());
            }
        }

        return details;
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
        TransferRequest tempRequest = TransferRequest.builder()
                .nursingLevel(application.getNursingLevel())
                .isolationRequirement(application.getIsolationRequirement())
                .requiredBedType(application.getRequiredBedType())
                .appointmentTime(application.getApplicationTime())
                .reason(application.getReason())
                .build();

        Patient tempPatient = Patient.builder()
                .status(newStatus)
                .build();

        return calculatePriorityScore(tempPatient, tempRequest);
    }

    @Transactional
    public void retryPendingTransfers() {
        List<TransferApplication> pendingApplications = transferRepository
                .findByStatusIn(List.of(TransferStatus.NO_BED_AVAILABLE, TransferStatus.ISOLATION_REQUIRED))
                .stream()
                .sorted((a, b) -> {
                    int scoreCompare = Integer.compare(b.getPriorityScore(), a.getPriorityScore());
                    if (scoreCompare != 0) return scoreCompare;
                    return a.getApplicationTime().compareTo(b.getApplicationTime());
                })
                .collect(Collectors.toList());

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
