package com.example.model;

import java.util.List;
import java.util.Map;

/**
 * Kết quả kiểm tra hôn nhân có vi phạm huyết thống không.
 */
public class MarriageCheckResult {

    public static class BloodRelation {
        private String type;  // "DIRECT_LINE", "WITHIN_THREE_GENERATIONS", "BEYOND_THREE_GENERATIONS", "NONE_FOUND"
        private int doi;      // đời = max(d1,d2)+1
        private int d1;
        private int d2;
        private String label;
        private List<String> commonAncestors;

        public BloodRelation() {}
        public String getType() { return type; } public void setType(String type) { this.type = type; }
        public int getDoi() { return doi; } public void setDoi(int doi) { this.doi = doi; }
        public int getD1() { return d1; } public void setD1(int d1) { this.d1 = d1; }
        public int getD2() { return d2; } public void setD2(int d2) { this.d2 = d2; }
        public String getLabel() { return label; } public void setLabel(String label) { this.label = label; }
        public List<String> getCommonAncestors() { return commonAncestors; } public void setCommonAncestors(List<String> commonAncestors) { this.commonAncestors = commonAncestors; }
    }

    public static class Warning {
        private String code;
        private String severity;
        private String message;

        public Warning() {}
        public Warning(String code, String severity, String message) {
            this.code = code; this.severity = severity; this.message = message;
        }
        public String getCode() { return code; } public void setCode(String code) { this.code = code; }
        public String getSeverity() { return severity; } public void setSeverity(String severity) { this.severity = severity; }
        public String getMessage() { return message; } public void setMessage(String message) { this.message = message; }
    }

    public static class PathInfo {
        private List<String> nodes;
        private List<Map<String, String>> edges;
        public PathInfo() {}
        public PathInfo(List<String> nodes, List<Map<String, String>> edges) { this.nodes = nodes; this.edges = edges; }
        public List<String> getNodes() { return nodes; } public void setNodes(List<String> nodes) { this.nodes = nodes; }
        public List<Map<String, String>> getEdges() { return edges; } public void setEdges(List<Map<String, String>> edges) { this.edges = edges; }
    }

    private String verdict;       // "VIOLATION" hoặc "ALLOWED"
    private BloodRelation bloodRelation;
    private String legalBasis;
    private List<String> explanation;
    private List<Warning> additionalWarnings;
    private PathInfo path;

    // Getters & Setters
    public String getVerdict() { return verdict; } public void setVerdict(String verdict) { this.verdict = verdict; }
    public BloodRelation getBloodRelation() { return bloodRelation; } public void setBloodRelation(BloodRelation bloodRelation) { this.bloodRelation = bloodRelation; }
    public String getLegalBasis() { return legalBasis; } public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }
    public List<String> getExplanation() { return explanation; } public void setExplanation(List<String> explanation) { this.explanation = explanation; }
    public List<Warning> getAdditionalWarnings() { return additionalWarnings; } public void setAdditionalWarnings(List<Warning> additionalWarnings) { this.additionalWarnings = additionalWarnings; }
    public PathInfo getPath() { return path; } public void setPath(PathInfo path) { this.path = path; }
}
