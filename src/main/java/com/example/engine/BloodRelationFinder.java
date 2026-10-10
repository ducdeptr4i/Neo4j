package com.example.engine;

import java.util.*;

/**
 * Tìm tổ tiên chung gần nhất trong FamilyGraph (Java thuần, dùng cho unit test).
 * Trong production, dùng Cypher qua PersonRepository.findCommonAncestors().
 */
public class BloodRelationFinder {

    private final FamilyGraph graph;

    public BloodRelationFinder(FamilyGraph graph) {
        this.graph = graph;
    }

    /**
     * Tìm tất cả tổ tiên chung của A và B, sắp xếp theo d1+d2 tăng dần.
     * Mỗi kết quả: {ancestorId, d1, d2, pathA (từ ancestor xuống A), pathB (từ ancestor xuống B)}
     */
    public List<Map<String, Object>> findCommonAncestors(String idA, String idB) {
        // Tìm tất cả tổ tiên của A với đường đi và khoảng cách
        Map<String, int[]> ancestorsOfA = new HashMap<>();  // ancestorId -> [d1]
        Map<String, List<String>> pathsToA = new HashMap<>(); // ancestorId -> path từ ancestor xuống A

        bfsAncestors(idA, ancestorsOfA, pathsToA);

        // Tìm tất cả tổ tiên của B và giao với A
        Map<String, int[]> ancestorsOfB = new HashMap<>();
        Map<String, List<String>> pathsToB = new HashMap<>();

        bfsAncestors(idB, ancestorsOfB, pathsToB);

        // Tổ tiên chung: giao của 2 tập
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ancId : ancestorsOfA.keySet()) {
            if (ancestorsOfB.containsKey(ancId)) {
                int d1 = ancestorsOfA.get(ancId)[0];
                int d2 = ancestorsOfB.get(ancId)[0];
                Map<String, Object> entry = new HashMap<>();
                entry.put("ancestorId", ancId);
                entry.put("d1", d1);
                entry.put("d2", d2);
                entry.put("pathA", pathsToA.getOrDefault(ancId, List.of())); // ancestor -> A
                entry.put("pathB", pathsToB.getOrDefault(ancId, List.of())); // ancestor -> B
                result.add(entry);
            }
        }

        // Sắp xếp theo d1+d2 tăng dần
        result.sort(Comparator.comparingInt(m -> (int) m.get("d1") + (int) m.get("d2")));
        return result;
    }

    /**
     * BFS từ personId lên tất cả tổ tiên.
     * @param distances map: ancestorId -> [depth]
     * @param paths map: ancestorId -> path từ ancestor xuống person (ngược lại BFS)
     */
    private void bfsAncestors(String personId,
                               Map<String, int[]> distances,
                               Map<String, List<String>> paths) {
        // Bản thân cũng là "tổ tiên" ở d=0
        distances.put(personId, new int[]{0});
        paths.put(personId, List.of(personId));

        // BFS: queue chứa (currentId, depth, path từ personId lên current)
        Queue<Object[]> queue = new LinkedList<>();
        queue.offer(new Object[]{personId, 0, new ArrayList<>(List.of(personId))});

        while (!queue.isEmpty()) {
            Object[] item = queue.poll();
            String current = (String) item[0];
            int depth = (int) item[1];
            @SuppressWarnings("unchecked")
            List<String> pathUp = (List<String>) item[2]; // path từ personId lên current

            for (String parentId : graph.getParents(current)) {
                if (!distances.containsKey(parentId)) {
                    int newDepth = depth + 1;
                    distances.put(parentId, new int[]{newDepth});

                    // path từ ancestor xuống person = reverse of pathUp + [parentId]
                    List<String> newPathUp = new ArrayList<>(pathUp);
                    newPathUp.add(parentId);
                    // path từ ancestor xuống person: đảo ngược
                    List<String> pathDown = new ArrayList<>(newPathUp);
                    Collections.reverse(pathDown);
                    paths.put(parentId, pathDown);

                    List<String> nextPathUp = new ArrayList<>(newPathUp);
                    queue.offer(new Object[]{parentId, newDepth, nextPathUp});
                }
            }
        }
    }
}
