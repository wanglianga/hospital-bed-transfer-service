package com.hospital.transfer.entity;

import com.hospital.transfer.enums.IsolationType;
import com.hospital.transfer.enums.NursingLevel;
import com.hospital.transfer.enums.PatientStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String admissionNumber;

    @Column(nullable = false)
    private String name;

    private String diagnosis;

    @Column(nullable = false)
    private String currentDepartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NursingLevel nursingLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IsolationType isolationRequirement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatientStatus status = PatientStatus.STABLE;

    private LocalDateTime admissionTime;

    private LocalDateTime updateTime;

    public Patient() {
    }

    public Patient(Long id, String admissionNumber, String name, String diagnosis,
                   String currentDepartment, NursingLevel nursingLevel,
                   IsolationType isolationRequirement, PatientStatus status,
                   LocalDateTime admissionTime, LocalDateTime updateTime) {
        this.id = id;
        this.admissionNumber = admissionNumber;
        this.name = name;
        this.diagnosis = diagnosis;
        this.currentDepartment = currentDepartment;
        this.nursingLevel = nursingLevel;
        this.isolationRequirement = isolationRequirement;
        this.status = status;
        this.admissionTime = admissionTime;
        this.updateTime = updateTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAdmissionNumber() { return admissionNumber; }
    public void setAdmissionNumber(String admissionNumber) { this.admissionNumber = admissionNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getCurrentDepartment() { return currentDepartment; }
    public void setCurrentDepartment(String currentDepartment) { this.currentDepartment = currentDepartment; }

    public NursingLevel getNursingLevel() { return nursingLevel; }
    public void setNursingLevel(NursingLevel nursingLevel) { this.nursingLevel = nursingLevel; }

    public IsolationType getIsolationRequirement() { return isolationRequirement; }
    public void setIsolationRequirement(IsolationType isolationRequirement) { this.isolationRequirement = isolationRequirement; }

    public PatientStatus getStatus() { return status; }
    public void setStatus(PatientStatus status) { this.status = status; }

    public LocalDateTime getAdmissionTime() { return admissionTime; }
    public void setAdmissionTime(LocalDateTime admissionTime) { this.admissionTime = admissionTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String admissionNumber;
        private String name;
        private String diagnosis;
        private String currentDepartment;
        private NursingLevel nursingLevel;
        private IsolationType isolationRequirement;
        private PatientStatus status = PatientStatus.STABLE;
        private LocalDateTime admissionTime;
        private LocalDateTime updateTime;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder admissionNumber(String admissionNumber) { this.admissionNumber = admissionNumber; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder diagnosis(String diagnosis) { this.diagnosis = diagnosis; return this; }
        public Builder currentDepartment(String currentDepartment) { this.currentDepartment = currentDepartment; return this; }
        public Builder nursingLevel(NursingLevel nursingLevel) { this.nursingLevel = nursingLevel; return this; }
        public Builder isolationRequirement(IsolationType isolationRequirement) { this.isolationRequirement = isolationRequirement; return this; }
        public Builder status(PatientStatus status) { this.status = status; return this; }
        public Builder admissionTime(LocalDateTime admissionTime) { this.admissionTime = admissionTime; return this; }
        public Builder updateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }

        public Patient build() {
            return new Patient(id, admissionNumber, name, diagnosis, currentDepartment,
                    nursingLevel, isolationRequirement, status, admissionTime, updateTime);
        }
    }
}
