package com.hospital.transfer.dto;

import com.hospital.transfer.enums.IsolationType;

public class IsolationRestriction {

    private String restrictionType;
    private String message;
    private IsolationType requiredIsolationType;
    private String detail;

    public IsolationRestriction() {
    }

    public IsolationRestriction(String restrictionType, String message,
                                IsolationType requiredIsolationType, String detail) {
        this.restrictionType = restrictionType;
        this.message = message;
        this.requiredIsolationType = requiredIsolationType;
        this.detail = detail;
    }

    public String getRestrictionType() {
        return restrictionType;
    }

    public void setRestrictionType(String restrictionType) {
        this.restrictionType = restrictionType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public IsolationType getRequiredIsolationType() {
        return requiredIsolationType;
    }

    public void setRequiredIsolationType(IsolationType requiredIsolationType) {
        this.requiredIsolationType = requiredIsolationType;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String restrictionType;
        private String message;
        private IsolationType requiredIsolationType;
        private String detail;

        public Builder restrictionType(String restrictionType) {
            this.restrictionType = restrictionType;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder requiredIsolationType(IsolationType requiredIsolationType) {
            this.requiredIsolationType = requiredIsolationType;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public IsolationRestriction build() {
            return new IsolationRestriction(restrictionType, message, requiredIsolationType, detail);
        }
    }
}
