# Quản lý Nhân thân & Gia phả (Neo4j + Java Javalin)

Ứng dụng web quản lý dữ liệu nhân thân và gia phả 20 người được lưu trữ trong cơ sở dữ liệu đồ thị **Neo4j**, viết bằng **Java 17 (Maven)**, backend nhẹ **Javalin 6**, và frontend trực quan tương tác với **Bootstrap 5.3** và **vis-network**.

---

## 1. Tính năng chính

1. **Sơ đồ gia phả tương tác (Interactive Family Tree)**:
   - Đồ thị trực quan phân cấp theo thế hệ (`generation`), điều hướng mượt mà (zoom, pan, fit).
   - Nút người phân màu chuẩn: Nam (Xanh dương), Nữ (Hồng), Đã mất (Xám kèm biểu tượng ✝).
   - Đường nối: Quan hệ cha mẹ – con (mũi tên đặc), Hôn nhân còn hiệu lực (xanh lá), Góa (xám), Đã ly hôn (nét đứt đỏ).
   - Bảng thông tin chi tiết người (cha mẹ, vợ/chồng, con cái, anh chị em) với khả năng nhấp để chuyển nhanh qua lại.
2. **Tìm cách xưng hô giữa 2 người (Kinship Resolver)**:
   - Xác định quan hệ: Vợ chồng trực tiếp, Huyết thống (qua tổ tiên chung gần nhất - LCA), Thông gia (In-law qua hôn nhân).
   - Hiển thị cả 2 chiều: A gọi B là gì, B gọi A là gì; cách tự xưng (`selfRefer`).
   - Lời giải thích từng bước logic bằng tiếng Việt.
   - Sơ đồ thu nhỏ tô sáng đường dẫn kết nối giữa 2 người.
   - Hỗ trợ tuỳ chọn vùng miền (`COMMON` phổ thông, `NORTH` miền Bắc) và tuỳ chọn tính cả hôn nhân đã ly hôn.
   - Bộ ví dụ nhanh nạp sẵn các ca chuẩn **T1–T23**.
3. **Kiểm tra kết hôn theo luật định (Marriage Blood Checker)**:
   - Căn cứ pháp lý: **Luật Hôn nhân và Gia đình Việt Nam 2014** (Điều 5 khoản 2 điểm d; Điều 3).
   - Xác định: Cùng dòng máu về trực hệ (mọi đời) hoặc trong phạm vi ba đời ❌ **VI PHẠM**.
   - Ngoài ba đời hoặc không có huyết thống ✅ **KHÔNG VI PHẠM**.
   - Khối cảnh báo bổ sung riêng biệt: Cùng giới tính, chưa đủ tuổi kết hôn, một người đã mất, đang có hôn nhân với người khác, cấm kết hôn thông gia (cha/mẹ chồng - con dâu, mẹ vợ - con rể...), lệch thế hệ.
   - Sơ đồ tô sáng đường huyết thống và bộ ví dụ nhanh **M1–M11**.
4. **Danh sách người (Persons Directory)**:
   - Danh sách 20 người với chức năng tìm kiếm thời gian thực (theo tên, ID, nghề nghiệp), lọc theo giới tính, lọc trạng thái còn sống / đã mất, sắp xếp cột.
5. **Kiểm tra chất lượng dữ liệu (Data Quality Dashboard)**:
   - 12 tiêu chí kiểm toán tự động (Q1–Q11), kiểm tra tính toàn vẹn của đồ thị (vòng lặp tổ tiên, tuổi cha mẹ - con, tính hợp lệ của hôn nhân, v.v.).

---

## 2. Yêu cầu hệ thống & Cài đặt Neo4j

### Yêu cầu
- **Java**: 17 trở lên (đã kiểm thử tương thích Java 17 và Java 21).
- **Maven**: 3.6+ (có sẵn wrapper `mvnw` / `mvnw.cmd` và `mvn.cmd`).
- **Neo4j**: 5.x (hỗ trợ hàm `elementId()`).

### Cài đặt Neo4j nhanh bằng Docker
```bash
docker run -d \
  --name neo4j-nhanthan \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password123 \
  neo4j:5-community
```
*Truy cập Neo4j Browser tại: [http://localhost:7474](http://localhost:7474)*

Hoặc dùng **Neo4j Desktop** tạo Database 5.x với user `neo4j` và mật khẩu tùy ý.

---

## 3. Cấu hình biến môi trường

Ứng dụng đọc cấu hình từ file `src/main/resources/config.properties` và ưu tiên ghi đè từ các biến môi trường:

| Biến môi trường | Mặc định | Mô tả |
|---|---|---|
| `NEO4J_URI` | `bolt://localhost:7687` | URI kết nối Neo4j Bolt |
| `NEO4J_USER` | `neo4j` | Tên đăng nhập |
| `NEO4J_PASSWORD` | *(không đặt)* | Mật khẩu Neo4j (**bắt buộc thiết lập**) |
| `NEO4J_DATABASE` | `neo4j` | Tên database |
| `PORT` | `8080` | Cổng HTTP của Web Server |
| `APP_SEED_IF_EMPTY` | `true` | Tự động nạp `seed.cypher` khi DB chưa có dữ liệu |

### Thiết lập biến môi trường trên Windows (PowerShell)
```powershell
$env:NEO4J_PASSWORD="password123"
```

### Thiết lập trên Windows (CMD)
```cmd
set NEO4J_PASSWORD=password123
```

---

## 4. Chạy kiểm thử & Khởi động ứng dụng

### Chạy Unit Test (40 tests, không cần Neo4j)
Toàn bộ logic thuật toán xưng hô, kiểm tra kết hôn và chất lượng dữ liệu được kiểm thử bằng đồ thị trong bộ nhớ (`FamilyGraph`), đạt 100% độ phủ cho các ca T1–T23 và M1–M11:
```cmd
mvn test
# hoặc dùng wrapper:
./mvnw test
```

### Khởi động ứng dụng Web
```cmd
mvn compile exec:java
# hoặc:
./mvnw compile exec:java
```

Khi khởi động thành công, mở trình duyệt tại:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 5. Bảng ca kiểm thử chuẩn

### Chức năng 1: Tìm xưng hô (T1–T23)
| Mã | Người A | Người B | A gọi B | B gọi A |
|---|---|---|---|---|
| T1 | P011 Đức | P003 Thành | ba (cha) | con |
| T2 | P011 Đức | P002 Mai | bà nội | cháu |
| T3 | P019 Bảo | P001 Hòa | cụ ông | chắt |
| T4 | P011 Đức | P012 Ngọc Anh | em (em gái) | anh |
| T5 | P011 Đức | P005 Bình | chú | cháu |
| T6 | P011 Đức | P007 Lan | cô | cháu |
| T7 | P013 Nam | P011 Đức | anh họ (con bác) | em họ (con chú) |
| T8 | P013 Nam | P015 Khang | anh họ (con cô) | em họ (con cậu) |
| T9 | P012 Ngọc Anh | P016 Vy | em họ (con cô) | chị họ (con cậu) |
| T10 | P020 Châu | P019 Bảo | anh họ (con cậu) | em họ (con cô) |
| T11 | P019 Bảo | P005 Bình | ông chú | cháu |
| T12 | P019 Bảo | P007 Lan | bà cô | cháu |
| T13 | P019 Bảo | P014 Hà | cô họ | cháu họ |
| T14 | P004 Hạnh | P001 Hòa | bố chồng | con dâu |
| T15 | P018 Sơn | P004 Hạnh | mẹ vợ | con rể |
| T16 | P004 Hạnh | P007 Lan | em chồng (cô) | chị dâu |
| T17 | P017 Ngọc | P012 Ngọc Anh | em chồng | chị dâu |
| T18 | P003 Thành | P010 Phúc | em rể | anh vợ |
| T19 | P010 Phúc | P008 Quang | anh em đồng hao (cột chèo) | (đối xứng) |
| T20 | P005 Bình | P006 Thu | Đã ly hôn (2008-08-15) | (đối xứng) |
| T21 | P006 Thu | P003 Thành | Không quan hệ (bật ly hôn: anh chồng (cũ)) | (em dâu (cũ)) |
| T22 | P004 Hạnh | P006 Thu | Không quan hệ (bật ly hôn: chị em dâu (cũ)) | (đối xứng) |
| T23 | P004 Hạnh | P002 Mai | mẹ chồng | con dâu |

### Chức năng 2: Kiểm tra kết hôn (M1–M11)
| Mã | Người A | Người B | Kết luận | Lý do |
|---|---|---|---|---|
| M1 | P003 Thành | P012 Ngọc Anh | ❌ VI PHẠM | Trực hệ, cha–con |
| M2 | P001 Hòa | P011 Đức | ❌ VI PHẠM | Trực hệ, ông–cháu |
| M3 | P011 Đức | P012 Ngọc Anh | ❌ VI PHẠM | Anh chị em ruột (đời 2) |
| M4 | P005 Bình | P012 Ngọc Anh | ❌ VI PHẠM | Chú–cháu ruột (đời 3) |
| M5 | P011 Đức | P016 Vy | ❌ VI PHẠM | Anh chị em họ (đời 3) |
| M6 | P015 Khang | P012 Ngọc Anh | ❌ VI PHẠM | Anh chị em họ (đời 3) |
| M7 | P019 Bảo | P020 Châu | ❌ VI PHẠM | Anh chị em họ (đời 3) |
| M8 | P014 Hà | P019 Bảo | ✅ KHÔNG VI PHẠM | Đời 4; cảnh báo chưa đủ tuổi kết hôn & lệch thế hệ |
| M9 | P004 Hạnh | P018 Sơn | ✅ KHÔNG VI PHẠM | Không huyết thống; cảnh báo mẹ vợ – con rể |
| M10 | P015 Khang | P017 Ngọc | ✅ KHÔNG VI PHẠM | Không huyết thống; cảnh báo đang có hôn nhân |
| M11 | P002 Mai | P018 Sơn | ✅ KHÔNG VI PHẠM | Cùng họ Lê nhưng không có quan hệ huyết thống trong DB |

---

## 6. Giới hạn đã biết
1. Dữ liệu hệ thống hiện gồm 20 người. Một số người kết hôn vào (P004, P006, P008, P010, P017, P018) không có thông tin về cha mẹ/anh chị em trong DB. Kết luận "Không phát hiện quan hệ huyết thống" được nêu rõ là **trong phạm vi dữ liệu hiện có**.
2. P002 (*Lê Thị Mai*) và P018 (*Lê Văn Sơn*) cùng họ Lê nhưng không có cạnh kết nối huyết thống trong CSDL nên được coi là trùng họ ngẫu nhiên.
