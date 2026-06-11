package com.hospital.transfer.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "family_notifications")
public class FamilyNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transferApplicationId;

    @Column(nullable = false)
    private String patientAdmissionNumber;

    @Column(nullable = false)
    private String familyContact;

    @Column(nullable = false)
    private String notificationContent;

    @Column(nullable = false)
    private Boolean notified = false;

    @Column(nullable = false)
    private Boolean familyAgreed = true;

    private String refusalReason;

    private LocalDateTime notificationTime;

    private LocalDateTime responseTime;

    public FamilyNotification() {
    }

    public FamilyNotification(Long id, Long transferApplicationId, String patientAdmissionNumber,
                              String familyContact, String notificationContent, Boolean notified,
                              Boolean familyAgreed, String refusalReason,
                              LocalDateTime notificationTime, LocalDateTime responseTime) {
        this.id = id;
        this.transferApplicationId = transferApplicationId;
        this.patientAdmissionNumber = patientAdmissionNumber;
        this.familyContact = familyContact;
        this.notificationContent = notificationContent;
        this.notified = notified;
        this.familyAgreed = familyAgreed;
        this.refusalReason = refusalReason;
        this.notificationTime = notificationTime;
        this.responseTime = responseTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTransferApplicationId() { return transferApplicationId; }
    public void setTransferApplicationId(Long transferApplicationId) { this.transferApplicationId = transferApplicationId; }

    public String getPatientAdmissionNumber() { return patientAdmissionNumber; }
    public void setPatientAdmissionNumber(String patientAdmissionNumber) { this.patientAdmissionNumber = patientAdmissionNumber; }

    public String getFamilyContact() { return familyContact; }
    public void setFamilyContact(String familyContact) { this.familyContact = familyContact; }

    public String getNotificationContent() { return notificationContent; }
    public void setNotificationContent(String notificationContent) { this.notificationContent = notificationContent; }

    public Boolean getNotified() { return notified; }
    public void setNotified(Boolean notified) { this.notified = notified; }

    public Boolean getFamilyAgreed() { return familyAgreed; }
    public void setFamilyAgreed(Boolean familyAgreed) { this.familyAgreed = familyAgreed; }

    public String getRefusalReason() { return refusalReason; }
    public void setRefusalReason(String refusalReason) { this.refusalReason = refusalReason; }

    public LocalDateTime getNotificationTime() { return notificationTime; }
    public void setNotificationTime(LocalDateTime notificationTime) { this.notificationTime = notificationTime; }

    public LocalDateTime getResponseTime() { return responseTime; }
    public void setResponseTime(LocalDateTime responseTime) { this.responseTime = responseTime; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long transferApplicationId;
        private String patientAdmissionNumber;
        private String familyContact;
        private String notificationContent;
        private Boolean notified = false;
        private Boolean familyAgreed = true;
        private String refusalReason;
        private LocalDateTime notificationTime;
        private LocalDateTime responseTime;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder transferApplicationId(Long transferApplicationId) { this.transferApplicationId = transferApplicationId; return this; }
        public Builder patientAdmissionNumber(String patientAdmissionNumber) { this.patientAdmissionNumber = patientAdmissionNumber; return this; }
        public Builder familyContact(String familyContact) { this.familyContact = familyContact; return this; }
        public Builder notificationContent(String notificationContent) { this.notificationContent = notificationContent; return this; }
        public Builder notified(Boolean notified) { this.notified = notified; return this; }
        public Builder familyAgreed(Boolean familyAgreed) { this.familyAgreed = familyAgreed; return this; }
        public Builder refusalReason(String refusalReason) { this.refusalReason = refusalReason; return this; }
        public Builder notificationTime(LocalDateTime notificationTime) { this.notificationTime = notificationTime; return this; }
        public Builder responseTime(LocalDateTime responseTime) { this.responseTime = responseTime; return this; }

        public FamilyNotification build() {
            return new FamilyNotification(id, transferApplicationId, patientAdmissionNumber,
                    familyContact, notificationContent, notified, familyAgreed, refusalReason,
                    notificationTime, responseTime);
        }
    }
}
