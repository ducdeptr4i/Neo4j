package com.example.engine;

import com.example.model.Marriage;
import com.example.model.Person;

import java.util.*;

/**
 * Đồ thị gia phả trong bộ nhớ (Java thuần, không cần Neo4j).
 * Dùng cho unit test và suy luận nhanh.
 */
public class FamilyGraph {

    // Danh sách người, key = personId
    private final Map<String, Person> persons = new LinkedHashMap<>();

    // Cạnh cha mẹ: parent -> set of children
    private final Map<String, Set<String>> parentToChildren = new HashMap<>();

    // Cạnh ngược: child -> set of parents
    private final Map<String, Set<String>> childToParents = new HashMap<>();

    // Hôn nhân: personId -> list of marriages
    private final Map<String, List<Marriage>> marriages = new HashMap<>();

    public FamilyGraph() {}

    // =========================================================
    // Nạp dữ liệu
    // =========================================================

    public void addPerson(Person p) {
        persons.put(p.getPersonId(), p);
    }

    public void addParentEdge(String parentId, String childId) {
        parentToChildren.computeIfAbsent(parentId, k -> new HashSet<>()).add(childId);
        childToParents.computeIfAbsent(childId, k -> new HashSet<>()).add(parentId);
    }

    public void addMarriage(Marriage m) {
        marriages.computeIfAbsent(m.getPersonId1(), k -> new ArrayList<>()).add(m);
        marriages.computeIfAbsent(m.getPersonId2(), k -> new ArrayList<>()).add(m);
    }

    // =========================================================
    // Truy vấn cơ bản
    // =========================================================

    public Person getPerson(String personId) {
        return persons.get(personId);
    }

    public Collection<Person> getAllPersons() {
        return persons.values();
    }

    public Set<String> getChildren(String personId) {
        return parentToChildren.getOrDefault(personId, Set.of());
    }

    public Set<String> getParents(String personId) {
        return childToParents.getOrDefault(personId, Set.of());
    }

    public List<Marriage> getMarriages(String personId) {
        return marriages.getOrDefault(personId, List.of());
    }

    /**
     * Lấy hôn nhân trực tiếp giữa 2 người (nếu có).
     */
    public Optional<Marriage> getDirectMarriage(String idA, String idB) {
        for (Marriage m : getMarriages(idA)) {
            if (idB.equals(m.getSpouseId(idA))) return Optional.of(m);
        }
        return Optional.empty();
    }

    /**
     * Lấy danh sách vợ/chồng hiệu lực của một người.
     * @param includeDivorced có tính hôn nhân đã ly hôn không
     */
    public List<String> getEffectiveSpouses(String personId, boolean includeDivorced) {
        List<String> result = new ArrayList<>();
        for (Marriage m : getMarriages(personId)) {
            if (m.isEffective(includeDivorced)) {
                result.add(m.getSpouseId(personId));
            }
        }
        return result;
    }

    /**
     * Lấy tất cả vợ/chồng (kể cả đã ly hôn và đã chết).
     */
    public List<String> getAllSpouses(String personId) {
        List<String> result = new ArrayList<>();
        for (Marriage m : getMarriages(personId)) {
            result.add(m.getSpouseId(personId));
        }
        return result;
    }

    /**
     * Kiểm tra có tổ tiên chung trong đồ thị bộ nhớ không.
     */
    public Set<String> getAncestors(String personId) {
        Set<String> ancestors = new HashSet<>();
        Queue<String> queue = new LinkedList<>(getParents(personId));
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (ancestors.add(current)) {
                queue.addAll(getParents(current));
            }
        }
        return ancestors;
    }

    public boolean containsPerson(String personId) {
        return persons.containsKey(personId);
    }

    public int size() {
        return persons.size();
    }
}
