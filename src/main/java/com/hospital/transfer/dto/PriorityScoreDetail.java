package com.hospital.transfer.dto;

public class PriorityScoreDetail {

    private String factor;
    private int score;
    private String description;

    public PriorityScoreDetail() {
    }

    public PriorityScoreDetail(String factor, int score, String description) {
        this.factor = factor;
        this.score = score;
        this.description = description;
    }

    public String getFactor() {
        return factor;
    }

    public void setFactor(String factor) {
        this.factor = factor;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String factor;
        private int score;
        private String description;

        public Builder factor(String factor) {
            this.factor = factor;
            return this;
        }

        public Builder score(int score) {
            this.score = score;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public PriorityScoreDetail build() {
            return new PriorityScoreDetail(factor, score, description);
        }
    }
}
