package com.hospital.transfer.service;

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
class CoreBusinessLogicTest {

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

    private Bed createAndSaveBed(String bedNumber, String dept, BedType bedType, IsolationType iso) {
        Bed b = new Bed();
        b.setBedNumber(bedNumber);
        b.setDepartment(dept);
        b.setBedType(bedType);
        b.setIsolationType(iso);
        b.setOccupied(false);
        b.setEnabled(true);
        return bedRepository.save(b);
    }

    // ============ 床位余量统计测试 ============

    @Test
    @DisplayName("全院可用床位统计 - countAllAvailableBeds")
    void testCountAllAvailableBeds() {
        long count = bedService.countAllAvailableBeds();
        assertTrue(count >= 18, "全院至少有18个初始可用床位，实际: " + count);

        createAndSaveBed("TEST-BED-1", "TestDept", BedType.GENERAL, IsolationType.NONE);
        createAndSaveBed("TEST-BED-2", "TestDept", BedType.GENERAL, IsolationType.NONE);

        long newCount = bedService.countAllAvailableBeds();
        assertEquals(count + 2, newCount, "新增2个后应增加2");
    }

    @Test
    @DisplayName("按科室统计可用床位 - countAvailableBeds")
    void testCountAvailableBeds_ByDepartment() {
        createAndSaveBed("STAT-1", "StatDept", BedType.GENERAL, IsolationType.NONE);
        createAndSaveBed("STAT-2", "StatDept", BedType.GENERAL, IsolationType.NONE);
        createAndSaveBed("STAT-3", "StatDept", BedType.ICU, IsolationType.AIRBORNE);

        long count = bedService.countAvailableBeds("StatDept");
        assertEquals(3, count, "StatDept应有3个可用床位");
    }

    @Test
    @DisplayName("按科室+床型统计 - countAvailableBedsByType")
    void testCountAvailableBedsByType() {
        createAndSaveBed("TYPE-1", "TypeDept", BedType.GENERAL, IsolationType.NONE);
        createAndSaveBed("TYPE-2", "TypeDept", BedType.GENERAL, IsolationType.NONE);
        createAndSaveBed("TYPE-3", "TypeDept", BedType.ICU, IsolationType.NONE);

        long generalCount = bedService.countAvailableBedsByType("TypeDept", BedType.GENERAL);
        long icuCount = bedService.countAvailableBedsByType("TypeDept", BedType.ICU);

        assertEquals(2, generalCount, "TypeDept应有2个普通床");
        assertEquals(1, icuCount, "TypeDept应有1个ICU床");
    }

    @Test
    @DisplayName("按科室+隔离类型统计 - countAvailableBedsByIsolation")
    void testCountAvailableBedsByIsolation() {
        createAndSaveBed("ISO-1", "IsoDept", BedType.ISOLATION, IsolationType.CONTACT);
        createAndSaveBed("ISO-2", "IsoDept", BedType.ISOLATION, IsolationType.CONTACT);
        createAndSaveBed("ISO-3", "IsoDept", BedType.ISOLATION, IsolationType.AIRBORNE);
        createAndSaveBed("ISO-4", "IsoDept", BedType.GENERAL, IsolationType.NONE);

        long contactCount = bedService.countAvailableBedsByIsolation("IsoDept", IsolationType.CONTACT);
        long airborneCount = bedService.countAvailableBedsByIsolation("IsoDept", IsolationType.AIRBORNE);
        long noneCount = bedService.countAvailableBedsByIsolation("IsoDept", IsolationType.NONE);

        assertEquals(2, contactCount, "IsoDept应有2个CONTACT隔离床");
        assertEquals(1, airborneCount, "IsoDept应有1个AIRBORNE隔离床");
        assertEquals(1, noneCount, "IsoDept应有1个非隔离床");
    }

    @Test
    @DisplayName("仅按床型全院统计 - countAvailableBedsOnlyByType")
    void testCountAvailableBedsOnlyByType() {
        createAndSaveBed("ONLY-ICU-1", "ADept", BedType.ICU, IsolationType.NONE);
        createAndSaveBed("ONLY-ICU-2", "BDept", BedType.ICU, IsolationType.AIRBORNE);
        createAndSaveBed("ONLY-GEN-1", "CDept", BedType.GENERAL, IsolationType.NONE);

        long beforeIcu = bedService.countAvailableBedsOnlyByType(BedType.ICU);
        assertTrue(beforeIcu >= 2, "全院ICU床型应至少2个(新增的)");
    }

    @Test
    @DisplayName("仅按隔离类型全院统计 - countAvailableBedsOnlyByIsolation")
    void testCountAvailableBedsOnlyByIsolation() {
        createAndSaveBed("ONLY-ISO-C1", "XDept", BedType.GENERAL, IsolationType.CONTACT);
        createAndSaveBed("ONLY-ISO-C2", "YDept", BedType.ICU, IsolationType.CONTACT);

        long count = bedService.countAvailableBedsOnlyByIsolation(IsolationType.CONTACT);
        assertTrue(count >= 2, "全院CONTACT隔离床应至少2个(新增的)");
    }

    // ============ 隔离床匹配测试 ============

    @Test
    @DisplayName("隔离床匹配成功 - findBestMatchBed找到匹配隔离床")
    void testFindBestMatchBed_IsolationMatch_Success() {
        createAndSaveBed("MATCH-ISO-1", "IsoMatchDept", BedType.ICU, IsolationType.AIRBORNE);
        createAndSaveBed("MATCH-NORMAL-1", "IsoMatchDept", BedType.ICU, IsolationType.NONE);

        Bed matched = bedService.findBestMatchBed("IsoMatchDept", BedType.ICU, IsolationType.AIRBORNE);

        assertNotNull(matched, "应能匹配到AIRBORNE隔离床");
        assertEquals(IsolationType.AIRBORNE, matched.getIsolationType(),
                "匹配的床位隔离类型必须是AIRBORNE");
        assertEquals("MATCH-ISO-1", matched.getBedNumber(),
                "应匹配正确的隔离床号");
    }

    @Test
    @DisplayName("隔离床匹配失败 - 科室无对应隔离床必须返回null")
    void testFindBestMatchBed_IsolationMatch_NoBed_ReturnsNull() {
        createAndSaveBed("WRONG-ISO-1", "WrongIsoDept", BedType.ICU, IsolationType.CONTACT);
        createAndSaveBed("WRONG-NORMAL-1", "WrongIsoDept", BedType.ICU, IsolationType.NONE);

        Bed result = bedService.findBestMatchBed("WrongIsoDept", BedType.ICU, IsolationType.AIRBORNE);

        assertNull(result, "无匹配AIRBORNE隔离床时必须返回null，不能分配普通床或错隔离床");
    }

    @Test
    @DisplayName("隔离床匹配失败 - 只有普通床时返回null，禁止退而求其次")
    void testFindBestMatchBed_IsolationRequired_OnlyNormalBeds_ReturnsNull() {
        createAndSaveBed("NORMAL-ONLY-1", "NormalOnlyDept", BedType.GENERAL, IsolationType.NONE);
        createAndSaveBed("NORMAL-ONLY-2", "NormalOnlyDept", BedType.GENERAL, IsolationType.NONE);

        Bed result = bedService.findBestMatchBed("NormalOnlyDept", BedType.GENERAL, IsolationType.CONTACT);

        assertNull(result, "科室只有普通床时，隔离患者必须返回null");
    }

    @Test
    @DisplayName("非隔离患者匹配 - 不分配隔离床位，返回非隔离床")
    void testFindBestMatchBed_NoIsolation_AvoidsIsolationBeds() {
        createAndSaveBed("AVOID-ISO-1", "AvoidDept", BedType.GENERAL, IsolationType.CONTACT);
        createAndSaveBed("AVOID-NORMAL-1", "AvoidDept", BedType.GENERAL, IsolationType.NONE);

        Bed result = bedService.findBestMatchBed("AvoidDept", BedType.GENERAL, IsolationType.NONE);

        assertNotNull(result);
        assertEquals(IsolationType.NONE, result.getIsolationType(),
                "非隔离患者应分配非隔离床");
        assertEquals("AVOID-NORMAL-1", result.getBedNumber());
    }

    @Test
    @DisplayName("隔离匹配成功时优先选择同类型且正确床型的床位")
    void testFindBestMatchBed_IsolationMatch_PrefersCorrectBedType() {
        createAndSaveBed("ISO-GEN", "PrefDept", BedType.GENERAL, IsolationType.AIRBORNE);
        createAndSaveBed("ISO-ICU", "PrefDept", BedType.ICU, IsolationType.AIRBORNE);

        Bed result = bedService.findBestMatchBed("PrefDept", BedType.ICU, IsolationType.AIRBORNE);

        assertNotNull(result);
        assertEquals("ISO-ICU", result.getBedNumber(),
                "应优先选择隔离类型+床型都匹配的床位");
        assertEquals(BedType.ICU, result.getBedType());
        assertEquals(IsolationType.AIRBORNE, result.getIsolationType());
    }

    // ============ 转科申请隔离匹配测试 ============

    @Test
    @DisplayName("隔离患者转科 - 有匹配隔离床：分配成功，状态不是ISOLATION_REQUIRED")
    void testApplyTransfer_Isolation_Success() {
        Patient isoPatient = createAndSavePatient("ISO-SUCCESS-001", "Emergency",
                NursingLevel.LEVEL1, IsolationType.AIRBORNE, PatientStatus.CRITICAL);

        createAndSaveBed("SUCC-ISO-ICU-1", "IsoSuccessDept", BedType.ICU, IsolationType.AIRBORNE);

        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("ISO-SUCCESS-001")
                .targetDepartment("IsoSuccessDept")
                .requiredBedType(BedType.ICU)
                .isolationRequirement(IsolationType.AIRBORNE)
                .nursingLevel(NursingLevel.LEVEL1)
                .diagnosis("TB")
                .reason("Need airborne isolation")
                .applicantDoctorId("DR001")
                .build();

        TransferResponse response = transferService.applyTransfer(request);

        assertNotNull(response.getAssignedBedId(), "匹配成功必须分配床位ID");
        assertNotNull(response.getAssignedBedNumber(), "匹配成功必须分配床位号");
        assertNotEquals(TransferStatus.ISOLATION_REQUIRED, response.getStatus(),
                "隔离匹配成功后不应是ISOLATION_REQUIRED状态");
        assertNotEquals(TransferStatus.NO_BED_AVAILABLE, response.getStatus(),
                "隔离匹配成功后不应是NO_BED_AVAILABLE状态");

        List<TransferApplication> saved = transferRepository
                .findByPatientAdmissionNumber("ISO-SUCCESS-001");
        assertFalse(saved.isEmpty());
        TransferApplication app = saved.get(0);
        assertNotNull(app.getAssignedBedId(), "数据库中必须有分配的床位ID");
        assertEquals("SUCC-ISO-ICU-1", app.getAssignedBedNumber());

        Bed assignedBed = bedRepository.findById(app.getAssignedBedId()).orElse(null);
        assertNotNull(assignedBed);
        assertTrue(assignedBed.getOccupied(), "匹配成功后床位应被占用");
        assertEquals(IsolationType.AIRBORNE, assignedBed.getIsolationType(),
                "占用的床位必须是AIRBORNE隔离类型");
    }

    @Test
    @DisplayName("隔离患者转科 - 无匹配隔离床：保持ISOLATION_REQUIRED，不分配床位，不占床")
    void testApplyTransfer_Isolation_NoMatchingBed_Failure() {
        Patient isoPatient = createAndSavePatient("ISO-FAIL-001", "Respiratory",
                NursingLevel.LEVEL1, IsolationType.CONTACT, PatientStatus.STABLE);

        createAndSaveBed("FAIL-AIRBORNE", "IsoFailDept", BedType.ICU, IsolationType.AIRBORNE);
        createAndSaveBed("FAIL-NORMAL", "IsoFailDept", BedType.GENERAL, IsolationType.NONE);

        long availableBefore = bedRepository.countByDepartmentAndOccupiedFalseAndEnabledTrue("IsoFailDept");

        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("ISO-FAIL-001")
                .targetDepartment("IsoFailDept")
                .requiredBedType(BedType.GENERAL)
                .isolationRequirement(IsolationType.CONTACT)
                .nursingLevel(NursingLevel.LEVEL1)
                .diagnosis("MDR")
                .reason("Need contact isolation")
                .applicantDoctorId("DR001")
                .build();

        TransferResponse response = transferService.applyTransfer(request);

        assertEquals(TransferStatus.ISOLATION_REQUIRED, response.getStatus(),
                "无匹配CONTACT隔离床，状态必须是ISOLATION_REQUIRED，实际: " + response.getStatus());
        assertNull(response.getAssignedBedId(), "隔离匹配失败，assignedBedId必须为null");
        assertNull(response.getAssignedBedNumber(), "隔离匹配失败，assignedBedNumber必须为null");
        assertNotNull(response.getRemark(), "备注必须说明隔离等待情况");
        assertTrue(response.getRemark().contains("隔离") || response.getRemark().contains("等待"),
                "备注应说明隔离床位等待情况");

        long availableAfter = bedRepository.countByDepartmentAndOccupiedFalseAndEnabledTrue("IsoFailDept");
        assertEquals(availableBefore, availableAfter,
                "隔离匹配失败时，科室可用床位数不应变化（不应错占床）");

        List<TransferApplication> saved = transferRepository
                .findByPatientAdmissionNumber("ISO-FAIL-001");
        assertFalse(saved.isEmpty());
        TransferApplication app = saved.get(0);
        assertNull(app.getAssignedBedId(), "数据库中转科申请的床位ID必须为null");
        assertNull(app.getAssignedBedNumber(), "数据库中转科申请的床位号必须为null");
    }

    @Test
    @DisplayName("隔离患者转科 - 有普通床但无隔离床时坚决不分配普通床")
    void testApplyTransfer_Isolation_NeverAssignNormalBed() {
        Patient isoPatient = createAndSavePatient("ISO-NORMAL-001", "InfectiousDept",
                NursingLevel.LEVEL1, IsolationType.DROPLET, PatientStatus.STABLE);

        createAndSaveBed("NORMAL-BED-1", "OnlyNormalDept", BedType.GENERAL, IsolationType.NONE);
        createAndSaveBed("NORMAL-BED-2", "OnlyNormalDept", BedType.GENERAL, IsolationType.NONE);

        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("ISO-NORMAL-001")
                .targetDepartment("OnlyNormalDept")
                .requiredBedType(BedType.GENERAL)
                .isolationRequirement(IsolationType.DROPLET)
                .nursingLevel(NursingLevel.LEVEL1)
                .diagnosis("Influenza")
                .reason("Need droplet isolation")
                .applicantDoctorId("DR001")
                .build();

        TransferResponse response = transferService.applyTransfer(request);

        assertEquals(TransferStatus.ISOLATION_REQUIRED, response.getStatus());
        assertNull(response.getAssignedBedId(), "隔离患者无匹配隔离床时绝对不能分配普通床");
        assertNull(response.getAssignedBedNumber());

        List<Bed> stillAvailable = bedRepository
                .findByDepartmentAndOccupiedFalseAndEnabledTrue("OnlyNormalDept");
        assertEquals(2, stillAvailable.size(),
                "2个普通床都应依然可用，不应被隔离患者错误占用");
    }

    @Test
    @DisplayName("重试机制 - ISOLATION_REQUIRED状态的申请应被重试")
    void testRetryPendingTransfers_IsolationRequiredShouldBeRetried() {
        Patient patient = createAndSavePatient("RETRY-001", "SourceDept",
                NursingLevel.LEVEL1, IsolationType.CONTACT, PatientStatus.STABLE);

        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("RETRY-001")
                .targetDepartment("RetryDept")
                .requiredBedType(BedType.GENERAL)
                .isolationRequirement(IsolationType.CONTACT)
                .nursingLevel(NursingLevel.LEVEL1)
                .diagnosis("Test")
                .reason("Test")
                .applicantDoctorId("DR001")
                .build();

        TransferResponse first = transferService.applyTransfer(request);
        assertEquals(TransferStatus.ISOLATION_REQUIRED, first.getStatus(),
                "初始无隔离床，状态应为ISOLATION_REQUIRED");
        assertNull(first.getAssignedBedId());

        createAndSaveBed("RETRY-ISO-1", "RetryDept", BedType.GENERAL, IsolationType.CONTACT);

        transferService.retryPendingTransfers();

        List<TransferApplication> updated = transferRepository
                .findByPatientAdmissionNumber("RETRY-001");
        assertFalse(updated.isEmpty());
        TransferApplication app = updated.get(0);
        assertNotNull(app.getAssignedBedId(),
                "重试时科室已有匹配隔离床，应分配成功");
        assertEquals("RETRY-ISO-1", app.getAssignedBedNumber());
        assertNotEquals(TransferStatus.ISOLATION_REQUIRED, app.getStatus(),
                "分配成功后状态不再是ISOLATION_REQUIRED");
    }

    // ============ 优先级评估测试 ============

    @Test
    @DisplayName("优先级评估 - 危重+特级护理+ICU+隔离 得高分")
    void testCalculatePriorityScore_HighScore() {
        Patient critical = createAndSavePatient("HIGH-SCORE-001", "ED",
                NursingLevel.SPECIAL, IsolationType.AIRBORNE, PatientStatus.CRITICAL);

        TransferRequest request = TransferRequest.builder()
                .nursingLevel(NursingLevel.SPECIAL)
                .isolationRequirement(IsolationType.AIRBORNE)
                .requiredBedType(BedType.ICU)
                .build();

        int score = transferService.calculatePriorityScore(critical, request);

        assertTrue(score >= 95, "危重(40)+特级(30)+隔离(15)+ICU(20)=105，应≥95，实际: " + score);
    }

    @Test
    @DisplayName("优先级评估 - 稳定+三级护理 得低分")
    void testCalculatePriorityScore_LowScore() {
        Patient stable = createAndSavePatient("LOW-SCORE-001", "Ward",
                NursingLevel.LEVEL3, IsolationType.NONE, PatientStatus.STABLE);

        TransferRequest request = TransferRequest.builder()
                .nursingLevel(NursingLevel.LEVEL3)
                .isolationRequirement(IsolationType.NONE)
                .requiredBedType(BedType.GENERAL)
                .build();

        int score = transferService.calculatePriorityScore(stable, request);

        assertTrue(score <= 25, "稳定(10)+三级(5) = 15，应≤25，实际: " + score);
    }
}
