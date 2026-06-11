package com.hospital.transfer.controller;

import com.hospital.transfer.dto.TransferRequest;
import com.hospital.transfer.dto.TransferResponse;
import com.hospital.transfer.entity.Bed;
import com.hospital.transfer.entity.Patient;
import com.hospital.transfer.enums.*;
import com.hospital.transfer.repository.BedRepository;
import com.hospital.transfer.repository.PatientRepository;
import com.hospital.transfer.repository.TransferApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TransferApplicationRepository transferRepository;

    @Autowired
    private BedRepository bedRepository;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        patientRepository.deleteAll();

        Patient p1 = new Patient();
        p1.setAdmissionNumber("API-P001");
        p1.setName("Patient One");
        p1.setDiagnosis("Fever");
        p1.setCurrentDepartment("Emergency");
        p1.setNursingLevel(NursingLevel.LEVEL2);
        p1.setIsolationRequirement(IsolationType.NONE);
        p1.setStatus(PatientStatus.STABLE);
        p1.setAdmissionTime(LocalDateTime.now());
        patientRepository.save(p1);

        Patient p2 = new Patient();
        p2.setAdmissionNumber("API-P002");
        p2.setName("Patient Two");
        p2.setDiagnosis("TB");
        p2.setCurrentDepartment("Respiratory");
        p2.setNursingLevel(NursingLevel.LEVEL1);
        p2.setIsolationRequirement(IsolationType.AIRBORNE);
        p2.setStatus(PatientStatus.CRITICAL);
        p2.setAdmissionTime(LocalDateTime.now());
        patientRepository.save(p2);

        Patient p3 = new Patient();
        p3.setAdmissionNumber("API-P003");
        p3.setName("Patient Three");
        p3.setDiagnosis("MDR Infection");
        p3.setCurrentDepartment("InfectiousDisease");
        p3.setNursingLevel(NursingLevel.SPECIAL);
        p3.setIsolationRequirement(IsolationType.CONTACT);
        p3.setStatus(PatientStatus.CRITICAL);
        p3.setAdmissionTime(LocalDateTime.now());
        patientRepository.save(p3);
    }

    @SuppressWarnings("unchecked")
    private <T> T parseResponseData(MvcResult result, Class<T> clazz) throws Exception {
        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Object data = response.get("data");
        if (clazz == Integer.class || clazz == Long.class) {
            Number n = (Number) data;
            if (clazz == Long.class) {
                Long v = Long.valueOf(n.longValue());
                return (T) v;
            } else {
                Integer v = Integer.valueOf(n.intValue());
                return (T) v;
            }
        }
        if (data instanceof String) return clazz.cast(data);
        return objectMapper.convertValue(data, clazz);
    }

    @SuppressWarnings("unchecked")
    private Long parseLong(MvcResult result, String key) throws Exception {
        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) return null;
        Object val = data.get(key);
        if (val == null) return null;
        return val instanceof Number ? ((Number) val).longValue() : Long.parseLong(val.toString());
    }

    @SuppressWarnings("unchecked")
    private String parseString(MvcResult result, String key) throws Exception {
        String content = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(content, Map.class);
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) return null;
        Object val = data.get(key);
        return val == null ? null : val.toString();
    }

    // ============== 床位余量统计接口测试 ==============

    @Test
    @DisplayName("全院余量统计 - 不传任何参数返回全院总数")
    void testCountAvailable_HospitalWide_NoParams() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/beds/count/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();

        Long count = parseResponseData(result, Long.class);
        assertNotNull(count);
        assertTrue(count >= 18, "全院至少18个初始可用床位，实际: " + count);
        System.out.println("[全院统计] 全院可用床位总数: " + count);
    }

    @Test
    @DisplayName("按科室统计 - ICU科室返回3个可用床位")
    void testCountAvailable_ByDepartment_ICU() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/beds/count/available")
                        .param("department", "ICU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long count = parseResponseData(result, Long.class);
        assertEquals(3L, count, "ICU科室应有3个可用床位");
        System.out.println("[科室统计] ICU可用床位: " + count);
    }

    @Test
    @DisplayName("按科室+床型统计 - ICU+ICU类型返回3个")
    void testCountAvailable_ByDeptAndType() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/beds/count/available")
                        .param("department", "ICU")
                        .param("bedType", "ICU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long count = parseResponseData(result, Long.class);
        assertEquals(3L, count, "ICU科室ICU型应有3个");
        System.out.println("[科室+床型统计] ICU+ICU: " + count);
    }

    @Test
    @DisplayName("按科室+隔离类型统计 - ICU+AIRBORNE返回1个")
    void testCountAvailable_ByDeptAndIsolation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/beds/count/available")
                        .param("department", "ICU")
                        .param("isolationType", "AIRBORNE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long count = parseResponseData(result, Long.class);
        assertEquals(1L, count, "ICU科室AIRBORNE隔离型应有1个");
        System.out.println("[科室+隔离统计] ICU+AIRBORNE: " + count);
    }

    @Test
    @DisplayName("仅按床型全院统计 - 不传department不返回500")
    void testCountAvailable_OnlyByBedType_No500() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/beds/count/available")
                        .param("bedType", "ICU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();

        Long count = parseResponseData(result, Long.class);
        assertNotNull(count);
        assertTrue(count >= 3, "全院ICU床型至少3个，实际: " + count);
        System.out.println("[仅床型全院统计] 全院ICU床型: " + count);
    }

    @Test
    @DisplayName("仅按隔离类型全院统计 - 不传department不返回500")
    void testCountAvailable_OnlyByIsolation_No500() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/beds/count/available")
                        .param("isolationType", "AIRBORNE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();

        Long count = parseResponseData(result, Long.class);
        assertNotNull(count);
        assertTrue(count >= 3, "全院AIRBORNE隔离床至少3个(ICU1+呼吸科1+感染科1)，实际: " + count);
        System.out.println("[仅隔离全院统计] 全院AIRBORNE隔离床: " + count);
    }

    @Test
    @DisplayName("不存在的科室返回0 - 不报错不返回500")
    void testCountAvailable_NonExistentDept_ReturnsZero() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/beds/count/available")
                        .param("department", "NonExistentDepartment123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Long count = parseResponseData(result, Long.class);
        assertEquals(0L, count, "不存在的科室应返回0");
        System.out.println("[不存在科室统计] NonExistentDepartment123: " + count);
    }

    @Test
    @DisplayName("不传department不返回500 - 关键修复验证(1/2)")
    void testCountAvailable_NoDepartment_No500() throws Exception {
        mockMvc.perform(get("/api/beds/count/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
        System.out.println("[500检查] 不传department返回200 OK");
    }

    @Test
    @DisplayName("仅传bedType不返回500 - 关键修复验证(2/2)")
    void testCountAvailable_OnlyBedType_No500() throws Exception {
        mockMvc.perform(get("/api/beds/count/available")
                        .param("bedType", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        System.out.println("[500检查] 仅传bedType返回200 OK");
    }

    // ============== 隔离床匹配接口测试 ==============

    @Test
    @DisplayName("隔离床匹配成功 - AIRBORNE隔离患者转ICU，分到AIRBORNE隔离床ICU-003")
    void testTransferApply_IsolationMatch_Success() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("API-P002")
                .targetDepartment("ICU")
                .requiredBedType(BedType.ICU)
                .isolationRequirement(IsolationType.AIRBORNE)
                .nursingLevel(NursingLevel.LEVEL1)
                .diagnosis("TB")
                .reason("Need airborne isolation")
                .applicantDoctorId("DR-API-1")
                .build();

        MvcResult result = mockMvc.perform(post("/api/transfers/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String status = parseString(result, "status");
        String bedNumber = parseString(result, "assignedBedNumber");

        System.out.println("[隔离匹配成功] 状态: " + status + ", 床位: " + bedNumber);

        assertNotNull(bedNumber, "匹配成功必须分配床位号");
        assertEquals("ICU-003", bedNumber, "ICU中唯一AIRBORNE隔离床是ICU-003");
        assertNotEquals("ISOLATION_REQUIRED", status, "匹配成功后不应是ISOLATION_REQUIRED");
        assertNotEquals("NO_BED_AVAILABLE", status, "匹配成功后不应是NO_BED_AVAILABLE");

        List<Bed> icuAirborneBeds = bedRepository
                .findByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("ICU", IsolationType.AIRBORNE);
        assertEquals(0, icuAirborneBeds.size(),
                "ICU的AIRBORNE隔离床应被占用1个，剩余0");
    }

    @Test
    @DisplayName("隔离床匹配失败 - CONTACT隔离患者转ICU，ICU无CONTACT床，保持ISOLATION_REQUIRED，不分床")
    void testTransferApply_IsolationNoMatch_RemainIsolationRequired() throws Exception {
        long icuTotalBefore = bedRepository.countByDepartmentAndOccupiedFalseAndEnabledTrue("ICU");

        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("API-P003")
                .targetDepartment("ICU")
                .requiredBedType(BedType.ICU)
                .isolationRequirement(IsolationType.CONTACT)
                .nursingLevel(NursingLevel.SPECIAL)
                .diagnosis("MDR")
                .reason("Need contact isolation")
                .applicantDoctorId("DR-API-2")
                .build();

        MvcResult result = mockMvc.perform(post("/api/transfers/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String status = parseString(result, "status");
        Long bedId = parseLong(result, "assignedBedId");
        String bedNumber = parseString(result, "assignedBedNumber");
        String remark = parseString(result, "remark");

        System.out.println("[隔离匹配失败] 状态: " + status + ", 床位ID: " + bedId
                + ", 床位号: " + bedNumber + ", 备注: " + remark);

        assertEquals("ISOLATION_REQUIRED", status,
                "无匹配CONTACT隔离床，状态必须是ISOLATION_REQUIRED");
        assertNull(bedId, "隔离匹配失败时assignedBedId必须为null");
        assertNull(bedNumber, "隔离匹配失败时assignedBedNumber必须为null");
        assertNotNull(remark, "必须说明隔离等待原因");
        assertTrue(remark.contains("隔离") || remark.contains("等待"),
                "Remark应说明隔离情况: " + remark);

        long icuTotalAfter = bedRepository.countByDepartmentAndOccupiedFalseAndEnabledTrue("ICU");
        assertEquals(icuTotalBefore, icuTotalAfter,
                "ICU的可用床位总数不应减少（不能错占普通床）");

        Bed icu003 = bedRepository.findByBedNumber("ICU-003").orElse(null);
        assertNotNull(icu003);
        assertFalse(icu003.getOccupied(),
                "ICU-003(AIRBORNE)不应被CONTACT隔离患者占用");
        Bed icu001 = bedRepository.findByBedNumber("ICU-001").orElse(null);
        assertNotNull(icu001);
        assertFalse(icu001.getOccupied(),
                "ICU-001(普通)不应被CONTACT隔离患者占用");
    }

    @Test
    @DisplayName("隔离床匹配失败 - 坚决不允许分配错隔离类型床位")
    void testTransferApply_Isolation_NoWrongBedOccupied() throws Exception {
        long isoContactBefore = bedRepository
                .countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("InfectiousDisease", IsolationType.CONTACT);
        long isoAirborneBefore = bedRepository
                .countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("InfectiousDisease", IsolationType.AIRBORNE);
        long isoDropletBefore = bedRepository
                .countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("InfectiousDisease", IsolationType.DROPLET);

        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("API-P003")
                .targetDepartment("ICU")
                .requiredBedType(BedType.ICU)
                .isolationRequirement(IsolationType.CONTACT)
                .nursingLevel(NursingLevel.SPECIAL)
                .diagnosis("Test")
                .reason("Test")
                .applicantDoctorId("DR-TEST")
                .build();

        mockMvc.perform(post("/api/transfers/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        long isoContactAfter = bedRepository
                .countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("InfectiousDisease", IsolationType.CONTACT);
        long isoAirborneAfter = bedRepository
                .countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("InfectiousDisease", IsolationType.AIRBORNE);
        long isoDropletAfter = bedRepository
                .countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("InfectiousDisease", IsolationType.DROPLET);

        assertEquals(isoContactBefore, isoContactAfter, "感染科CONTACT隔离床数量不应变化");
        assertEquals(isoAirborneBefore, isoAirborneAfter, "感染科AIRBORNE隔离床数量不应变化");
        assertEquals(isoDropletBefore, isoDropletAfter, "感染科DROPLET隔离床数量不应变化");
        System.out.println("[错隔离验证] 感染科各隔离类型床数量未变化");
    }

    @Test
    @DisplayName("非隔离患者 - 正常分配普通床位，不占隔离床")
    void testTransferApply_NormalPatient_AssignGeneralNotIsolation() throws Exception {
        long genSurgContactBefore = bedRepository
                .countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("GeneralSurgery", IsolationType.CONTACT);

        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("API-P001")
                .targetDepartment("GeneralSurgery")
                .requiredBedType(BedType.GENERAL)
                .isolationRequirement(IsolationType.NONE)
                .nursingLevel(NursingLevel.LEVEL2)
                .diagnosis("Fever")
                .reason("Routine")
                .applicantDoctorId("DR-API-3")
                .build();

        MvcResult result = mockMvc.perform(post("/api/transfers/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String status = parseString(result, "status");
        String bedNumber = parseString(result, "assignedBedNumber");

        System.out.println("[非隔离患者] 状态: " + status + ", 床位: " + bedNumber);

        assertEquals("BED_ASSIGNED", status);
        assertNotNull(bedNumber);
        assertTrue(bedNumber.startsWith("GEN-SURG"), "应分配普外科床位");
        assertNotEquals("GEN-SURG-003", bedNumber, "不能分配CONTACT隔离床(GEN-SURG-003)");

        long genSurgContactAfter = bedRepository
                .countByDepartmentAndIsolationTypeAndOccupiedFalseAndEnabledTrue("GeneralSurgery", IsolationType.CONTACT);
        assertEquals(genSurgContactBefore, genSurgContactAfter,
                "非隔离患者不应占用CONTACT隔离床");
    }

    @Test
    @DisplayName("重试接口 - 隔离失败后新增隔离床，重试分配成功")
    void testRetryPendingTransfers_IsolationRetrySuccess() throws Exception {
        TransferRequest request = TransferRequest.builder()
                .patientAdmissionNumber("API-P003")
                .targetDepartment("ICU")
                .requiredBedType(BedType.ICU)
                .isolationRequirement(IsolationType.CONTACT)
                .nursingLevel(NursingLevel.SPECIAL)
                .diagnosis("MDR")
                .reason("Need contact isolation")
                .applicantDoctorId("DR-RETRY-1")
                .build();

        MvcResult first = mockMvc.perform(post("/api/transfers/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String firstStatus = parseString(first, "status");
        assertEquals("ISOLATION_REQUIRED", firstStatus,
                "初始无CONTACT隔离床，应为ISOLATION_REQUIRED");
        System.out.println("[重试测试] 初始状态: " + firstStatus);

        Bed newIsoBed = new Bed();
        newIsoBed.setBedNumber("ICU-RETRY-ISO-001");
        newIsoBed.setDepartment("ICU");
        newIsoBed.setBedType(BedType.ICU);
        newIsoBed.setIsolationType(IsolationType.CONTACT);
        newIsoBed.setOccupied(false);
        newIsoBed.setEnabled(true);
        bedRepository.save(newIsoBed);

        mockMvc.perform(post("/api/transfers/retry-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        List<TransferResponse> list = transferRepository
                .findByPatientAdmissionNumber("API-P003").stream()
                .map(app -> TransferResponse.builder()
                        .id(app.getId())
                        .status(app.getStatus())
                        .assignedBedNumber(app.getAssignedBedNumber())
                        .assignedBedId(app.getAssignedBedId())
                        .build())
                .toList();

        assertFalse(list.isEmpty());
        TransferResponse latest = list.get(0);
        System.out.println("[重试测试] 重试后状态: " + latest.getStatus()
                + ", 床位: " + latest.getAssignedBedNumber());
        assertNotEquals(TransferStatus.ISOLATION_REQUIRED, latest.getStatus(),
                "重试后应成功分配，不再是ISOLATION_REQUIRED");
        assertNotNull(latest.getAssignedBedNumber(), "重试后应分配到床位号");
        assertEquals("ICU-RETRY-ISO-001", latest.getAssignedBedNumber(),
                "应分配到新加入的CONTACT隔离床");
    }
}
