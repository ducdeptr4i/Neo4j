package com.example.model;

import java.util.List;

/**
 * Vấn đề chất lượng dữ liệu được phát hiện bởi DataQualityService.
 */
public class QualityIssue {
    private String code;          // Q1, Q2, ...
    private String severity;      // "ERROR", "WARNING", "INFO"
    private String message;       // mô tả ngắn
    private List<String> items;   // danh sách personId / cặp liên quan

    public QualityIssue() {}

    public QualityIssue(String code, String severity, String message, List<String> items) {
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.items = items;
    }

    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public String getSeverity() { return severity; } public void setSeverity(String severity) { this.severity = severity; }
    public String getMessage() { return message; } public void setMessage(String message) { this.message = message; }
    public List<String> getItems() { return items; } public void setItems(List<String> items) { this.items = items; }
}
