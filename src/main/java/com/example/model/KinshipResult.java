package com.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Kết quả tìm cách xưng hô giữa 2 người.
 */
public class KinshipResult {

    /** Thông tin một chiều xưng hô */
    public static class Direction {
        private String call;       // lời gọi ngắn: "chú", "cháu"
        private String label;      // danh xưng đầy đủ: "chú ruột (em trai của cha)"
        private String selfRefer;  // cách tự xưng: "cháu"
        private String sentence;   // câu đầy đủ

        public Direction() {}
        public Direction(String call, String label, String selfRefer, String sentence) {
            this.call = call; this.label = label; this.selfRefer = selfRefer; this.sentence = sentence;
        }
        public String getCall() { return call; } public void setCall(String call) { this.call = call; }
        public String getLabel() { return label; } public void setLabel(String label) { this.label = label; }
        public String getSelfRefer() { return selfRefer; } public void setSelfRefer(String selfRefer) { this.selfRefer = selfRefer; }
        public String getSentence() { return sentence; } public void setSentence(String sentence) { this.sentence = sentence; }
    }

    /** Tổ tiên chung */
    public static class CommonAncestor {
        private String personId;
        private int d1; // bước từ A lên tổ tiên
        private int d2; // bước từ B lên tổ tiên

        public CommonAncestor() {}
        public CommonAncestor(String personId, int d1, int d2) {
            this.personId = personId; this.d1 = d1; this.d2 = d2;
        }
        public String getPersonId() { return personId; } public void setPersonId(String personId) { this.personId = personId; }
        public int getD1() { return d1; } public void setD1(int d1) { this.d1 = d1; }
        public int getD2() { return d2; } public void setD2(int d2) { this.d2 = d2; }
    }

    /** Đường đi trên đồ thị */
    public static class PathInfo {
        private List<String> nodes;
        private List<Map<String, String>> edges;

        public PathInfo() {}
        public PathInfo(List<String> nodes, List<Map<String, String>> edges) {
            this.nodes = nodes; this.edges = edges;
        }
        public List<String> getNodes() { return nodes; } public void setNodes(List<String> nodes) { this.nodes = nodes; }
        public List<Map<String, String>> getEdges() { return edges; } public void setEdges(List<Map<String, String>> edges) { this.edges = edges; }
    }

    private Person personA;
    private Person personB;
    private String category;     // "SPOUSE", "BLOOD", "IN_LAW", "NONE"
    private String summary;
    private Direction aToB;
    private Direction bToA;
    private int generationGap;
    private List<CommonAncestor> commonAncestors;
    private List<String> explanation;
    private PathInfo path;
    private List<String> notes;

    // Getters & Setters
    public Person getPersonA() { return personA; } public void setPersonA(Person personA) { this.personA = personA; }
    public Person getPersonB() { return personB; } public void setPersonB(Person personB) { this.personB = personB; }
    public String getCategory() { return category; } public void setCategory(String category) { this.category = category; }
    public String getSummary() { return summary; } public void setSummary(String summary) { this.summary = summary; }
    @JsonProperty("aToB")
    public Direction getAToB() { return aToB; }
    @JsonProperty("aToB")
    public void setAToB(Direction aToB) { this.aToB = aToB; }
    @JsonProperty("bToA")
    public Direction getBToA() { return bToA; }
    @JsonProperty("bToA")
    public void setBToA(Direction bToA) { this.bToA = bToA; }
    public int getGenerationGap() { return generationGap; } public void setGenerationGap(int generationGap) { this.generationGap = generationGap; }
    public List<CommonAncestor> getCommonAncestors() { return commonAncestors; } public void setCommonAncestors(List<CommonAncestor> commonAncestors) { this.commonAncestors = commonAncestors; }
    public List<String> getExplanation() { return explanation; } public void setExplanation(List<String> explanation) { this.explanation = explanation; }
    public PathInfo getPath() { return path; } public void setPath(PathInfo path) { this.path = path; }
    public List<String> getNotes() { return notes; } public void setNotes(List<String> notes) { this.notes = notes; }
}
