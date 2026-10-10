package com.example.model;

import java.time.LocalDate;

/**
 * Đại diện cho một người trong hệ thống gia phả.
 */
public class Person {
    private String personId;
    private String fullName;
    private String gender;       // "Nam" hoặc "Nữ"
    private LocalDate dateOfBirth;
    private String occupation;
    private LocalDate dateOfDeath; // null nếu còn sống

    public Person() {}

    public Person(String personId, String fullName, String gender,
                  LocalDate dateOfBirth, String occupation, LocalDate dateOfDeath) {
        this.personId = personId;
        this.fullName = fullName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.occupation = occupation;
        this.dateOfDeath = dateOfDeath;
    }

    public boolean isDeceased() {
        return dateOfDeath != null;
    }

    public boolean isMale() {
        return "Nam".equals(gender);
    }

    public boolean isFemale() {
        return "Nữ".equals(gender);
    }

    // Getters & Setters
    public String getPersonId() { return personId; }
    public void setPersonId(String personId) { this.personId = personId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public LocalDate getDateOfDeath() { return dateOfDeath; }
    public void setDateOfDeath(LocalDate dateOfDeath) { this.dateOfDeath = dateOfDeath; }

    @Override
    public String toString() {
        return personId + " - " + fullName;
    }
}
