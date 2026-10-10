package com.example.model;

import java.time.LocalDate;

/**
 * Đại diện cho quan hệ hôn nhân giữa 2 người.
 * MARRIED_TO lưu một chiều trong Neo4j, nhưng logic xử lý đối xứng.
 */
public class Marriage {
    private String personId1;
    private String personId2;
    private LocalDate startDate;
    private LocalDate endDate;     // null nếu còn hiệu lực
    private String endReason;      // "DEATH", "DIVORCE", hoặc null

    public Marriage() {}

    public Marriage(String personId1, String personId2,
                    LocalDate startDate, LocalDate endDate, String endReason) {
        this.personId1 = personId1;
        this.personId2 = personId2;
        this.startDate = startDate;
        this.endDate = endDate;
        this.endReason = endReason;
    }

    public boolean isActive() {
        return endDate == null;
    }

    public boolean isDivorced() {
        return "DIVORCE".equals(endReason);
    }

    public boolean isEndedByDeath() {
        return "DEATH".equals(endReason);
    }

    /**
     * Kiểm tra hôn nhân có "hiệu lực" theo cấu hình:
     * - Kết thúc do DEATH: luôn tính
     * - Kết thúc do DIVORCE: chỉ tính khi includeDivorced = true
     */
    public boolean isEffective(boolean includeDivorced) {
        if (isActive()) return true;
        if (isEndedByDeath()) return true;
        if (isDivorced()) return includeDivorced;
        return false;
    }

    public String getSpouseId(String personId) {
        if (personId1.equals(personId)) return personId2;
        if (personId2.equals(personId)) return personId1;
        return null;
    }

    // Getters & Setters
    public String getPersonId1() { return personId1; }
    public void setPersonId1(String personId1) { this.personId1 = personId1; }

    public String getPersonId2() { return personId2; }
    public void setPersonId2(String personId2) { this.personId2 = personId2; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }
}
