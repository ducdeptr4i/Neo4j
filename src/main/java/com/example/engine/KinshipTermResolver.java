package com.example.engine;

import com.example.model.KinshipResult.Direction;
import com.example.model.Person;

import java.util.List;

/**
 * Bảng thuật ngữ xưng hô huyết thống theo mục 6.3 của PROMPT.
 *
 * Ký hiệu:
 *   d1 = số bước từ A lên tổ tiên chung (LCA)
 *   d2 = số bước từ B lên LCA
 *   g  = d1 - d2  (g > 0: B thuộc hàng trên A; g < 0: B hàng dưới)
 *   pathA[i] = người thứ i từ LCA xuống A (pathA[0]=LCA, pathA[d1]=A)
 *   pathB[i] = người thứ i từ LCA xuống B
 */
public class KinshipTermResolver {

    /**
     * Giải quyết cách xưng hô A → B dựa trên tổ tiên chung.
     */
    public static Direction resolve(Person personA, Person personB,
                                    int d1, int d2,
                                    List<String> pathA, List<String> pathB,
                                    FamilyGraph graph, String region) {
        return resolveInternal(personA, personB, d1, d2, pathA, pathB, graph, region, true);
    }

    private static Direction resolveInternal(Person personA, Person personB,
                                            int d1, int d2,
                                            List<String> pathA, List<String> pathB,
                                            FamilyGraph graph, String region,
                                            boolean computeSelfRefer) {
        int g = d1 - d2;

        String call;
        String label;

        // ---------------------------------------------------------
        // B là hậu duệ của A (d1=0): A nhìn xuống B
        // ---------------------------------------------------------
        if (d1 == 0) {
            int k = d2;
            if (k == 1) {
                call = "con";
                label = "con " + (personB.isMale() ? "trai" : "gái");
            } else if (k == 2) {
                // Cháu nội hay ngoại: phụ thuộc pathB[1] (con của A dẫn tới B)
                boolean throughSon = pathB.size() > 1 && graph.getPerson(pathB.get(1)) != null
                        && graph.getPerson(pathB.get(1)).isMale();
                String loai = throughSon ? "nội" : "ngoại";
                call = "cháu";
                label = "cháu " + loai + " (qua con " + (throughSon ? "trai" : "gái") + " của A)";
            } else if (k == 3) {
                call = "chắt";
                label = "chắt (đời 3)";
            } else if (k == 4) {
                call = "chít";
                label = "chít (đời 4)";
            } else {
                call = "hậu duệ";
                label = "hậu duệ đời " + k;
            }

            String selfRefer = "";
            if (computeSelfRefer) {
                Direction rev = resolveInternal(personB, personA, d2, d1, pathB, pathA, graph, region, false);
                selfRefer = rev != null ? rev.getCall() : "";
            }
            return new Direction(call, label, selfRefer,
                    buildSentence(personA.getFullName(), personB.getFullName(), call, selfRefer));
        }

        // ---------------------------------------------------------
        // B là tổ tiên của A (d2=0): A nhìn lên B
        // ---------------------------------------------------------
        if (d2 == 0) {
            int k = d1;
            if (k == 1) {
                call = personB.isMale() ? "ba" : "mẹ";
                label = personB.isMale() ? "cha (ba/bố)" : "mẹ (má)";
            } else if (k == 2) {
                // Ông/bà nội hay ngoại: pathA[1] là con của B dẫn tới A
                boolean throughSon = pathA.size() > 1 && graph.getPerson(pathA.get(1)) != null
                        && graph.getPerson(pathA.get(1)).isMale();
                String loai = throughSon ? "nội" : "ngoại";
                call = (personB.isMale() ? "ông " : "bà ") + loai;
                label = (personB.isMale() ? "ông" : "bà") + " " + loai;
            } else if (k == 3) {
                call = personB.isMale() ? "cụ ông" : "cụ bà";
                label = (personB.isMale() ? "cụ ông" : "cụ bà") + " (đời 3)";
            } else if (k == 4) {
                call = "kỵ";
                label = "kỵ (đời 4)";
            } else {
                call = "tổ tiên";
                label = "tổ tiên đời " + k;
            }

            String selfRefer = "";
            if (computeSelfRefer) {
                Direction rev = resolveInternal(personB, personA, d2, d1, pathB, pathA, graph, region, false);
                selfRefer = rev != null ? rev.getCall() : "";
            }
            return new Direction(call, label, selfRefer,
                    buildSentence(personA.getFullName(), personB.getFullName(), call, selfRefer));
        }

        // ---------------------------------------------------------
        // Cùng hàng (g = 0)
        // ---------------------------------------------------------
        if (g == 0) {
            if (d1 == 1 && d2 == 1) {
                // Anh chị em ruột
                boolean bIsOlder = personB.getDateOfBirth() != null && personA.getDateOfBirth() != null
                        && personB.getDateOfBirth().isBefore(personA.getDateOfBirth());
                if (bIsOlder) {
                    call = personB.isMale() ? "anh" : "chị";
                    label = (personB.isMale() ? "anh" : "chị") + " ruột";
                } else {
                    call = "em " + (personB.isMale() ? "trai" : "gái");
                    label = "em " + (personB.isMale() ? "trai" : "gái") + " ruột";
                }
            } else {
                // Anh/chị/em họ: so sánh vai vế nhánh pathA[1] và pathB[1]
                Person branchA = pathA.size() > 1 ? graph.getPerson(pathA.get(1)) : null;
                Person branchB = pathB.size() > 1 ? graph.getPerson(pathB.get(1)) : null;
                boolean bBranchOlder = branchB != null && branchA != null
                        && branchB.getDateOfBirth() != null && branchA.getDateOfBirth() != null
                        && branchB.getDateOfBirth().isBefore(branchA.getDateOfBirth());

                String relation;
                if (bBranchOlder) {
                    relation = personB.isMale() ? "anh họ" : "chị họ";
                } else {
                    relation = "em họ";
                }

                String note = buildCousinNote(pathA, pathB, graph);
                String suffix = d1 >= 3 ? " xa (đời " + d1 + ")" : "";
                call = relation;
                label = relation + suffix + (note.isEmpty() ? "" : " (" + note + ")");
            }

            String selfRefer = "";
            if (computeSelfRefer) {
                Direction rev = resolveInternal(personB, personA, d2, d1, pathB, pathA, graph, region, false);
                selfRefer = rev != null ? rev.getCall() : "";
            }
            return new Direction(call, label, selfRefer,
                    buildSentence(personA.getFullName(), personB.getFullName(), call, selfRefer));
        }

        // ---------------------------------------------------------
        // B hàng trên A (g > 0): A gọi B theo vai B với A
        // ---------------------------------------------------------
        if (g > 0) {
            Person nodeX = (pathA.size() > d2) ? graph.getPerson(pathA.get(d2)) : null;
            Person branchA = pathA.size() > 1 ? graph.getPerson(pathA.get(1)) : null;
            Person branchBNode = pathB.size() > 1 ? graph.getPerson(pathB.get(1)) : personB;
            Person branchB = d2 == 1 ? personB : branchBNode;

            boolean bBranchOlder = branchB != null && branchA != null
                    && branchB.getDateOfBirth() != null && branchA.getDateOfBirth() != null
                    && branchB.getDateOfBirth().isBefore(branchA.getDateOfBirth());

            call = resolveUncleAuntCall(personB, nodeX, branchA, bBranchOlder, region);

            String hoSuffix = d2 >= 2 ? " họ" : "";
            String gPrefix = "";
            if (g == 2) {
                gPrefix = personB.isMale() ? "ông " : "bà ";
            } else if (g >= 3) {
                String selfRefer = "";
                if (computeSelfRefer) {
                    Direction rev = resolveInternal(personB, personA, d2, d1, pathB, pathA, graph, region, false);
                    selfRefer = rev != null ? rev.getCall() : "";
                }
                return new Direction(
                        "họ hàng xa",
                        "họ hàng bậc trên " + g + " đời",
                        selfRefer,
                        buildSentence(personA.getFullName(), personB.getFullName(), "họ hàng xa", selfRefer)
                );
            }

            label = gPrefix + call + hoSuffix;
            call = gPrefix + call + hoSuffix;

            String selfRefer = "";
            if (computeSelfRefer) {
                Direction rev = resolveInternal(personB, personA, d2, d1, pathB, pathA, graph, region, false);
                selfRefer = rev != null ? rev.getCall() : "";
            }
            return new Direction(call, label, selfRefer,
                    buildSentence(personA.getFullName(), personB.getFullName(), call, selfRefer));
        }

        // ---------------------------------------------------------
        // B hàng dưới A (g < 0)
        // ---------------------------------------------------------
        {
            int absG = -g;
            if (absG == 1) {
                if (d1 == 1) {
                    call = "cháu";
                    label = "cháu ruột";
                } else {
                    call = "cháu họ";
                    label = "cháu họ";
                }
            } else if (absG == 2) {
                call = "cháu";
                label = "cháu (hàng chắt/cháu 2 đời của anh chị em)";
            } else {
                call = "hậu bối";
                label = "hậu bối bậc dưới " + absG + " đời";
            }

            String selfRefer = "";
            if (computeSelfRefer) {
                Direction rev = resolveInternal(personB, personA, d2, d1, pathB, pathA, graph, region, false);
                selfRefer = rev != null ? rev.getCall() : "";
            }
            return new Direction(call, label, selfRefer,
                    buildSentence(personA.getFullName(), personB.getFullName(), call, selfRefer));
        }
    }

    /**
     * Xác định "call" A gọi B khi B hàng trên A (g > 0).
     */
    private static String resolveUncleAuntCall(Person personB, Person nodeX,
                                               Person branchA, boolean bBranchOlder,
                                               String region) {
        boolean xIsMale = nodeX != null && nodeX.isMale();

        if ("COMMON".equals(region)) {
            if (personB.isMale() && xIsMale) {
                return bBranchOlder ? "bác" : "chú";
            } else if (!personB.isMale() && xIsMale) {
                return "cô";
            } else if (personB.isMale() && !xIsMale) {
                return "cậu";
            } else {
                return "dì";
            }
        } else { // NORTH
            if (bBranchOlder) return "bác";
            if (personB.isMale() && xIsMale) return "chú";
            if (!personB.isMale() && xIsMale) return "cô";
            if (personB.isMale() && !xIsMale) return "cậu";
            return "dì";
        }
    }

    /**
     * Tạo ghi chú "con chú / con bác / con cô / con cậu / con dì" cho quan hệ anh em họ.
     */
    private static String buildCousinNote(List<String> pathA, List<String> pathB, FamilyGraph graph) {
        if (pathA.size() < 2 || pathB.size() < 2) return "";
        Person branchA = graph.getPerson(pathA.get(1));
        Person branchB = graph.getPerson(pathB.get(1));
        if (branchA == null || branchB == null) return "";

        boolean branchBIsOlder = branchB.getDateOfBirth() != null && branchA.getDateOfBirth() != null
                && branchB.getDateOfBirth().isBefore(branchA.getDateOfBirth());

        if (branchA.isMale()) {
            if (branchB.isMale()) {
                return branchBIsOlder ? "con bác" : "con chú";
            } else {
                return "con cô";
            }
        } else {
            if (branchB.isMale()) {
                return "con cậu";
            } else {
                return branchBIsOlder ? "con bác" : "con dì";
            }
        }
    }

    private static String buildSentence(String nameA, String nameB, String call, String selfRefer) {
        return nameA + " gọi " + nameB + " là \"" + call + "\"; " + nameA + " xưng là \"" + selfRefer + "\"";
    }
}
