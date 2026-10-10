# PROMPT: Xây dựng web quản lý nhân thân (gia phả) bằng Neo4j + Java Maven

> **Cách dùng file này**
> 1. Xem mục **2** (cấu hình & giả định) và sửa nếu khác thực tế của bạn (Java, Neo4j, framework…).
> 2. Gửi **toàn bộ file này** cho AI, kèm 2 file đính kèm: `database.txt` (script Cypher) và `project_structure.txt`.
> 3. Nếu AI trả lời bị cắt do quá dài, bảo nó làm **từng giai đoạn** ở mục **11** và gõ "tiếp tục" sau mỗi giai đoạn.

---

## 1. Vai trò & mục tiêu

Bạn là lập trình viên full-stack cao cấp (Java, Neo4j, web). Hãy **tự sinh toàn bộ file và code hoàn chỉnh, chạy được** cho bài tập môn NoSQL: một **website trực quan** để xem dữ liệu nhân thân **20 người** lưu trong **Neo4j**, và thực hiện **2 chức năng chính**:

1. **Tìm cách xưng hô giữa 2 người** (A gọi B là gì, B gọi A là gì, kèm giải thích từng bước).
2. **Kiểm tra việc kết hôn giữa 2 người có vi phạm quan hệ huyết thống không** (theo Luật Hôn nhân và Gia đình Việt Nam).

Ngoài ra có thêm: sơ đồ gia phả tương tác, danh sách người, và trang **Kiểm tra chất lượng dữ liệu** (mục 5).

Yêu cầu chung: giao diện **trực quan, rõ ràng, dễ quan sát, dễ hiểu, dễ hình dung**; mọi kết quả đều phải có **sơ đồ đường đi** được tô sáng trên đồ thị và **lời giải thích từng bước** bằng tiếng Việt.

---

## 2. Cấu hình & giả định (sửa trực tiếp nếu khác)

| Mục | Giá trị mặc định | Ghi chú |
|---|---|---|
| Java | **17** | `[ĐIỀN nếu khác]` |
| Build | **Maven** (đã có `pom.xml`, groupId `com.example`, lớp chính `com.example.App`) | Giữ nguyên cấu trúc thư mục hiện có, chỉ bổ sung |
| Backend | **Javalin 6** + Jackson + Neo4j Java Driver 5.x + SLF4J | Nhẹ, chạy từ `main()`. Nếu giảng viên yêu cầu Spring Boot 3 → `[ĐIỀN: Spring Boot]` |
| Neo4j | Server 5.x, `bolt://localhost:7687`, user `neo4j`, database `neo4j` | Mật khẩu đọc từ biến môi trường `NEO4J_PASSWORD`, **không hard-code** |
| Frontend | HTML5, CSS3, JavaScript ES6 (module, `fetch`), **Bootstrap 5.3** (CDN), Bootstrap Icons, **vis-network** (CDN) | Không dùng bước build (npm/webpack) |
| Ngôn ngữ giao diện | Tiếng Việt | |
| Vùng miền xưng hô | `COMMON` (phổ thông) | Có tuỳ chọn `NORTH` (xem mục 6.3) |
| Hôn nhân đã **ly hôn** | **Không tính** khi xưng hô (có công tắc bật) | |
| Hôn nhân kết thúc do **tử vong** | **Vẫn tính** khi xưng hô | Ví dụ bà Mai vẫn là mẹ chồng của bà Hạnh |
| Thứ bậc anh/chị/em họ | Theo **vai vế** (thứ bậc cha mẹ), không theo tuổi | Hiển thị thêm ghi chú tuổi thực tế |
| Kết luận kiểm tra kết hôn | Chỉ dựa trên **huyết thống** | Các cảnh báo khác hiển thị riêng (mục 7.3) |

Nếu thiếu thông tin **không chặn được việc code**: dùng giá trị mặc định ở trên, ghi rõ vào README, **không dừng lại hỏi**.

---

## 3. Cấu trúc project cần sinh

Giữ `pom.xml`, `src/main/java/com/example/App.java`, `src/test/java/com/example/AppTest.java`; bổ sung:

```
pom.xml                                   (bổ sung dependency + plugin, UTF-8)
README.md                                 (cách cài, cấu hình, chạy, test, ví dụ)
.gitignore
src/main/java/com/example/
    App.java                              (khởi động: đọc config, kết nối Neo4j, seed nếu DB rỗng, chạy web)
    config/AppConfig.java                 (config.properties + biến môi trường)
    db/Neo4jClient.java                   (Driver, session, đóng khi tắt app)
    db/DataSeeder.java                    (đọc seed.cypher, tách câu lệnh theo ';', chạy tuần tự)
    model/                                (Person, Marriage, ParentEdge, KinshipResult, MarriageCheckResult, QualityIssue, ...)
    repository/PersonRepository.java      (mọi truy vấn Cypher nằm ở đây)
    engine/FamilyGraph.java               (đồ thị trong bộ nhớ: Java thuần, dễ test)
    engine/BloodRelationFinder.java       (tìm tổ tiên chung, d1, d2)
    engine/KinshipTermResolver.java       (bảng thuật ngữ xưng hô)
    service/KinshipService.java
    service/MarriageCheckService.java
    service/DataQualityService.java
    service/GraphViewService.java         (nodes/edges + generation level cho vis-network)
    web/Routes.java                       (đăng ký API, xử lý lỗi JSON thống nhất)
src/main/resources/
    config.properties
    db/seed.cypher                        (= database.txt, giữ nguyên nội dung)
    db/patch.cypher                       (mục 4.5)
    public/index.html
    public/css/style.css
    public/js/{app.js,api.js,graph.js,kinship.js,marriage.js,quality.js,persons.js}
src/test/java/com/example/
    AppTest.java                          (giữ nguyên)
    KinshipServiceTest.java               (dùng dữ liệu 20 người dựng sẵn trong bộ nhớ, KHÔNG cần Neo4j)
    MarriageCheckServiceTest.java
    DataQualityTest.java
```

---

## 4. Dữ liệu Neo4j

### 4.1 Mô hình (KHÔNG đổi tên nhãn/quan hệ/thuộc tính)

- Nút `(:Person {personId, fullName, gender('Nam'|'Nữ'), dateOfBirth(date), occupation, dateOfDeath(date|null)})`; có constraint `personId` UNIQUE.
- `(cha/mẹ)-[:BIOLOGICAL_PARENT_OF {since:date}]->(con)` — mỗi người con có **2 quan hệ** (từ cha và từ mẹ).
- `(a)-[:MARRIED_TO {startDate:date, endDate:date|null, endReason:'DEATH'|'DIVORCE'|null}]->(b)` — **chỉ lưu một chiều** ⇒ mọi truy vấn phải dùng **vô hướng** `-[:MARRIED_TO]-`.

### 4.2 Tóm tắt 20 người (để AI viết test; nguồn chuẩn là `database.txt`)

| ID | Họ tên | Giới | Sinh | Mất | | ID | Họ tên | Giới | Sinh | Mất |
|---|---|---|---|---|---|---|---|---|---|---|
| P001 | Nguyễn Văn Hòa | Nam | 1938-02-14 | 2018-11-03 | | P011 | Nguyễn Minh Đức | Nam | 1988-03-15 | |
| P002 | Lê Thị Mai | Nữ | 1941-07-21 | | | P012 | Nguyễn Ngọc Anh | Nữ | 1992-10-02 | |
| P003 | Nguyễn Văn Thành | Nam | 1963-05-10 | | | P013 | Nguyễn Hoàng Nam | Nam | 1999-07-11 | |
| P004 | Phạm Thị Hạnh | Nữ | 1965-09-22 | | | P014 | Nguyễn Thu Hà | Nữ | 2003-02-25 | |
| P005 | Nguyễn Văn Bình | Nam | 1974-12-03 | | | P015 | Võ Minh Khang | Nam | 1996-06-05 | |
| P006 | Trần Thị Thu | Nữ | 1975-04-18 | | | P016 | Đặng Thảo Vy | Nữ | 2001-09-17 | |
| P007 | Nguyễn Thị Lan | Nữ | 1971-06-16 | | | P017 | Đỗ Thị Ngọc | Nữ | 1990-12-08 | |
| P008 | Võ Văn Quang | Nam | 1969-01-30 | 2022-06-30 | | P018 | Lê Văn Sơn | Nam | 1990-01-19 | |
| P009 | Nguyễn Thị Hương | Nữ | 1977-08-09 | | | P019 | Nguyễn Gia Bảo | Nam | 2015-04-12 | |
| P010 | Đặng Văn Phúc | Nam | 1975-01-30 | | | P020 | Lê Minh Châu | Nữ | 2018-09-01 | |

**Cha mẹ → con (24 cạnh):** (P001+P002) → P003, P007, P005, P009 · (P003+P004) → P011, P012 · (P005+P006) → P013, P014 · (P007+P008) → P015 · (P009+P010) → P016 · (P011+P017) → P019 · (P012+P018) → P020.

**Hôn nhân (7):** P001–P002 (1961, kết thúc DEATH 2018) · P003–P004 (còn) · P005–P006 (1997, **DIVORCE** 2008) · P007–P008 (1994, DEATH 2022) · P009–P010 (còn) · P011–P017 (còn) · P012–P018 (còn).

### 4.3 Kết quả rà soát database (đã thực hiện thủ công trước khi viết prompt)

**Kết luận: dữ liệu ổn, KHÔNG có lỗi bắt buộc phải sửa.** Đã kiểm tra:

- 20 nút, 24 cạnh cha/mẹ → con, 7 cạnh hôn nhân; không trùng ID; đồ thị tổ tiên là DAG (không có vòng lặp).
- Mỗi người con có đúng 2 cha mẹ **khác giới**; cha/mẹ hơn con tối thiểu 22 tuổi.
- Ngày kết hôn đều sau ngày sinh 2 người (tuổi kết hôn ≥ 19) và **trước** ngày sinh người con đầu.
- `endDate` của hôn nhân kết thúc do `DEATH` khớp đúng `dateOfDeath` (P001, P008); hôn nhân còn hiệu lực thì cả hai vợ/chồng đang sống.
- **Không có cặp vợ chồng nào có quan hệ huyết thống** (sáu người kết hôn vào P004, P006, P008, P010, P017, P018 đều là người ngoài dòng máu).

### 4.4 Lưu ý & hạn chế (AI phải xử lý trong code/README, không cần sửa dữ liệu)

1. `MARRIED_TO` một chiều ⇒ dùng truy vấn vô hướng (đã nêu ở 4.1).
2. Thuộc tính `since` của `BIOLOGICAL_PARENT_OF` trùng `dateOfBirth` của con ⇒ dư thừa; ứng dụng **không dựa vào** nó.
3. `database.txt` gồm nhiều câu lệnh cách nhau bởi `;` ⇒ driver Java không chạy được như một query; `DataSeeder` phải tách và chạy từng câu (bỏ chunk rỗng/chỉ có comment; **bỏ qua** câu `MATCH p=... RETURN p` cuối file vì chỉ để xem trong Neo4j Browser). Đọc file bằng **UTF-8** (Windows mặc định không phải UTF-8 → thêm `-Dfile.encoding=UTF-8`, `project.build.sourceEncoding=UTF-8`).
4. Seed dùng `MERGE` nên chạy lại an toàn (idempotent). Ứng dụng chỉ tự seed khi DB chưa có nút `Person`.
5. Người kết hôn vào (P004, P006, P008, P010, P017, P018) **không có cha mẹ/anh chị em trong DB** ⇒ kết quả "không phát hiện huyết thống" chỉ có nghĩa **"trong dữ liệu hiện có"**. Giao diện phải nêu rõ giới hạn này.
6. Dữ liệu chỉ có quan hệ huyết thống ruột (chưa có con nuôi, cha dượng/mẹ kế). Thiết kế code cho phép mở rộng thêm loại quan hệ sau này, nhưng **không thêm dữ liệu** khi chưa được yêu cầu.
7. Chỉ dùng được Neo4j 5.x cho hàm `elementId()`; nếu Neo4j 4.x dùng `id()`.

**Điểm cần người dùng tự xác nhận (không chặn code):** P018 *Lê Văn Sơn* cùng họ *Lê* với P002 *Lê Thị Mai* nhưng trong DB không có quan hệ họ hàng ⇒ coi là **trùng họ ngẫu nhiên**. Khoảng trống 1963–1971 giữa P003 và P007 có thể thiếu anh/chị/em; chỉ ảnh hưởng nếu sau này muốn xưng hô theo "thứ" (Hai, Ba, Út…).

### 4.5 `patch.cypher` (tuỳ chọn, idempotent — chạy sau seed)

```cypher
CREATE INDEX person_fullname IF NOT EXISTS FOR (p:Person) ON (p.fullName);
```

---

## 5. Trang "Kiểm tra chất lượng dữ liệu" (Bước 0 — làm trước các chức năng chính)

`DataQualityService` chạy các truy vấn Cypher trong `PersonRepository` và trả `[ {code, severity: ERROR|WARNING|INFO, message, items[]} ]`. UI hiển thị bảng ✅/⚠️/❌, mở rộng từng mục để xem chi tiết. Với dữ liệu hiện tại kỳ vọng: **0 ERROR, 0 WARNING**, một vài INFO. Các kiểm tra:

| Mã | Mức | Nội dung |
|---|---|---|
| Q1 | ERROR | Thiếu `fullName`/`dateOfBirth` hoặc `gender` ∉ {Nam, Nữ} |
| Q2 | ERROR | Người có > 2 cha mẹ ruột |
| Q3 | ERROR | Hai cha mẹ cùng giới |
| Q4 | ERROR | Có vòng lặp tổ tiên |
| Q5 | ERROR/WARN | Cha/mẹ sinh sau con (ERROR); chênh tuổi < 15 (WARNING) |
| Q6 | ERROR | Hôn nhân: tự cưới mình, `endDate < startDate`, kết hôn trước khi sinh |
| Q7 | ERROR | `endReason='DEATH'` nhưng không khớp `dateOfDeath`; hôn nhân còn hiệu lực nhưng một người đã mất |
| Q8 | ERROR | Một người có 2 hôn nhân chồng lấn thời gian |
| Q9 | ERROR | **Kiểm toán**: cặp vợ chồng hiện có nằm trong diện cấm huyết thống (dùng `MarriageCheckService`) |
| Q10 | INFO | Người không có quan hệ nào; người kết hôn vào không có thông tin cha mẹ |
| Q11 | INFO | Con sinh trước ngày kết hôn của cha mẹ |

Mẫu Cypher (AI viết tiếp các mục còn lại theo cùng phong cách, luôn dùng tham số):

```cypher
// Q2
MATCH (c:Person)<-[:BIOLOGICAL_PARENT_OF]-(p:Person)
WITH c, collect(p.personId) AS parents WHERE size(parents) > 2
RETURN c.personId AS childId, parents;

// Q3
MATCH (p1:Person)-[:BIOLOGICAL_PARENT_OF]->(c:Person)<-[:BIOLOGICAL_PARENT_OF]-(p2:Person)
WHERE p1.personId < p2.personId AND p1.gender = p2.gender
RETURN c.personId AS childId, p1.personId AS parent1, p2.personId AS parent2;

// Q4
MATCH (x:Person)-[:BIOLOGICAL_PARENT_OF*1..]->(x) RETURN DISTINCT x.personId AS personId;

// Q5
MATCH (p:Person)-[:BIOLOGICAL_PARENT_OF]->(c:Person)
WITH p, c, duration.between(p.dateOfBirth, c.dateOfBirth).years AS gap
WHERE gap < 15
RETURN p.personId AS parentId, c.personId AS childId, gap;

// Q7
MATCH (a:Person)-[m:MARRIED_TO]->(b:Person)
WHERE (m.endReason = 'DEATH' AND NOT (a.dateOfDeath = m.endDate OR b.dateOfDeath = m.endDate))
   OR (m.endDate IS NULL AND (a.dateOfDeath IS NOT NULL OR b.dateOfDeath IS NOT NULL))
RETURN a.personId AS a, b.personId AS b, m.endDate AS endDate, m.endReason AS reason;

// Q8
MATCH (p:Person)-[m1:MARRIED_TO]-(:Person), (p)-[m2:MARRIED_TO]-(:Person)
WHERE elementId(m1) < elementId(m2)
  AND m1.startDate <= coalesce(m2.endDate, date('9999-12-31'))
  AND m2.startDate <= coalesce(m1.endDate, date('9999-12-31'))
RETURN p.personId AS personId;
```

---

## 6. Chức năng 1 — Tìm cách xưng hô giữa 2 người

### 6.1 Đầu vào / đầu ra

`GET /api/kinship?a=P011&b=P005&includeDivorced=false&region=COMMON`

```json
{
  "personA": {...}, "personB": {...},
  "category": "SPOUSE | BLOOD | IN_LAW | NONE",
  "summary": "Bình là chú ruột của Đức",
  "aToB": { "call": "chú", "label": "chú ruột (em trai của cha)", "selfRefer": "cháu", "sentence": "Đức gọi Bình là chú; Đức xưng là cháu" },
  "bToA": { "call": "cháu", "label": "cháu ruột (con của anh trai)", "selfRefer": "chú", "sentence": "..." },
  "generationGap": 1,
  "commonAncestors": [ {"personId":"P001","d1":2,"d2":1} ],
  "explanation": ["Bước 1: ...", "Bước 2: ..."],
  "path": { "nodes": ["P011","P003","P001","P005"], "edges": [ {"from":"P003","to":"P011","type":"BIOLOGICAL_PARENT_OF"} ] },
  "notes": ["Hôn nhân P005–P006 đã ly hôn nên không tính..."]
}
```

- `call` = **lời gọi trực tiếp** (ngắn), `label` = danh xưng đầy đủ, `selfRefer` = cách tự xưng (= `call` của chiều ngược lại; giữa vợ chồng là "anh/em").
- Luôn trả **cả hai chiều** (A→B và B→A).

### 6.2 Thứ tự xử lý

0. A = B → lỗi 400. ID không tồn tại → 404.
1. **Vợ chồng trực tiếp** (có cạnh `MARRIED_TO` giữa A và B, tính cả đã kết thúc): trạng thái *Đang là vợ chồng / Góa (một người đã mất) / Đã ly hôn (ngày)*.
2. **Huyết thống**: tìm tổ tiên chung gần nhất bằng **Cypher** (bắt buộc dùng Neo4j):
   ```cypher
   MATCH (a:Person {personId:$a}), (b:Person {personId:$b})
   MATCH pa = (anc:Person)-[:BIOLOGICAL_PARENT_OF*0..10]->(a)
   MATCH pb = (anc)-[:BIOLOGICAL_PARENT_OF*0..10]->(b)
   RETURN anc.personId AS ancestorId, length(pa) AS d1, length(pb) AS d2,
          [n IN nodes(pa) | n.personId] AS pathA,   // từ tổ tiên xuống A
          [n IN nodes(pb) | n.personId] AS pathB    // từ tổ tiên xuống B
   ORDER BY d1 + d2;
   ```
   Lấy tổ tiên chung có `d1+d2` nhỏ nhất. Hai người có **2 tổ tiên chung tại (1,1)** ⇒ anh chị em **ruột**; chỉ 1 ⇒ **cùng cha khác mẹ / cùng mẹ khác cha**.
3. **Thông gia (qua hôn nhân)** — mục 6.4.
4. **Không có quan hệ**: `category = NONE`, nói rõ "không tìm thấy quan hệ trong dữ liệu hiện có".
5. Ở mọi trường hợp có kết nối, thêm đường đi ngắn nhất bằng Cypher `shortestPath((a)-[:BIOLOGICAL_PARENT_OF|MARRIED_TO*..12]-(b))` để vẽ tô sáng.

Nguyên tắc kiến trúc: Neo4j là nguồn dữ liệu duy nhất. Với ~20 nút, cho phép nạp toàn bộ đồ thị vào `FamilyGraph` (1 truy vấn) để bộ suy luận Java thuần xử lý và dễ unit-test, **nhưng** phải dùng Cypher cho: tổ tiên chung, `shortestPath`, toàn bộ kiểm tra chất lượng dữ liệu, danh sách/chi tiết người.

### 6.3 Bảng thuật ngữ huyết thống (A gọi B là …)

Ký hiệu: `d1` = số bước từ A lên tổ tiên chung (LCA), `d2` = từ B lên LCA, `g = d1 − d2` (g > 0: B thuộc hàng **trên** A). `pathA[i]` là người thứ i từ LCA xuống A (`pathA[0]=LCA`, `pathA[d1]=A`).

| Điều kiện | A gọi B là |
|---|---|
| `d1=0` (B là hậu duệ của A), k=d2 | k=1: **con** (con trai/con gái) · k=2: **cháu nội** (qua con trai của A) / **cháu ngoại** (qua con gái) · k=3: **chắt** (nội/ngoại) · k=4: **chít** · k≥5: "hậu duệ đời k" |
| `d2=0` (B là tổ tiên), k=d1 | k=1: **cha (ba/bố)** / **mẹ (má)** · k=2: **ông/bà nội** nếu người trung gian `pathA[d1-1]` là nam, ngược lại **ông/bà ngoại** · k=3: **cụ ông/cụ bà** (nội/ngoại) · k=4: **kỵ** · k≥5: "tổ tiên đời k" |
| `d1=d2=1` | **anh/chị** nếu B sinh trước A; **em (trai/gái)** nếu sau. Ghi rõ "ruột" hoặc "cùng cha khác mẹ/cùng mẹ khác cha" |
| `d1=d2≥2` (cùng hàng) | **anh/chị/em họ**. Anh/chị hay em: so sánh ngày sinh của `pathA[1]` và `pathB[1]` (2 người con của LCA dẫn tới A và B): nhánh B sinh trước ⇒ B là anh/chị. Ghi chú theo cha/mẹ trung gian của B: *con chú / con bác / con cô / con cậu / con dì*. `d1≥3`: "anh/chị/em họ xa (đời k)". Nếu tuổi thực tế ngược với vai vế ⇒ thêm ghi chú |
| `g>0` (B hàng trên) | Đặt `X = pathA[d2]` (tổ tiên của A ngang hàng B). Xem bảng dưới |
| `g<0` (B hàng dưới) | g=−1: **cháu** (`d1=1`: cháu ruột; `d1≥2`: **cháu họ**) · g=−2: **cháu** (kèm ghi chú "hàng chắt/cháu 2 đời của anh chị em", nội/ngoại theo giới của người trung gian) · g≤−3: "hậu bối bậc dưới k đời" |

Với `g>0` (nhánh lớn/nhỏ so sánh `pathA[1]` với `pathB[1]`):

| Giới B | Giới X | Nhánh B so với A | `COMMON` | `NORTH` |
|---|---|---|---|---|
| Nam | Nam (bên nội) | lớn hơn | **bác** | bác |
| Nam | Nam | nhỏ hơn | **chú** | chú |
| Nữ | Nam | bất kỳ | **cô** | lớn hơn → **bác**, nhỏ hơn → **cô** |
| Nam | Nữ (bên ngoại) | bất kỳ | **cậu** | lớn hơn → **bác**, nhỏ hơn → **cậu** |
| Nữ | Nữ | bất kỳ | **dì** | lớn hơn → **bác**, nhỏ hơn → **dì** |

- `d2≥2` ⇒ thêm hậu tố "**họ**" (chú họ, cô họ…).
- `g=2` ⇒ thêm tiền tố: **ông bác / ông chú / ông cậu / bà cô / bà dì** (anh em của ông bà). `g≥3`: "họ hàng bậc trên g đời (tổ chung: …)".
- Chiều ngược (B gọi A) tính bằng cách **đổi vai A↔B** và gọi lại cùng hàm.

### 6.4 Thông gia (quan hệ qua hôn nhân)

Chỉ xét hôn nhân "hiệu lực" theo cấu hình (`DEATH` luôn tính; `DIVORCE` chỉ tính khi `includeDivorced=true`, và ghi nhãn "(cũ)"). Xét lần lượt các mẫu (ưu tiên đường ngắn nhất):

**Mẫu 1 — Vợ/chồng của A là họ hàng ruột của B** (S = vợ/chồng của A; A gọi B theo quan hệ của S với B):

| B là … của S | A nữ (S là chồng) | A nam (S là vợ) |
|---|---|---|
| Cha / Mẹ | bố chồng (gọi "ba/bố") / mẹ chồng (gọi "mẹ") | bố vợ / mẹ vợ |
| Anh / Chị của S | anh chồng / chị chồng | anh vợ / chị vợ |
| Em trai/em gái của S | em chồng (chú / cô) | em vợ |
| Con của S (không phải con của A) | con chồng (con riêng) | con vợ (con riêng) |
| Họ hàng khác | "*{thuật ngữ S gọi B}* bên chồng/vợ" | tương tự |

**Mẫu 2 — B là vợ/chồng của họ hàng ruột R của A** (A gọi B theo quan hệ của R với A):

| R là … của A | B là |
|---|---|
| Anh trai / Em trai | **chị dâu** / **em dâu** |
| Chị gái / Em gái | **anh rể** / **em rể** |
| Con trai / Con gái | **con dâu** / **con rể** |
| Chú / Bác trai / Cậu | **thím** / **bác gái** / **mợ** |
| Cô / Dì | **dượng** |
| Cháu trai / Cháu gái | **cháu dâu** / **cháu rể** |
| Cha / Mẹ (mà B không phải cha/mẹ ruột) | **mẹ kế** / **dượng (cha dượng)** |

**Mẫu 3 — Hai vòng hôn nhân:** *sui gia (thông gia)* (con của A cưới con của B) · *chị em dâu* (hai vợ của hai anh em) · *anh em đồng hao / cột chèo* (hai chồng của hai chị em). Ngoài các mẫu trên: `category=IN_LAW`, `call` chung chung kiểu "chồng của em chồng" và **vẫn vẽ đường đi**.

### 6.5 Ca kiểm thử bắt buộc (mặc định `COMMON`, không tính ly hôn)

| # | A | B | A gọi B | B gọi A |
|---|---|---|---|---|
| T1 | P011 Đức | P003 Thành | ba (cha) | con |
| T2 | P011 Đức | P002 Mai | bà nội | cháu (cháu nội) |
| T3 | P019 Bảo | P001 Hòa | cụ ông (cụ nội) | chắt (chắt nội) |
| T4 | P011 Đức | P012 Ngọc Anh | em (em gái ruột) | anh |
| T5 | P011 Đức | P005 Bình | chú | cháu |
| T6 | P011 Đức | P007 Lan | cô | cháu |
| T7 | P013 Nam | P011 Đức | anh họ (con bác) | em họ (con chú) |
| T8 | P013 Nam | P015 Khang | anh họ (con cô) | em họ (con cậu) |
| T9 | P012 Ngọc Anh | P016 Vy | em họ (con cô) | chị họ (con cậu) |
| T10 | P020 Châu | P019 Bảo | anh họ (con cậu) | em họ (con cô) |
| T11 | P019 Bảo | P005 Bình | ông chú | cháu |
| T12 | P019 Bảo | P007 Lan | bà cô | cháu |
| T13 | P019 Bảo | P014 Hà | cô họ | cháu họ |
| T14 | P004 Hạnh | P001 Hòa | bố chồng (gọi "ba") | con dâu |
| T15 | P018 Sơn | P004 Hạnh | mẹ vợ (gọi "mẹ") | con rể |
| T16 | P004 Hạnh | P007 Lan | em chồng (cô) | chị dâu |
| T17 | P017 Ngọc | P012 Ngọc Anh | em chồng | chị dâu |
| T18 | P003 Thành | P010 Phúc | em rể | anh vợ |
| T19 | P010 Phúc | P008 Quang | anh em đồng hao (cột chèo) | (đối xứng) |
| T20 | P005 Bình | P006 Thu | **Đã ly hôn** 2008-08-15 (trạng thái vợ chồng cũ) | (đối xứng) |
| T21 | P006 Thu | P003 Thành | không có quan hệ (vì ly hôn) — bật `includeDivorced`: anh chồng (cũ) | em dâu (cũ) |
| T22 | P004 Hạnh | P006 Thu | không có quan hệ — bật `includeDivorced`: chị em dâu (cũ) | (đối xứng) |
| T23 | P004 Hạnh | P002 Mai (đối chiếu) | mẹ chồng | con dâu |

---

## 7. Chức năng 2 — Kiểm tra kết hôn có vi phạm quan hệ huyết thống không

### 7.1 Căn cứ pháp lý (hiển thị trong kết quả; nhắc người dùng đối chiếu văn bản luật hiện hành)

Luật Hôn nhân và Gia đình 2014, **Điều 5 khoản 2 điểm d** cấm kết hôn giữa những người **cùng dòng máu về trực hệ** và giữa những người **có họ trong phạm vi ba đời** (giải thích tại **Điều 3**): cha mẹ là đời thứ nhất; anh chị em cùng cha mẹ (kể cả cùng cha khác mẹ/cùng mẹ khác cha) là đời thứ hai; anh chị em con chú, con bác, con cô, con cậu, con dì là đời thứ ba.

### 7.2 Thuật toán (dùng cùng truy vấn tổ tiên chung ở 6.2)

Với mỗi tổ tiên chung, `d1`, `d2` như mục 6.3; lấy tổ tiên chung cho **đời nhỏ nhất**:

| Điều kiện | Phân loại | Kết luận |
|---|---|---|
| `d1=0` hoặc `d2=0` | Cùng dòng máu **trực hệ** (cha mẹ–con, ông bà–cháu, cụ–chắt… **mọi đời**) | ❌ **VI PHẠM** |
| `đời = max(d1,d2)+1 ≤ 3` | Trong phạm vi **ba đời** (anh chị em ruột/cùng cha/cùng mẹ: đời 2; chú/bác/cô/cậu/dì – cháu ruột và anh chị em họ: đời 3) | ❌ **VI PHẠM** |
| `đời ≥ 4` | Có huyết thống nhưng **ngoài ba đời** (vd. cháu họ) | ✅ **Không vi phạm** (kèm ghi chú lệch vai vế) |
| Không có tổ tiên chung | Không phát hiện huyết thống **trong dữ liệu hiện có** | ✅ **Không vi phạm** (kèm cảnh báo giới hạn dữ liệu, mục 4.4 điểm 5) |

Kết quả JSON `GET /api/marriage-check?a=..&b=..`:

```json
{
  "verdict": "VIOLATION | ALLOWED",
  "bloodRelation": { "type": "DIRECT_LINE | WITHIN_THREE_GENERATIONS | BEYOND_THREE_GENERATIONS | NONE_FOUND",
                     "doi": 3, "d1": 2, "d2": 2, "label": "Anh chị em họ (con của anh chị em ruột)",
                     "commonAncestors": ["P001","P002"] },
  "legalBasis": "Luật HN&GĐ 2014, Điều 5 khoản 2 điểm d; Điều 3",
  "explanation": ["Bước 1: ...", "Bước 2: ..."],
  "additionalWarnings": [ {"code":"SAME_SEX","severity":"WARNING","message":"..."} ],
  "path": { "nodes": [...], "edges": [...] }
}
```

### 7.3 Cảnh báo bổ sung (KHÔNG thay đổi `verdict`, hiển thị ở khối riêng)

| Mã | Nội dung |
|---|---|
| `SAME_SEX` | Cùng giới tính: Nhà nước không thừa nhận hôn nhân giữa những người cùng giới (Điều 8 khoản 2) |
| `UNDERAGE` | Chưa đủ tuổi kết hôn (nam ≥ 20, nữ ≥ 18, tính đến hôm nay) |
| `DECEASED` | Một trong hai người đã mất |
| `ALREADY_MARRIED` | Một trong hai đang có hôn nhân (`endDate` null) với người khác |
| `FORBIDDEN_IN_LAW` | Thuộc diện cấm thông gia (Điều 5 khoản 2 điểm d): cha/mẹ chồng–con dâu, cha/mẹ vợ–con rể, cha dượng–con riêng của vợ, mẹ kế–con riêng của chồng (tính cả hôn nhân đã kết thúc) |
| `GENERATION_MISMATCH` | Có huyết thống nhưng lệch thế hệ (`g≠0`), dù ngoài ba đời — chỉ mang tính phong tục |

### 7.4 Ca kiểm thử bắt buộc

| # | A | B | Verdict | Lý do |
|---|---|---|---|---|
| M1 | P003 Thành | P012 Ngọc Anh | ❌ VIOLATION | Trực hệ, cha–con |
| M2 | P001 Hòa | P011 Đức | ❌ VIOLATION | Trực hệ, ông–cháu |
| M3 | P011 Đức | P012 Ngọc Anh | ❌ VIOLATION | Anh chị em ruột (đời 2) |
| M4 | P005 Bình | P012 Ngọc Anh | ❌ VIOLATION | Chú–cháu ruột (đời 3) |
| M5 | P011 Đức | P016 Vy | ❌ VIOLATION | Anh chị em họ (đời 3) |
| M6 | P015 Khang | P012 Ngọc Anh | ❌ VIOLATION | Anh chị em họ (đời 3) |
| M7 | P019 Bảo | P020 Châu | ❌ VIOLATION | Anh chị em họ (đời 3) |
| M8 | P014 Hà | P019 Bảo | ✅ ALLOWED (huyết thống) | Đời 4; cảnh báo `UNDERAGE`, `GENERATION_MISMATCH` |
| M9 | P004 Hạnh | P018 Sơn | ✅ ALLOWED (huyết thống) | Không có huyết thống; cảnh báo `FORBIDDEN_IN_LAW` (mẹ vợ–con rể) |
| M10 | P015 Khang | P017 Ngọc | ✅ ALLOWED (huyết thống) | Không có huyết thống; cảnh báo `ALREADY_MARRIED` |
| M11 | P002 Mai | P018 Sơn | ✅ ALLOWED (huyết thống) | Cùng họ Lê nhưng không có quan hệ trong DB; nêu rõ giới hạn dữ liệu |

---

## 8. API (JSON, UTF-8; lỗi dạng `{ "error": "CODE", "message": "..." }` với 400/404/500)

| Method | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/api/health` | Trạng thái app + kết nối Neo4j |
| GET | `/api/persons` | Danh sách người (kèm tuổi, còn sống/đã mất) |
| GET | `/api/persons/{id}` | Chi tiết: cha mẹ, vợ/chồng (kèm trạng thái), con, anh chị em |
| GET | `/api/graph?includeEnded=true` | `nodes` (có `generation`), `edges` (`PARENT`/`MARRIAGE` + trạng thái) |
| GET | `/api/kinship?a=&b=&includeDivorced=&region=` | Chức năng 1 |
| GET | `/api/marriage-check?a=&b=` | Chức năng 2 |
| GET | `/api/quality` | Kiểm tra chất lượng dữ liệu |

Quy tắc chung: mọi Cypher dùng **tham số** (không nối chuỗi); dùng `executeRead`/`executeWrite`; chuyển `date` sang chuỗi ISO `yyyy-MM-dd`.

**Tính `generation` cho đồ thị:** người không có cha mẹ và không có vợ/chồng đã biết cấp ⇒ cấp 0; người có cha mẹ ⇒ `max(cấp cha mẹ)+1`; người không có cha mẹ nhưng có vợ/chồng ⇒ **cùng cấp với vợ/chồng**. Lặp đến khi ổn định.

---

## 9. Giao diện (HTML5 + Bootstrap 5 + CSS + JS)

Trang đơn `index.html`, thanh điều hướng + 5 tab, responsive (mobile → desktop), tiếng Việt, có trạng thái *đang tải* (spinner) và hộp báo lỗi thân thiện:

1. **Sơ đồ gia phả** (trang mặc định): đồ thị vis-network phân tầng theo `generation` (trên → dưới).
   - Nút: **Nam = xanh dương, Nữ = hồng**, người đã mất = **xám + ký hiệu ✝**; hiển thị tên, năm sinh–mất, nghề.
   - Cạnh cha/mẹ → con: mũi tên đặc; **hôn nhân đang còn: nét liền xanh lá**, **góa: nét liền xám**, **ly hôn: nét đứt đỏ**.
   - Click nút → panel bên phải: thông tin chi tiết, cha mẹ, vợ/chồng, con (bấm để nhảy sang người đó).
   - Bảng chú giải, nút *Vừa khung / Phóng to / Thu nhỏ*, công tắc *ẩn/hiện hôn nhân đã kết thúc*.
2. **Tìm cách xưng hô**: 2 ô chọn người (có tìm kiếm, hiển thị *Tên (năm sinh)*), nút **⇄ đổi vai**, công tắc *tính cả hôn nhân đã ly hôn*, chọn *Vùng miền (Phổ thông/Miền Bắc)*, nút **Ví dụ nhanh** (nạp sẵn các ca T1–T23). Kết quả: thẻ lớn hiển thị **"Đức gọi Bình là *chú* — Bình gọi Đức là *cháu*"**, danh xưng đầy đủ, **các bước giải thích đánh số**, đồ thị nhỏ **tô sáng đường đi** (nút/cạnh ngoài đường đi mờ đi), ghi chú.
3. **Kiểm tra kết hôn**: cùng bộ chọn; kết quả là **banner lớn**: 🔴 "VI PHẠM" hoặc 🟢 "KHÔNG VI PHẠM"; thước đo **"đời thứ N / giới hạn 3 đời"**; loại quan hệ huyết thống; tổ tiên chung; lời giải thích từng bước; căn cứ pháp lý; khối **Cảnh báo bổ sung** riêng; đồ thị tô sáng đường huyết thống. Nút **Ví dụ nhanh** nạp các ca M1–M11.
4. **Danh sách người**: bảng 20 người có tìm kiếm/sắp xếp/lọc theo giới và còn sống-đã mất.
5. **Chất lượng dữ liệu**: hiển thị kết quả `/api/quality` (mục 5).

Kỹ thuật: mỗi tab tách file JS riêng (ES module); gọi API bằng `fetch`; escape dữ liệu khi chèn vào DOM; HTML ngữ nghĩa, có `aria-label` cho nút/biểu tượng; CDN chỉ gồm Bootstrap 5.3, Bootstrap Icons, vis-network.

---

## 10. Yêu cầu phi chức năng

- **Cấu hình:** `config.properties` + biến môi trường ghi đè: `NEO4J_URI`, `NEO4J_USER`, `NEO4J_PASSWORD`, `NEO4J_DATABASE`, `PORT` (mặc định 8080), `APP_SEED_IF_EMPTY=true`.
- **Chạy:** `mvn compile exec:java` (cấu hình `exec-maven-plugin`, `mainClass=com.example.App`) và tuỳ chọn tạo jar chạy được (`maven-shade-plugin`). Mở `http://localhost:8080`.
- **Đóng gói tài nguyên:** frontend nằm trong `src/main/resources/public` và được phục vụ tĩnh từ classpath.
- **Test:** JUnit 5; test thuật toán chạy trên đồ thị 20 người dựng trong bộ nhớ (không cần Neo4j) và **bao phủ toàn bộ T1–T23, M1–M11**.
- **Chất lượng code:** tách lớp rõ ràng, comment tiếng Việt cho phần thuật toán, log bằng SLF4J, không để lộ stack trace ra client, không hard-code mật khẩu, xử lý mất kết nối Neo4j (thông báo rõ trên UI).
- **README:** cài Neo4j (Desktop/Docker), cách nạp `seed.cypher` thủ công trong Neo4j Browser, biến môi trường, lệnh chạy/test, bảng ca kiểm thử, giới hạn đã biết.

---

## 11. Quy trình làm việc & tiêu chí nghiệm thu

**Quy tắc xuất kết quả:** mỗi file ghi rõ **đường dẫn**, kèm **toàn bộ nội dung** (không viết tắt bằng "..."/"giữ nguyên phần còn lại"). Nếu dài, chia giai đoạn và dừng chờ "tiếp tục":

- **GĐ0** — Rà soát nhanh DB theo mục 4: chỉ nêu **vấn đề mới** (nếu có); nếu không có, sang GĐ1.
- **GĐ1** — `pom.xml`, cấu hình, lớp DB, `DataSeeder`, `PersonRepository`, `DataQualityService` + trang chất lượng dữ liệu.
- **GĐ2** — `engine/*`, `KinshipService`, `MarriageCheckService` + toàn bộ unit test.
- **GĐ3** — `Routes`, `GraphViewService`, `App.java`.
- **GĐ4** — Frontend (HTML/CSS/JS).
- **GĐ5** — `README.md`, hướng dẫn chạy và tự kiểm tra.

**Tiêu chí nghiệm thu (tự kiểm tra trước khi kết thúc):**

- [ ] `mvn clean test` xanh; `mvn compile exec:java` chạy được và tự seed khi DB rỗng.
- [ ] Trang chất lượng dữ liệu: 0 ERROR với dữ liệu hiện tại.
- [ ] T1–T23 và M1–M11 cho đúng kết quả; đổi vai A↔B luôn đối xứng.
- [ ] Ly hôn không bị tính khi mặc định; bật công tắc thì tính và ghi nhãn "(cũ)".
- [ ] Mọi kết quả đều có lời giải thích từng bước **và** đường đi được tô sáng trên đồ thị.
- [ ] Không có Cypher nối chuỗi; không lộ mật khẩu; hiển thị đúng tiếng Việt có dấu.
