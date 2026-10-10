package com.example.service;

import com.example.engine.FamilyGraph;
import com.example.model.Marriage;
import com.example.model.Person;
import com.example.repository.PersonRepository;

import java.util.*;

/**
 * Tạo dữ liệu nodes/edges cho vis-network, bao gồm generation level.
 */
public class GraphViewService {

    private final FamilyGraph graph;
    private final PersonRepository repo;

    public GraphViewService(FamilyGraph graph, PersonRepository repo) {
        this.graph = graph;
        this.repo = repo;
    }

    /**
     * Trả về {nodes, edges} cho vis-network.
     * @param includeEnded có bao gồm hôn nhân đã kết thúc không
     */
    public Map<String, Object> getGraphData(boolean includeEnded) {
        Map<String, Integer> generationMap = computeGenerations();

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // Nodes
        for (Person p : graph.getAllPersons()) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", p.getPersonId());
            node.put("personId", p.getPersonId());
            node.put("fullName", p.getFullName());
            node.put("gender", p.getGender());
            node.put("dateOfBirth", p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : null);
            node.put("dateOfDeath", p.getDateOfDeath() != null ? p.getDateOfDeath().toString() : null);
            node.put("occupation", p.getOccupation());
            node.put("deceased", p.isDeceased());
            node.put("generation", generationMap.getOrDefault(p.getPersonId(), 0));

            // Label cho vis-network
            String dobYear = p.getDateOfBirth() != null ? String.valueOf(p.getDateOfBirth().getYear()) : "?";
            String label = p.getFullName() + "\n(" + dobYear + (p.isDeceased() ? " – ✝" + p.getDateOfDeath().getYear() : "") + ")";
            node.put("label", label);

            nodes.add(node);
        }

        // Edges: BIOLOGICAL_PARENT_OF
        List<Map<String, String>> parentEdges = repo.findAllParentEdges();
        for (Map<String, String> pe : parentEdges) {
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("id", "p_" + pe.get("parent") + "_" + pe.get("child"));
            edge.put("from", pe.get("parent"));
            edge.put("to", pe.get("child"));
            edge.put("type", "PARENT");
            edge.put("arrows", "to");
            edge.put("color", "#888888");
            edges.add(edge);
        }

        // Edges: MARRIED_TO
        Set<String> addedMarriages = new HashSet<>();
        for (Person p : graph.getAllPersons()) {
            for (Marriage m : graph.getMarriages(p.getPersonId())) {
                String id1 = m.getPersonId1();
                String id2 = m.getPersonId2();
                String key = id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
                if (addedMarriages.add(key)) {
                    if (!includeEnded && !m.isActive()) continue;

                    Map<String, Object> edge = new LinkedHashMap<>();
                    edge.put("id", "m_" + m.getPersonId1() + "_" + m.getPersonId2());
                    edge.put("from", m.getPersonId1());
                    edge.put("to", m.getPersonId2());
                    edge.put("type", "MARRIAGE");
                    edge.put("arrows", "");
                    edge.put("status", m.isActive() ? "ACTIVE" : (m.isEndedByDeath() ? "DEATH" : "DIVORCE"));
                    edge.put("startDate", m.getStartDate() != null ? m.getStartDate().toString() : null);
                    edge.put("endDate", m.getEndDate() != null ? m.getEndDate().toString() : null);
                    edge.put("endReason", m.getEndReason());

                    // Màu cạnh: đang còn = xanh lá, góa = xám, ly hôn = đỏ nét đứt
                    if (m.isActive()) edge.put("color", "#28a745");
                    else if (m.isEndedByDeath()) edge.put("color", "#888888");
                    else edge.put("color", "#dc3545");
                    edge.put("dashes", m.isDivorced());

                    edges.add(edge);
                }
            }
        }

        return Map.of("nodes", nodes, "edges", edges);
    }

    /**
     * Tính generation cho mỗi người:
     * - Không có cha mẹ và không có vợ/chồng biết cấp → cấp 0
     * - Có cha mẹ → max(cấp cha mẹ) + 1
     * - Không có cha mẹ nhưng có vợ/chồng → cùng cấp với vợ/chồng
     * Lặp đến khi ổn định.
     */
    private Map<String, Integer> computeGenerations() {
        Map<String, Integer> gen = new HashMap<>();

        // Khởi tạo: người không có cha mẹ → gen 0
        for (Person p : graph.getAllPersons()) {
            Set<String> parents = graph.getParents(p.getPersonId());
            if (parents.isEmpty()) {
                gen.put(p.getPersonId(), 0);
            }
        }

        // Lặp gán generation cho người có cha mẹ
        boolean changed = true;
        int maxIter = 20;
        while (changed && maxIter-- > 0) {
            changed = false;
            for (Person p : graph.getAllPersons()) {
                String id = p.getPersonId();
                Set<String> parents = graph.getParents(id);
                if (!parents.isEmpty()) {
                    int maxParentGen = -1;
                    boolean allResolved = true;
                    for (String pid : parents) {
                        Integer pgen = gen.get(pid);
                        if (pgen == null) { allResolved = false; break; }
                        maxParentGen = Math.max(maxParentGen, pgen);
                    }
                    if (allResolved && maxParentGen >= 0) {
                        int newGen = maxParentGen + 1;
                        if (!Objects.equals(newGen, gen.get(id))) {
                            gen.put(id, newGen);
                            changed = true;
                        }
                    }
                }
            }
        }

        // Người không có cha mẹ nhưng có vợ/chồng đã biết gen → cùng gen vợ/chồng
        changed = true;
        maxIter = 10;
        while (changed && maxIter-- > 0) {
            changed = false;
            for (Person p : graph.getAllPersons()) {
                String id = p.getPersonId();
                if (!gen.containsKey(id)) {
                    for (Marriage m : graph.getMarriages(id)) {
                        String spouseId = m.getSpouseId(id);
                        if (gen.containsKey(spouseId)) {
                            gen.put(id, gen.get(spouseId));
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }

        // Fallback
        for (Person p : graph.getAllPersons()) {
            gen.putIfAbsent(p.getPersonId(), 0);
        }

        return gen;
    }
}
