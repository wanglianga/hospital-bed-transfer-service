package com.hospital.transfer.entity;

import com.hospital.transfer.enums.BedType;
import com.hospital.transfer.enums.IsolationType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "beds")
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bedNumber;

    @Column(nullable = false)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedType bedType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IsolationType isolationType = IsolationType.NONE;

    @Column(nullable = false)
    private Boolean occupied = false;

    private String occupiedByAdmissionNumber;

    private Long currentTransferApplicationId;

    private LocalDateTime occupiedTime;

    @Column(nullable = false)
    private Boolean enabled = true;

    private String roomNumber;

    private String wardNumber;

    private LocalDateTime updateTime;

    public Bed() {
    }

    public Bed(Long id, String bedNumber, String department, BedType bedType,
               IsolationType isolationType, Boolean occupied, String occupiedByAdmissionNumber,
               Long currentTransferApplicationId, LocalDateTime occupiedTime,
               Boolean enabled, String roomNumber, String wardNumber, LocalDateTime updateTime) {
        this.id = id;
        this.bedNumber = bedNumber;
        this.department = department;
        this.bedType = bedType;
        this.isolationType = isolationType;
        this.occupied = occupied;
        this.occupiedByAdmissionNumber = occupiedByAdmissionNumber;
        this.currentTransferApplicationId = currentTransferApplicationId;
        this.occupiedTime = occupiedTime;
        this.enabled = enabled;
        this.roomNumber = roomNumber;
        this.wardNumber = wardNumber;
        this.updateTime = updateTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public BedType getBedType() { return bedType; }
    public void setBedType(BedType bedType) { this.bedType = bedType; }

    public IsolationType getIsolationType() { return isolationType; }
    public void setIsolationType(IsolationType isolationType) { this.isolationType = isolationType; }

    public Boolean getOccupied() { return occupied; }
    public void setOccupied(Boolean occupied) { this.occupied = occupied; }

    public String getOccupiedByAdmissionNumber() { return occupiedByAdmissionNumber; }
    public void setOccupiedByAdmissionNumber(String occupiedByAdmissionNumber) { this.occupiedByAdmissionNumber = occupiedByAdmissionNumber; }

    public Long getCurrentTransferApplicationId() { return currentTransferApplicationId; }
    public void setCurrentTransferApplicationId(Long currentTransferApplicationId) { this.currentTransferApplicationId = currentTransferApplicationId; }

    public LocalDateTime getOccupiedTime() { return occupiedTime; }
    public void setOccupiedTime(LocalDateTime occupiedTime) { this.occupiedTime = occupiedTime; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getWardNumber() { return wardNumber; }
    public void setWardNumber(String wardNumber) { this.wardNumber = wardNumber; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    public boolean isAvailable() {
        return enabled && !occupied;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String bedNumber;
        private String department;
        private BedType bedType;
        private IsolationType isolationType = IsolationType.NONE;
        private Boolean occupied = false;
        private String occupiedByAdmissionNumber;
        private Long currentTransferApplicationId;
        private LocalDateTime occupiedTime;
        private Boolean enabled = true;
        private String roomNumber;
        private String wardNumber;
        private LocalDateTime updateTime;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder bedNumber(String bedNumber) { this.bedNumber = bedNumber; return this; }
        public Builder department(String department) { this.department = department; return this; }
        public Builder bedType(BedType bedType) { this.bedType = bedType; return this; }
        public Builder isolationType(IsolationType isolationType) { this.isolationType = isolationType; return this; }
        public Builder occupied(Boolean occupied) { this.occupied = occupied; return this; }
        public Builder occupiedByAdmissionNumber(String occupiedByAdmissionNumber) { this.occupiedByAdmissionNumber = occupiedByAdmissionNumber; return this; }
        public Builder currentTransferApplicationId(Long currentTransferApplicationId) { this.currentTransferApplicationId = currentTransferApplicationId; return this; }
        public Builder occupiedTime(LocalDateTime occupiedTime) { this.occupiedTime = occupiedTime; return this; }
        public Builder enabled(Boolean enabled) { this.enabled = enabled; return this; }
        public Builder roomNumber(String roomNumber) { this.roomNumber = roomNumber; return this; }
        public Builder wardNumber(String wardNumber) { this.wardNumber = wardNumber; return this; }
        public Builder updateTime(LocalDateTime updateTime) { this.updateTime = updateTime; return this; }

        public Bed build() {
            return new Bed(id, bedNumber, department, bedType, isolationType, occupied,
                    occupiedByAdmissionNumber, currentTransferApplicationId, occupiedTime,
                    enabled, roomNumber, wardNumber, updateTime);
        }
    }
}
