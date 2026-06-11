package com.hospital.transfer.entity;

import com.hospital.transfer.enums.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_applications")
public class TransferApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String patientAdmissionNumber;

    @Column(nullable = false)
    private String currentDepartment;

    @Column(nullable = false)
    private String targetDepartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedType requiredBedType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IsolationType isolationRequirement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NursingLevel nursingLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityLevel priorityLevel = PriorityLevel.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    private String diagnosis;

    private String reason;

    private String applicantDoctorId;

    private LocalDateTime applicationTime;

    private Long assignedBedId;

    private String assignedBedNumber;

    private LocalDateTime assignedTime;

    private LocalDateTime appointmentTime;

    private LocalDateTime occupationDeadline;

    private LocalDateTime admissionConfirmedTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private Integer priorityScore;

    private String remark;

    public TransferApplication() {
    }

    public TransferApplication(Long id, String patientAdmissionNumber, String currentDepartment,
                               String targetDepartment, BedType requiredBedType,
                               IsolationType isolationRequirement, NursingLevel nursingLevel,
                               PriorityLevel priorityLevel, TransferStatus status, String diagnosis,
                               String reason, String applicantDoctorId, LocalDateTime applicationTime,
                               Long assignedBedId, String assignedBedNumber, LocalDateTime assignedTime,
                               LocalDateTime appointmentTime, LocalDateTime occupationDeadline,
                               LocalDateTime admissionConfirmedTime, LocalDateTime cancelTime,
                               String cancelReason, Integer priorityScore, String remark) {
        this.id = id;
        this.patientAdmissionNumber = patientAdmissionNumber;
        this.currentDepartment = currentDepartment;
        this.targetDepartment = targetDepartment;
        this.requiredBedType = requiredBedType;
        this.isolationRequirement = isolationRequirement;
        this.nursingLevel = nursingLevel;
        this.priorityLevel = priorityLevel;
        this.status = status;
        this.diagnosis = diagnosis;
        this.reason = reason;
        this.applicantDoctorId = applicantDoctorId;
        this.applicationTime = applicationTime;
        this.assignedBedId = assignedBedId;
        this.assignedBedNumber = assignedBedNumber;
        this.assignedTime = assignedTime;
        this.appointmentTime = appointmentTime;
        this.occupationDeadline = occupationDeadline;
        this.admissionConfirmedTime = admissionConfirmedTime;
        this.cancelTime = cancelTime;
        this.cancelReason = cancelReason;
        this.priorityScore = priorityScore;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPatientAdmissionNumber() { return patientAdmissionNumber; }
    public void setPatientAdmissionNumber(String patientAdmissionNumber) { this.patientAdmissionNumber = patientAdmissionNumber; }

    public String getCurrentDepartment() { return currentDepartment; }
    public void setCurrentDepartment(String currentDepartment) { this.currentDepartment = currentDepartment; }

    public String getTargetDepartment() { return targetDepartment; }
    public void setTargetDepartment(String targetDepartment) { this.targetDepartment = targetDepartment; }

    public BedType getRequiredBedType() { return requiredBedType; }
    public void setRequiredBedType(BedType requiredBedType) { this.requiredBedType = requiredBedType; }

    public IsolationType getIsolationRequirement() { return isolationRequirement; }
    public void setIsolationRequirement(IsolationType isolationRequirement) { this.isolationRequirement = isolationRequirement; }

    public NursingLevel getNursingLevel() { return nursingLevel; }
    public void setNursingLevel(NursingLevel nursingLevel) { this.nursingLevel = nursingLevel; }

    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }

    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getApplicantDoctorId() { return applicantDoctorId; }
    public void setApplicantDoctorId(String applicantDoctorId) { this.applicantDoctorId = applicantDoctorId; }

    public LocalDateTime getApplicationTime() { return applicationTime; }
    public void setApplicationTime(LocalDateTime applicationTime) { this.applicationTime = applicationTime; }

    public Long getAssignedBedId() { return assignedBedId; }
    public void setAssignedBedId(Long assignedBedId) { this.assignedBedId = assignedBedId; }

    public String getAssignedBedNumber() { return assignedBedNumber; }
    public void setAssignedBedNumber(String assignedBedNumber) { this.assignedBedNumber = assignedBedNumber; }

    public LocalDateTime getAssignedTime() { return assignedTime; }
    public void setAssignedTime(LocalDateTime assignedTime) { this.assignedTime = assignedTime; }

    public LocalDateTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; }

    public LocalDateTime getOccupationDeadline() { return occupationDeadline; }
    public void setOccupationDeadline(LocalDateTime occupationDeadline) { this.occupationDeadline = occupationDeadline; }

    public LocalDateTime getAdmissionConfirmedTime() { return admissionConfirmedTime; }
    public void setAdmissionConfirmedTime(LocalDateTime admissionConfirmedTime) { this.admissionConfirmedTime = admissionConfirmedTime; }

    public LocalDateTime getCancelTime() { return cancelTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public Integer getPriorityScore() { return priorityScore; }
    public void setPriorityScore(Integer priorityScore) { this.priorityScore = priorityScore; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @PrePersist
    protected void onCreate() {
        if (applicationTime == null) {
            applicationTime = LocalDateTime.now();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String patientAdmissionNumber;
        private String currentDepartment;
        private String targetDepartment;
        private BedType requiredBedType;
        private IsolationType isolationRequirement;
        private NursingLevel nursingLevel;
        private PriorityLevel priorityLevel = PriorityLevel.NORMAL;
        private TransferStatus status = TransferStatus.PENDING;
        private String diagnosis;
        private String reason;
        private String applicantDoctorId;
        private LocalDateTime applicationTime;
        private Long assignedBedId;
        private String assignedBedNumber;
        private LocalDateTime assignedTime;
        private LocalDateTime appointmentTime;
        private LocalDateTime occupationDeadline;
        private LocalDateTime admissionConfirmedTime;
        private LocalDateTime cancelTime;
        private String cancelReason;
        private Integer priorityScore;
        private String remark;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder patientAdmissionNumber(String patientAdmissionNumber) { this.patientAdmissionNumber = patientAdmissionNumber; return this; }
        public Builder currentDepartment(String currentDepartment) { this.currentDepartment = currentDepartment; return this; }
        public Builder targetDepartment(String targetDepartment) { this.targetDepartment = targetDepartment; return this; }
        public Builder requiredBedType(BedType requiredBedType) { this.requiredBedType = requiredBedType; return this; }
        public Builder isolationRequirement(IsolationType isolationRequirement) { this.isolationRequirement = isolationRequirement; return this; }
        public Builder nursingLevel(NursingLevel nursingLevel) { this.nursingLevel = nursingLevel; return this; }
        public Builder priorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; return this; }
        public Builder status(TransferStatus status) { this.status = status; return this; }
        public Builder diagnosis(String diagnosis) { this.diagnosis = diagnosis; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder applicantDoctorId(String applicantDoctorId) { this.applicantDoctorId = applicantDoctorId; return this; }
        public Builder applicationTime(LocalDateTime applicationTime) { this.applicationTime = applicationTime; return this; }
        public Builder assignedBedId(Long assignedBedId) { this.assignedBedId = assignedBedId; return this; }
        public Builder assignedBedNumber(String assignedBedNumber) { this.assignedBedNumber = assignedBedNumber; return this; }
        public Builder assignedTime(LocalDateTime assignedTime) { this.assignedTime = assignedTime; return this; }
        public Builder appointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; return this; }
        public Builder occupationDeadline(LocalDateTime occupationDeadline) { this.occupationDeadline = occupationDeadline; return this; }
        public Builder admissionConfirmedTime(LocalDateTime admissionConfirmedTime) { this.admissionConfirmedTime = admissionConfirmedTime; return this; }
        public Builder cancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; return this; }
        public Builder cancelReason(String cancelReason) { this.cancelReason = cancelReason; return this; }
        public Builder priorityScore(Integer priorityScore) { this.priorityScore = priorityScore; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }

        public TransferApplication build() {
            return new TransferApplication(id, patientAdmissionNumber, currentDepartment, targetDepartment,
                    requiredBedType, isolationRequirement, nursingLevel, priorityLevel, status, diagnosis,
                    reason, applicantDoctorId, applicationTime, assignedBedId, assignedBedNumber,
                    assignedTime, appointmentTime, occupationDeadline, admissionConfirmedTime,
                    cancelTime, cancelReason, priorityScore, remark);
        }
    }
}
