package com.hospital.transfer.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nurse_station_confirmations")
public class NurseStationConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transferApplicationId;

    @Column(nullable = false)
    private String targetDepartment;

    @Column(nullable = false)
    private String nurseId;

    @Column(nullable = false)
    private String nurseName;

    @Column(nullable = false)
    private LocalDateTime confirmTime;

    @Column(nullable = false)
    private Boolean accepted = true;

    private String rejectReason;

    private LocalDateTime estimatedArrivalTime;

    private String remark;

    public NurseStationConfirmation() {
    }

    public NurseStationConfirmation(Long id, Long transferApplicationId, String targetDepartment,
                                    String nurseId, String nurseName, LocalDateTime confirmTime,
                                    Boolean accepted, String rejectReason,
                                    LocalDateTime estimatedArrivalTime, String remark) {
        this.id = id;
        this.transferApplicationId = transferApplicationId;
        this.targetDepartment = targetDepartment;
        this.nurseId = nurseId;
        this.nurseName = nurseName;
        this.confirmTime = confirmTime;
        this.accepted = accepted;
        this.rejectReason = rejectReason;
        this.estimatedArrivalTime = estimatedArrivalTime;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTransferApplicationId() { return transferApplicationId; }
    public void setTransferApplicationId(Long transferApplicationId) { this.transferApplicationId = transferApplicationId; }

    public String getTargetDepartment() { return targetDepartment; }
    public void setTargetDepartment(String targetDepartment) { this.targetDepartment = targetDepartment; }

    public String getNurseId() { return nurseId; }
    public void setNurseId(String nurseId) { this.nurseId = nurseId; }

    public String getNurseName() { return nurseName; }
    public void setNurseName(String nurseName) { this.nurseName = nurseName; }

    public LocalDateTime getConfirmTime() { return confirmTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getEstimatedArrivalTime() { return estimatedArrivalTime; }
    public void setEstimatedArrivalTime(LocalDateTime estimatedArrivalTime) { this.estimatedArrivalTime = estimatedArrivalTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long transferApplicationId;
        private String targetDepartment;
        private String nurseId;
        private String nurseName;
        private LocalDateTime confirmTime;
        private Boolean accepted = true;
        private String rejectReason;
        private LocalDateTime estimatedArrivalTime;
        private String remark;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder transferApplicationId(Long transferApplicationId) { this.transferApplicationId = transferApplicationId; return this; }
        public Builder targetDepartment(String targetDepartment) { this.targetDepartment = targetDepartment; return this; }
        public Builder nurseId(String nurseId) { this.nurseId = nurseId; return this; }
        public Builder nurseName(String nurseName) { this.nurseName = nurseName; return this; }
        public Builder confirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; return this; }
        public Builder accepted(Boolean accepted) { this.accepted = accepted; return this; }
        public Builder rejectReason(String rejectReason) { this.rejectReason = rejectReason; return this; }
        public Builder estimatedArrivalTime(LocalDateTime estimatedArrivalTime) { this.estimatedArrivalTime = estimatedArrivalTime; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }

        public NurseStationConfirmation build() {
            return new NurseStationConfirmation(id, transferApplicationId, targetDepartment,
                    nurseId, nurseName, confirmTime, accepted, rejectReason, estimatedArrivalTime, remark);
        }
    }
}
