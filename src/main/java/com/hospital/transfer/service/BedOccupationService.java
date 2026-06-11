package com.hospital.transfer.service;

import com.hospital.transfer.dto.TransferResponse;
import com.hospital.transfer.entity.Bed;
import com.hospital.transfer.entity.FamilyNotification;
import com.hospital.transfer.entity.NurseStationConfirmation;
import com.hospital.transfer.entity.TransferApplication;
import com.hospital.transfer.enums.TransferStatus;
import com.hospital.transfer.exception.BusinessException;
import com.hospital.transfer.repository.FamilyNotificationRepository;
import com.hospital.transfer.repository.NurseStationConfirmationRepository;
import com.hospital.transfer.repository.TransferApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BedOccupationService {

    private static final Logger log = LoggerFactory.getLogger(BedOccupationService.class);

    private final TransferApplicationRepository transferRepository;
    private final NurseStationConfirmationRepository nurseConfirmationRepository;
    private final FamilyNotificationRepository familyNotificationRepository;
    private final BedService bedService;

    public BedOccupationService(TransferApplicationRepository transferRepository,
                                NurseStationConfirmationRepository nurseConfirmationRepository,
                                FamilyNotificationRepository familyNotificationRepository,
                                BedService bedService) {
        this.transferRepository = transferRepository;
        this.nurseConfirmationRepository = nurseConfirmationRepository;
        this.familyNotificationRepository = familyNotificationRepository;
        this.bedService = bedService;
    }

    @Transactional
    public TransferResponse confirmOccupation(Long transferId) {
        TransferApplication application = transferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException("转科申请不存在: " + transferId));

        if (application.getStatus() != TransferStatus.BED_ASSIGNED
                && application.getStatus() != TransferStatus.ISOLATION_REQUIRED
                && application.getStatus() != TransferStatus.CRITICAL_PRIORITY) {
            throw new BusinessException("转科申请状态不允许占床确认，当前状态: " + application.getStatus());
        }

        if (application.getAssignedBedId() == null) {
            throw new BusinessException("转科申请未分配床位");
        }

        application.setStatus(TransferStatus.OCCUPIED);
        application.setRemark("患者已占床");
        TransferApplication saved = transferRepository.save(application);
        log.info("转科申请 {} 占床确认，床位: {}", transferId, application.getAssignedBedNumber());
        return toResponse(saved);
    }

    @Transactional
    public TransferResponse confirmAdmission(Long transferId) {
        TransferApplication application = transferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException("转科申请不存在: " + transferId));

        if (application.getStatus() != TransferStatus.OCCUPIED) {
            throw new BusinessException("转科申请状态不允许入科确认，当前状态: " + application.getStatus());
        }

        application.setStatus(TransferStatus.ADMISSION_CONFIRMED);
        application.setAdmissionConfirmedTime(LocalDateTime.now());

        if (application.getAssignedBedId() != null) {
            bedService.confirmAdmission(application.getAssignedBedId());
        }

        TransferApplication saved = transferRepository.save(application);
        log.info("转科申请 {} 入科确认完成，患者住院号: {}", transferId, application.getPatientAdmissionNumber());
        return toResponse(saved);
    }

    @Transactional
    public NurseStationConfirmation nurseStationConfirm(Long transferId, String nurseId, String nurseName,
                                                         Boolean accepted, String rejectReason,
                                                         LocalDateTime estimatedArrivalTime, String remark) {
        TransferApplication application = transferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException("转科申请不存在: " + transferId));

        if (application.getStatus() != TransferStatus.BED_ASSIGNED
                && application.getStatus() != TransferStatus.ISOLATION_REQUIRED
                && application.getStatus() != TransferStatus.CRITICAL_PRIORITY
                && application.getStatus() != TransferStatus.OCCUPIED) {
            throw new BusinessException("转科申请状态不允许护士站确认，当前状态: " + application.getStatus());
        }

        if (nurseConfirmationRepository.findByTransferApplicationId(transferId).isPresent()) {
            throw new BusinessException("该转科申请已有护士站确认记录");
        }

        NurseStationConfirmation confirmation = NurseStationConfirmation.builder()
                .transferApplicationId(transferId)
                .targetDepartment(application.getTargetDepartment())
                .nurseId(nurseId)
                .nurseName(nurseName)
                .confirmTime(LocalDateTime.now())
                .accepted(accepted)
                .rejectReason(rejectReason)
                .estimatedArrivalTime(estimatedArrivalTime)
                .remark(remark)
                .build();

        if (!accepted) {
            log.info("护士站拒绝接收转科申请 {}，原因: {}", transferId, rejectReason);
        } else {
            log.info("护士站确认接收转科申请 {}，预计到达时间: {}", transferId, estimatedArrivalTime);
        }

        return nurseConfirmationRepository.save(confirmation);
    }

    @Transactional
    public FamilyNotification notifyFamily(Long transferId, String familyContact, String content) {
        TransferApplication application = transferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException("转科申请不存在: " + transferId));

        FamilyNotification notification = familyNotificationRepository
                .findByTransferApplicationId(transferId)
                .orElse(null);

        if (notification == null) {
            notification = FamilyNotification.builder()
                    .transferApplicationId(transferId)
                    .patientAdmissionNumber(application.getPatientAdmissionNumber())
                    .familyContact(familyContact)
                    .notificationContent(content)
                    .build();
        } else {
            if (familyContact != null) notification.setFamilyContact(familyContact);
            if (content != null) notification.setNotificationContent(content);
        }

        notification.setNotified(true);
        notification.setNotificationTime(LocalDateTime.now());

        return familyNotificationRepository.save(notification);
    }

    @Transactional
    public FamilyNotification familyResponse(Long transferId, Boolean agreed, String refusalReason) {
        FamilyNotification notification = familyNotificationRepository
                .findByTransferApplicationId(transferId)
                .orElseThrow(() -> new BusinessException("未找到家属通知记录"));

        notification.setFamilyAgreed(agreed);
        notification.setResponseTime(LocalDateTime.now());

        if (!agreed) {
            notification.setRefusalReason(refusalReason);
            TransferApplication application = transferRepository.findById(transferId)
                    .orElseThrow(() -> new BusinessException("转科申请不存在"));

            application.setStatus(TransferStatus.FAMILY_REFUSED);
            application.setRemark("家属拒绝转科，原因: " + refusalReason);

            if (application.getAssignedBedId() != null) {
                bedService.releaseBed(application.getAssignedBedId());
                log.info("家属拒绝转科，释放床位: {}", application.getAssignedBedNumber());
            }

            transferRepository.save(application);
            log.info("转科申请 {} 家属拒绝，原因: {}", transferId, refusalReason);
        }

        return familyNotificationRepository.save(notification);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void checkOccupationTimeout() {
        LocalDateTime now = LocalDateTime.now();
        List<TransferApplication> occupiedTransfers = transferRepository
                .findByStatusIn(List.of(TransferStatus.OCCUPIED, TransferStatus.BED_ASSIGNED,
                        TransferStatus.CRITICAL_PRIORITY, TransferStatus.ISOLATION_REQUIRED));

        for (TransferApplication application : occupiedTransfers) {
            if (application.getOccupationDeadline() != null && now.isAfter(application.getOccupationDeadline())) {
                releaseOccupation(application, "占床超时，患者未按时转入");
            }
        }

        log.debug("占床超时检查完成，检查时间: {}", now);
    }

    @Transactional
    public TransferResponse releaseOccupation(Long transferId, String reason) {
        TransferApplication application = transferRepository.findById(transferId)
                .orElseThrow(() -> new BusinessException("转科申请不存在: " + transferId));
        return releaseOccupation(application, reason);
    }

    private TransferResponse releaseOccupation(TransferApplication application, String reason) {
        if (application.getAssignedBedId() != null) {
            bedService.releaseBed(application.getAssignedBedId());
            log.info("释放床位: {}，原因: {}", application.getAssignedBedNumber(), reason);
        }

        application.setStatus(TransferStatus.TIMEOUT_RELEASED);
        application.setCancelTime(LocalDateTime.now());
        application.setCancelReason(reason);
        application.setRemark(reason);

        TransferApplication saved = transferRepository.save(application);
        log.info("转科申请 {} 床位已释放，原因: {}", application.getId(), reason);
        return toResponse(saved);
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
