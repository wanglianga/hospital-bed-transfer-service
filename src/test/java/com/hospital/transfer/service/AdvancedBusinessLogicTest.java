package com.hospital.transfer.service;

import com.hospital.transfer.dto.IsolationRestriction;
import com.hospital.transfer.dto.PriorityScoreDetail;
import com.hospital.transfer.dto.TransferRequest;
import com.hospital.transfer.dto.TransferResponse;
import com.hospital.transfer.entity.Bed;
import com.hospital.transfer.entity.Patient;
import com.hospital.transfer.entity.TransferApplication;
import com.hospital.transfer.enums.*;
import com.hospital.transfer.repository.BedRepository;
import com.hospital.transfer.repository.PatientRepository;
import com.hospital.transfer.repository.TransferApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdvancedBusinessLogicTest {

    @Autowired
    private BedService bedService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TransferApplicationRepository transferRepository;

    @BeforeEach
    void cleanDatabase() {
        transferRepository.deleteAll();
        patientRepository.deleteAll();
    }

    private Patient createAndSavePatient(String admission, String dept, NursingLevel level,
                                         IsolationType iso, PatientStatus status) {
        Patient p = new Patient();
        p.setAdmissionNumber(admission);
        p.setName("Test-" + admission);
        p.setDiagnosis("Test Diagnosis");
        p.setCurrentDepartment(dept);
        p.setNursingLevel(level);
        p.setIsolationRequirement(iso);
        p.setStatus(status);
        p.setAdmissionTime(LocalDateTime.now());
        return patientRepository.save(p);
    }

    private Bed createAndSaveBed(String bedNumber, String dept, BedType bedType, IsolationType iso,
                                  String roomNumber, String wardNumber) {
        Bed b = new Bed();
        b.setBedNumber(bedNumber);
        b.setDepartment(dept);
        b.setBedType(bedType);
        b.setIsolationType(iso);
        b.setOccupied(false);
        b.setEnabled(true);
        b.setRoomNumber(roomNumber);
        b.setWardNumber(wardNumber);
        return bedRepository.save(b);
    }

    private TransferRequest createTransferRequest(String admission, String targetDept, BedType bedType,
                                                  IsolationType iso, NursingLevel level, String diagnosis,
                                                  String reason) {
        return TransferRequest.builder()
                .patientAdmissionNumber(admission)
                .targetDepartment(targetDept)
                .requiredBedType(bedType)
                .isolationRequirement(iso)
                .nursingLevel(level)
                .diagnosis(diagnosis)
                .reason(reason)
                .applicantDoctorId("DR001")
                .build();
    }

    // ============ 隔离限制测试 ============

    @Test
    @DisplayName("隔离患者 - 高风险感染病返回隔离限制详情")
    void testIsolationRestriction_HighRiskInfection() {
        Patient patient = createAndSavePatient("ISO-HIGH-001", "Emergency",
                NursingLevel.LEVEL1, IsolationType.AIRBORNE, PatientStatus.CRITICAL);

        IsolationRestriction restriction = bedService.checkIsolationRestriction(
                "NoIsolationWard", BedType.GENERAL, IsolationType.AIRBORNE,
                "Active TB", "Need immediate care");

        assertNotNull(restriction, "高风险传染病应返回隔离限制");
        assertEquals("WARD_UNSUITABLE", restriction.getRestrictionType());
        assertEquals(IsolationType.AIRBORNE, restriction.getRequiredIsolationType());
        assertNotNull(restriction.getMessage());
        assertNotNull(restriction.getDetail());
        assertTrue(restriction.getMessage().contains("不具备"));
    }

    @Test
    @DisplayName("隔离患者 - 无匹配隔离床位返回NO_ISOLATION_BED")
    void testIsolationRestriction_NoIsolationBed() {
        Patient patient = createAndSavePatient("ISO-NOBED-001", "Emergency",
                NursingLevel.LEVEL1, IsolationType.CONTACT, PatientStatus.STABLE);

        createAndSaveBed("AIRBORNE-ONLY", "IsoTestDept", BedType.GENERAL, IsolationType.AIRBORNE,
                "ROOM-1", "WARD-A");
        createAndSaveBed("NORMAL-ONLY", "IsoTestDept", BedType.GENERAL, IsolationType.NONE,
                "ROOM-2", "WARD-A");

        IsolationRestriction restriction = bedService.checkIsolationRestriction(
                "IsoTestDept", BedType.GENERAL, IsolationType.CONTACT,
                "MDR Infection", "Need isolation");

        assertNotNull(restriction, "无CONTACT隔离床应返回限制");
        assertEquals("NO_ISOLATION_BED", restriction.getRestrictionType());
    }

    @Test
    @DisplayName("隔离患者 - 房间混住限制测试")
    void testIsolationRestriction_RoomConflict() {
        Patient patient = createAndSavePatient("ISO-ROOM-001", "Emergency",
                NursingLevel.LEVEL1, IsolationType.CONTACT, PatientStatus.STABLE);

        Bed normalBedInRoom = createAndSaveBed("NORMAL-BED", "RoomConflictDept",
                BedType.GENERAL, IsolationType.NONE, "ROOM-101", "WARD-A");
        normalBedInRoom.setOccupied(true);
        normalBedInRoom.setOccupiedByAdmissionNumber("NORMAL-PATIENT-001");
        bedRepository.save(normalBedInRoom);

        createAndSaveBed("CONTACT-BED", "RoomConflictDept",
                BedType.GENERAL, IsolationType.CONTACT, "ROOM-101", "WARD-A");

        TransferRequest request = createTransferRequest("ISO-ROOM-001", "RoomConflictDept",
                BedType.GENERAL, IsolationType.CONTACT, NursingLevel.LEVEL1,
                "MDR", "Need isolation");

        TransferResponse response = transferService.applyTransfer(request);

        assertNotNull(response.getIsolationRestriction(), "房间有非隔离患者时应返回限制");
        assertEquals("ROOM_CONFLICT", response.getIsolationRestriction().getRestrictionType());
        assertEquals(TransferStatus.ISOLATION_REQUIRED, response.getStatus());
    }

    @Test
    @DisplayName("隔离患者 - 同房间不同隔离类型冲突")
    void testIsolationRestriction_DifferentIsolationInSameRoom() {
        createAndSaveBed("AIRBORNE-BED", "DiffIsoDept",
                BedType.GENERAL, IsolationType.AIRBORNE, "ROOM-201", "WARD-B");
        Bed occupiedAirborneBed = createAndSaveBed("AIRBORNE-BED-2", "DiffIsoDept",
                BedType.GENERAL, IsolationType.AIRBORNE, "ROOM-201", "WARD-B");
        occupiedAirborneBed.setOccupied(true);
        occupiedAirborneBed.setOccupiedByAdmissionNumber("AIRBORNE-PATIENT");
        bedRepository.save(occupiedAirborneBed);

        createAndSavePatient("CONTACT-PATIENT", "Emergency",
                NursingLevel.LEVEL1, IsolationType.CONTACT, PatientStatus.STABLE);

        TransferRequest request = createTransferRequest("CONTACT-PATIENT", "DiffIsoDept",
                BedType.GENERAL, IsolationType.CONTACT, NursingLevel.LEVEL1,
                "MRSA", "Contact isolation needed");

        TransferResponse response = transferService.applyTransfer(request);

        assertEquals(TransferStatus.ISOLATION_REQUIRED, response.getStatus());
    }

    // ============ 优先级计算明细测试 ============

    @Test
    @DisplayName("优先级计算 - 返回明细列表，保留排序依据")
    void testCalculatePriorityScore_DetailsReturned() {
        Patient criticalPatient = createAndSavePatient("PRIORITY-001", "ED",
                NursingLevel.SPECIAL, IsolationType.AIRBORNE, PatientStatus.CRITICAL);

        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("PRIORITY-001")
                .targetDepartment("ICU")
                .requiredBedType(BedType.ICU)
                .isolationRequirement(IsolationType.AIRBORNE)
                .nursingLevel(NursingLevel.SPECIAL)
                .diagnosis("Severe pneumonia")
                .reason("紧急，需要立即处理")
                .applicantDoctorId("DR001")
                .build();

        List<PriorityScoreDetail> details = transferService.calculatePriorityScoreWithDetails(criticalPatient, request);

        assertNotNull(details, "应返回优先级明细");
        assertFalse(details.isEmpty(), "明细列表不应为空");

        int totalScore = details.stream().mapToInt(PriorityScoreDetail::getScore).sum();
        assertTrue(totalScore >= 90, "危重+特级+隔离+ICU+紧急备注 应得高分，实际: " + totalScore);

        boolean hasPatientStatus = details.stream().anyMatch(d -> "PATIENT_STATUS".equals(d.getFactor()));
        boolean hasNursingLevel = details.stream().anyMatch(d -> "NURSING_LEVEL".equals(d.getFactor()));
        boolean hasIsolation = details.stream().anyMatch(d -> "ISOLATION_REQUIRED".equals(d.getFactor()));
        boolean hasBedType = details.stream().anyMatch(d -> "BED_TYPE_CRITICAL".equals(d.getFactor()));
        boolean hasDoctorRemark = details.stream().anyMatch(d -> "DOCTOR_REMARK".equals(d.getFactor()));

        assertTrue(hasPatientStatus, "应包含患者状态因素");
        assertTrue(hasNursingLevel, "应包含护理等级因素");
        assertTrue(hasIsolation, "应包含隔离要求因素");
        assertTrue(hasBedType, "应包含床位类型因素");
        assertTrue(hasDoctorRemark, "应包含医生备注因素");

        for (PriorityScoreDetail detail : details) {
            assertNotNull(detail.getFactor(), "因素名称不应为空");
            assertNotNull(detail.getDescription(), "描述不应为空");
            assertTrue(detail.getScore() > 0, "分数应大于0");
        }
    }

    @Test
    @DisplayName("等待时间加分测试")
    void testCalculatePriorityScore_WaitingTimeBonus() {
        Patient patient = createAndSavePatient("WAIT-001", "ED",
                NursingLevel.LEVEL2, IsolationType.NONE, PatientStatus.STABLE);

        TransferRequest oldRequest = TransferRequest.builder()
                .patientAdmissionNumber("WAIT-001")
                .targetDepartment("GeneralSurgery")
                .requiredBedType(BedType.GENERAL)
                .isolationRequirement(IsolationType.NONE)
                .nursingLevel(NursingLevel.LEVEL2)
                .appointmentTime(LocalDateTime.now().minusHours(6))
                .applicantDoctorId("DR001")
                .build();

        List<PriorityScoreDetail> oldDetails = transferService.calculatePriorityScoreWithDetails(patient, oldRequest);
        int oldScore = oldDetails.stream().mapToInt(PriorityScoreDetail::getScore).sum();

        TransferRequest newRequest = TransferRequest.builder()
                .patientAdmissionNumber("WAIT-001")
                .targetDepartment("GeneralSurgery")
                .requiredBedType(BedType.GENERAL)
                .isolationRequirement(IsolationType.NONE)
                .nursingLevel(NursingLevel.LEVEL2)
                .appointmentTime(LocalDateTime.now())
                .applicantDoctorId("DR001")
                .build();

        List<PriorityScoreDetail> newDetails = transferService.calculatePriorityScoreWithDetails(patient, newRequest);
        int newScore = newDetails.stream().mapToInt(PriorityScoreDetail::getScore).sum();

        assertTrue(oldScore > newScore, "等待时间长的申请应获得更高分数");
        assertTrue(oldDetails.stream().anyMatch(d -> "WAITING_TIME".equals(d.getFactor())),
                "等待超过2小时应有等待时间加分");
    }

    // ============ 重症插队测试 ============

    @Test
    @DisplayName("重症患者插队 - EMERGENCY优先级可抢占NORMAL优先级床位")
    void testCriticalPatientPreempt_NormalPriority() {
        createAndSavePatient("CRITICAL-001", "ED",
                NursingLevel.SPECIAL, IsolationType.NONE, PatientStatus.CRITICAL);
        Patient normalPatient = createAndSavePatient("NORMAL-001", "ED",
                NursingLevel.LEVEL2, IsolationType.NONE, PatientStatus.STABLE);

        createAndSaveBed("ONLY-BED", "PreemptDept", BedType.GENERAL, IsolationType.NONE,
                "ROOM-1", "WARD-A");

        TransferRequest normalRequest = createTransferRequest("NORMAL-001", "PreemptDept",
                BedType.GENERAL, IsolationType.NONE, NursingLevel.LEVEL2,
                "Routine check", "Elective admission");
        TransferResponse normalResponse = transferService.applyTransfer(normalRequest);
        assertEquals(TransferStatus.BED_ASSIGNED, normalResponse.getStatus(),
                "普通患者应先分配到床位");
        String assignedBedNumber = normalResponse.getAssignedBedNumber();

        TransferRequest criticalRequest = createTransferRequest("CRITICAL-001", "PreemptDept",
                BedType.GENERAL, IsolationType.NONE, NursingLevel.SPECIAL,
                "Acute myocardial infarction", "紧急，需要立即处理");
        TransferResponse criticalResponse = transferService.applyTransfer(criticalRequest);

        assertEquals(TransferStatus.CRITICAL_PRIORITY, criticalResponse.getStatus(),
                "重症患者应抢占成功并获得CRITICAL_PRIORITY状态");
        assertEquals(assignedBedNumber, criticalResponse.getAssignedBedNumber(),
                "重症患者应抢占到相同床位");

        List<TransferApplication> normalApps = transferRepository.findByPatientAdmissionNumber("NORMAL-001");
        assertFalse(normalApps.isEmpty());
        assertEquals(TransferStatus.NO_BED_AVAILABLE, normalApps.get(0).getStatus(),
                "被抢占的普通患者应变为NO_BED_AVAILABLE状态");
        assertNotNull(normalApps.get(0).getRemark(),
                "被抢占的申请应有备注说明");
        assertTrue(normalApps.get(0).getRemark().contains("抢占"),
                "备注应包含抢占信息");
    }

    @Test
    @DisplayName("重症患者插队 - URGENT优先级不可抢占EMERGENCY优先级床位")
    void testCriticalPatientPreempt_UrgentCannotPreemptEmergency() {
        createAndSavePatient("EMERGENCY-001", "ED",
                NursingLevel.SPECIAL, IsolationType.NONE, PatientStatus.CRITICAL);
        createAndSavePatient("URGENT-001", "ED",
                NursingLevel.LEVEL1, IsolationType.NONE, PatientStatus.POSTOPERATIVE);

        createAndSaveBed("ONLY-ICU-BED", "ICU", BedType.ICU, IsolationType.NONE,
                "ICU-ROOM-1", "ICU-WARD");

        List<Bed> existingIcuBeds = bedRepository.findByDepartment("ICU");
        for (Bed bed : existingIcuBeds) {
            if (!bed.getBedNumber().equals("ONLY-ICU-BED")) {
                bed.setEnabled(false);
                bedRepository.save(bed);
            }
        }

        TransferRequest emergencyRequest = createTransferRequest("EMERGENCY-001", "ICU",
                BedType.ICU, IsolationType.NONE, NursingLevel.SPECIAL,
                "Cardiac arrest", "最紧急");
        TransferResponse emergencyResponse = transferService.applyTransfer(emergencyRequest);
        assertEquals(TransferStatus.CRITICAL_PRIORITY, emergencyResponse.getStatus());
        String emergencyBed = emergencyResponse.getAssignedBedNumber();

        TransferRequest urgentRequest = createTransferRequest("URGENT-001", "ICU",
                BedType.ICU, IsolationType.NONE, NursingLevel.LEVEL1,
                "Post surgery monitoring", "紧急");
        TransferResponse urgentResponse = transferService.applyTransfer(urgentRequest);

        assertEquals(TransferStatus.NO_BED_AVAILABLE, urgentResponse.getStatus(),
                "URGENT优先级不能抢占EMERGENCY优先级的床位");
        assertNull(urgentResponse.getAssignedBedId(),
                "URGENT患者不应分配到床位");

        List<TransferApplication> emergencyApps = transferRepository.findByPatientAdmissionNumber("EMERGENCY-001");
        assertEquals(TransferStatus.CRITICAL_PRIORITY, emergencyApps.get(0).getStatus(),
                "EMERGENCY患者的床位不应被抢占");
        assertEquals(emergencyBed, emergencyApps.get(0).getAssignedBedNumber(),
                "EMERGENCY患者应保留原床位");
    }

    @Test
    @DisplayName("重症患者插队 - 隔离床位不被非隔离重症抢占")
    void testCriticalPatientPreempt_IsolationBedProtected() {
        createAndSavePatient("ISO-PATIENT", "ED",
                NursingLevel.LEVEL3, IsolationType.CONTACT, PatientStatus.STABLE);
        createAndSavePatient("CRITICAL-NON-ISO", "ED",
                NursingLevel.SPECIAL, IsolationType.NONE, PatientStatus.CRITICAL);

        createAndSaveBed("ISO-BED-1", "IsoPreemptDept", BedType.GENERAL, IsolationType.CONTACT,
                "ISO-ROOM-1", "ISO-WARD");

        TransferRequest isoRequest = createTransferRequest("ISO-PATIENT", "IsoPreemptDept",
                BedType.GENERAL, IsolationType.CONTACT, NursingLevel.LEVEL3,
                "MRSA", "Routine contact isolation");
        TransferResponse isoResponse = transferService.applyTransfer(isoRequest);
        assertEquals(TransferStatus.BED_ASSIGNED, isoResponse.getStatus());

        TransferRequest criticalRequest = createTransferRequest("CRITICAL-NON-ISO", "IsoPreemptDept",
                BedType.GENERAL, IsolationType.NONE, NursingLevel.SPECIAL,
                "Sepsis", "紧急");
        TransferResponse criticalResponse = transferService.applyTransfer(criticalRequest);

        assertEquals(TransferStatus.NO_BED_AVAILABLE, criticalResponse.getStatus(),
                "非隔离重症患者不应抢占隔离床位");

        List<TransferApplication> isoApps = transferRepository.findByPatientAdmissionNumber("ISO-PATIENT");
        assertEquals(TransferStatus.BED_ASSIGNED, isoApps.get(0).getStatus(),
                "隔离患者的床位不应被非隔离患者抢占");
    }

    // ============ 队列位置测试 ============

    @Test
    @DisplayName("队列位置 - 高优先级申请排在前面")
    void testQueuePosition_HighPriorityFirst() {
        createAndSavePatient("LOW-PATIENT", "ED",
                NursingLevel.LEVEL3, IsolationType.NONE, PatientStatus.STABLE);
        createAndSavePatient("HIGH-PATIENT", "ED",
                NursingLevel.SPECIAL, IsolationType.NONE, PatientStatus.CRITICAL);

        createAndSaveBed("QUEUE-BED-1", "QueueDept", BedType.GENERAL, IsolationType.NONE,
                "ROOM-1", "WARD-A");

        TransferRequest lowRequest = createTransferRequest("LOW-PATIENT", "QueueDept",
                BedType.GENERAL, IsolationType.NONE, NursingLevel.LEVEL3,
                "Routine", "Elective");
        TransferResponse lowResponse = transferService.applyTransfer(lowRequest);
        assertEquals(TransferStatus.BED_ASSIGNED, lowResponse.getStatus());

        TransferRequest highRequest = createTransferRequest("HIGH-PATIENT", "QueueDept",
                BedType.GENERAL, IsolationType.NONE, NursingLevel.SPECIAL,
                "Critical care", "紧急");
        TransferResponse highResponse = transferService.applyTransfer(highRequest);

        assertNotNull(highResponse.getQueuePosition(), "应返回队列位置");
        assertNotNull(highResponse.getTotalWaiting(), "应返回总等待数");
        assertTrue(highResponse.getQueuePosition() <= highResponse.getTotalWaiting(),
                "队列位置不应超过总等待数");

        assertNotNull(highResponse.getPriorityScoreDetails(), "应返回优先级明细");
        assertFalse(highResponse.getPriorityScoreDetails().isEmpty(),
                "优先级明细不应为空");
    }

    // ============ 重试机制测试 ============

    @Test
    @DisplayName("重试机制 - 按优先级排序重试")
    void testRetryPendingTransfers_PriorityOrder() {
        createAndSavePatient("LOW-RETRY", "ED",
                NursingLevel.LEVEL3, IsolationType.NONE, PatientStatus.STABLE);
        createAndSavePatient("HIGH-RETRY", "ED",
                NursingLevel.SPECIAL, IsolationType.NONE, PatientStatus.CRITICAL);
        createAndSavePatient("MID-RETRY", "ED",
                NursingLevel.LEVEL1, IsolationType.NONE, PatientStatus.POSTOPERATIVE);

        TransferRequest lowRequest = createTransferRequest("LOW-RETRY", "RetryDept",
                BedType.GENERAL, IsolationType.NONE, NursingLevel.LEVEL3,
                "Routine", "");
        transferService.applyTransfer(lowRequest);

        TransferRequest highRequest = createTransferRequest("HIGH-RETRY", "RetryDept",
                BedType.GENERAL, IsolationType.NONE, NursingLevel.SPECIAL,
                "Critical", "紧急");
        transferService.applyTransfer(highRequest);

        TransferRequest midRequest = createTransferRequest("MID-RETRY", "RetryDept",
                BedType.GENERAL, IsolationType.NONE, NursingLevel.LEVEL1,
                "Post-op", "");
        transferService.applyTransfer(midRequest);

        createAndSaveBed("RETRY-BED", "RetryDept", BedType.GENERAL, IsolationType.NONE,
                "ROOM-1", "WARD-A");

        transferService.retryPendingTransfers();

        List<TransferApplication> highApps = transferRepository.findByPatientAdmissionNumber("HIGH-RETRY");
        assertEquals(TransferStatus.CRITICAL_PRIORITY, highApps.get(0).getStatus(),
                "高优先级应先获得床位");
        assertNotNull(highApps.get(0).getAssignedBedId());

        List<TransferApplication> midApps = transferRepository.findByPatientAdmissionNumber("MID-RETRY");
        assertEquals(TransferStatus.NO_BED_AVAILABLE, midApps.get(0).getStatus(),
                "中优先级仍应等待");

        List<TransferApplication> lowApps = transferRepository.findByPatientAdmissionNumber("LOW-RETRY");
        assertEquals(TransferStatus.NO_BED_AVAILABLE, lowApps.get(0).getStatus(),
                "低优先级仍应等待");
    }
}
