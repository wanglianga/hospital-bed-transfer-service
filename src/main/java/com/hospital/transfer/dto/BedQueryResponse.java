package com.hospital.transfer.dto;

import com.hospital.transfer.enums.BedType;
import com.hospital.transfer.enums.IsolationType;

public class BedQueryResponse {

    private Long id;
    private String bedNumber;
    private String department;
    private BedType bedType;
    private IsolationType isolationType;
    private Boolean occupied;
    private String occupiedByAdmissionNumber;
    private String roomNumber;
    private String wardNumber;

    public BedQueryResponse() {
    }

    public BedQueryResponse(Long id, String bedNumber, String department, BedType bedType,
                            IsolationType isolationType, Boolean occupied, String occupiedByAdmissionNumber,
                            String roomNumber, String wardNumber) {
        this.id = id;
        this.bedNumber = bedNumber;
        this.department = department;
        this.bedType = bedType;
        this.isolationType = isolationType;
        this.occupied = occupied;
        this.occupiedByAdmissionNumber = occupiedByAdmissionNumber;
        this.roomNumber = roomNumber;
        this.wardNumber = wardNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(String bedNumber) {
        this.bedNumber = bedNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public BedType getBedType() {
        return bedType;
    }

    public void setBedType(BedType bedType) {
        this.bedType = bedType;
    }

    public IsolationType getIsolationType() {
        return isolationType;
    }

    public void setIsolationType(IsolationType isolationType) {
        this.isolationType = isolationType;
    }

    public Boolean getOccupied() {
        return occupied;
    }

    public void setOccupied(Boolean occupied) {
        this.occupied = occupied;
    }

    public String getOccupiedByAdmissionNumber() {
        return occupiedByAdmissionNumber;
    }

    public void setOccupiedByAdmissionNumber(String occupiedByAdmissionNumber) {
        this.occupiedByAdmissionNumber = occupiedByAdmissionNumber;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getWardNumber() {
        return wardNumber;
    }

    public void setWardNumber(String wardNumber) {
        this.wardNumber = wardNumber;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Long id;
        private String bedNumber;
        private String department;
        private BedType bedType;
        private IsolationType isolationType;
        private Boolean occupied;
        private String occupiedByAdmissionNumber;
        private String roomNumber;
        private String wardNumber;

        Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder bedNumber(String bedNumber) {
            this.bedNumber = bedNumber;
            return this;
        }

        public Builder department(String department) {
            this.department = department;
            return this;
        }

        public Builder bedType(BedType bedType) {
            this.bedType = bedType;
            return this;
        }

        public Builder isolationType(IsolationType isolationType) {
            this.isolationType = isolationType;
            return this;
        }

        public Builder occupied(Boolean occupied) {
            this.occupied = occupied;
            return this;
        }

        public Builder occupiedByAdmissionNumber(String occupiedByAdmissionNumber) {
            this.occupiedByAdmissionNumber = occupiedByAdmissionNumber;
            return this;
        }

        public Builder roomNumber(String roomNumber) {
            this.roomNumber = roomNumber;
            return this;
        }

        public Builder wardNumber(String wardNumber) {
            this.wardNumber = wardNumber;
            return this;
        }

        public BedQueryResponse build() {
            return new BedQueryResponse(id, bedNumber, department, bedType, isolationType, occupied,
                    occupiedByAdmissionNumber, roomNumber, wardNumber);
        }
    }
}
