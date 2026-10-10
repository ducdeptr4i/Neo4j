// Tạo index fullName (tùy chọn, idempotent)
CREATE INDEX person_fullname IF NOT EXISTS FOR (p:Person) ON (p.fullName);
