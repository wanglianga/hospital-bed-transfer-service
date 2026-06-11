package com.hospital.transfer.dto;

import com.hospital.transfer.enums.BedType;
import com.hospital.transfer.enums.IsolationType;
import com.hospital.transfer.enums.NursingLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class TransferRequest {

    @NotBlank(message = "患者住院号不能为空")
    private String patientAdmissionNumber;

    @NotBlank(message = "目标科室不能为空")
    private String targetDepartment;

    @NotNull(message = "所需床位类型不能为空")
    private BedType requiredBedType;

    @NotNull(message = "隔离要求不能为空")
    private IsolationType isolationRequirement;

    @NotNull(message = "护理等级不能为空")
    private NursingLevel nursingLevel;

    private String diagnosis;

    private String reason;

    private String applicantDoctorId;

    private LocalDateTime appointmentTime;

    public TransferRequest() {
    }

    public TransferRequest(String patientAdmissionNumber, String targetDepartment, BedType requiredBedType,
                           IsolationType isolationRequirement, NursingLevel nursingLevel, String diagnosis,
                           String reason, String applicantDoctorId, LocalDateTime appointmentTime) {
        this.patientAdmissionNumber = patientAdmissionNumber;
        this.targetDepartment = targetDepartment;
        this.requiredBedType = requiredBedType;
        this.isolationRequirement = isolationRequirement;
        this.nursingLevel = nursingLevel;
        this.diagnosis = diagnosis;
        this.reason = reason;
        this.applicantDoctorId = applicantDoctorId;
        this.appointmentTime = appointmentTime;
    }

    public String getPatientAdmissionNumber() {
        return patientAdmissionNumber;
    }

    public void setPatientAdmissionNumber(String patientAdmissionNumber) {
        this.patientAdmissionNumber = patientAdmissionNumber;
    }

    public String getTargetDepartment() {
        return targetDepartment;
    }

    public void setTargetDepartment(String targetDepartment) {
        this.targetDepartment = targetDepartment;
    }

    public BedType getRequiredBedType() {
        return requiredBedType;
    }

    public void setRequiredBedType(BedType requiredBedType) {
        this.requiredBedType = requiredBedType;
    }

    public IsolationType getIsolationRequirement() {
        return isolationRequirement;
    }

    public void setIsolationRequirement(IsolationType isolationRequirement) {
        this.isolationRequirement = isolationRequirement;
    }

    public NursingLevel getNursingLevel() {
        return nursingLevel;
    }

    public void setNursingLevel(NursingLevel nursingLevel) {
        this.nursingLevel = nursingLevel;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getApplicantDoctorId() {
        return applicantDoctorId;
    }

    public void setApplicantDoctorId(String applicantDoctorId) {
        this.applicantDoctorId = applicantDoctorId;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String patientAdmissionNumber;
        private String targetDepartment;
        private BedType requiredBedType;
        private IsolationType isolationRequirement;
        private NursingLevel nursingLevel;
        private String diagnosis;
        private String reason;
        private String applicantDoctorId;
        private LocalDateTime appointmentTime;

        Builder() {
        }

        public Builder patientAdmissionNumber(String patientAdmissionNumber) {
            this.patientAdmissionNumber = patientAdmissionNumber;
            return this;
        }

        public Builder targetDepartment(String targetDepartment) {
            this.targetDepartment = targetDepartment;
            return this;
        }

        public Builder requiredBedType(BedType requiredBedType) {
            this.requiredBedType = requiredBedType;
            return this;
        }

        public Builder isolationRequirement(IsolationType isolationRequirement) {
            this.isolationRequirement = isolationRequirement;
            return this;
        }

        public Builder nursingLevel(NursingLevel nursingLevel) {
            this.nursingLevel = nursingLevel;
            return this;
        }

        public Builder diagnosis(String diagnosis) {
            this.diagnosis = diagnosis;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder applicantDoctorId(String applicantDoctorId) {
            this.applicantDoctorId = applicantDoctorId;
            return this;
        }

        public Builder appointmentTime(LocalDateTime appointmentTime) {
            this.appointmentTime = appointmentTime;
            return this;
        }

        public TransferRequest build() {
            return new TransferRequest(patientAdmissionNumber, targetDepartment, requiredBedType,
                    isolationRequirement, nursingLevel, diagnosis, reason, applicantDoctorId, appointmentTime);
        }
    }
}
