// ============================================================
// 1. TẠO CONSTRAINT
// ============================================================

CREATE CONSTRAINT person_id_unique IF NOT EXISTS
FOR (p:Person)
REQUIRE p.personId IS UNIQUE;


// ============================================================
// 2. TẠO 20 NGƯỜI
// ============================================================

UNWIND [
    {
        id: 'P001',
        name: 'Huỳnh Văn Hòa',
        gender: 'Nam',
        dob: '1938-02-14',
        job: 'Nông dân',
        dod: '2018-11-03'
    },
    {
        id: 'P002',
        name: 'Lê Thị Mai',
        gender: 'Nữ',
        dob: '1941-07-21',
        job: 'Giáo viên nghỉ hưu',
        dod: null
    },
    {
        id: 'P003',
        name: 'Huỳnh Văn Thành',
        gender: 'Nam',
        dob: '1963-05-10',
        job: 'Kỹ sư xây dựng',
        dod: null
    },
    {
        id: 'P004',
        name: 'Phạm Thị Hạnh',
        gender: 'Nữ',
        dob: '1965-09-22',
        job: 'Kế toán',
        dod: null
    },
    {
        id: 'P005',
        name: 'Huỳnh Văn Bình',
        gender: 'Nam',
        dob: '1974-12-03',
        job: 'Bác sĩ',
        dod: null
    },
    {
        id: 'P006',
        name: 'Trần Thị Thu',
        gender: 'Nữ',
        dob: '1975-04-18',
        job: 'Dược sĩ',
        dod: null
    },
    {
        id: 'P007',
        name: 'Huỳnh Thị Lan',
        gender: 'Nữ',
        dob: '1971-06-16',
        job: 'Giáo viên',
        dod: null
    },
    {
        id: 'P008',
        name: 'Võ Văn Quang',
        gender: 'Nam',
        dob: '1969-01-30',
        job: 'Kỹ thuật viên',
        dod: '2022-06-30'
    },
    {
        id: 'P009',
        name: 'Huỳnh Thị Hương',
        gender: 'Nữ',
        dob: '1977-08-09',
        job: 'Điều dưỡng',
        dod: null
    },
    {
        id: 'P010',
        name: 'Đặng Văn Phúc',
        gender: 'Nam',
        dob: '1975-01-30',
        job: 'Kiến trúc sư',
        dod: null
    },
    {
        id: 'P011',
        name: 'Huỳnh Minh Đức',
        gender: 'Nam',
        dob: '1988-03-15',
        job: 'Kỹ sư phần mềm',
        dod: null
    },
    {
        id: 'P012',
        name: 'Huỳnh Ngọc Anh',
        gender: 'Nữ',
        dob: '1992-10-02',
        job: 'Giảng viên',
        dod: null
    },
    {
        id: 'P013',
        name: 'Huỳnh Hoàng Nam',
        gender: 'Nam',
        dob: '1999-07-11',
        job: 'Nhân viên ngân hàng',
        dod: null
    },
    {
        id: 'P014',
        name: 'Huỳnh Thu Hà',
        gender: 'Nữ',
        dob: '2003-02-25',
        job: 'Thiết kế đồ họa',
        dod: null
    },
    {
        id: 'P015',
        name: 'Võ Minh Khang',
        gender: 'Nam',
        dob: '1996-06-05',
        job: 'Nhân viên marketing',
        dod: null
    },
    {
        id: 'P016',
        name: 'Đặng Thảo Vy',
        gender: 'Nữ',
        dob: '2001-09-17',
        job: 'Phân tích dữ liệu',
        dod: null
    },
    {
        id: 'P017',
        name: 'Đỗ Thị Ngọc',
        gender: 'Nữ',
        dob: '1990-12-08',
        job: 'Dược sĩ',
        dod: null
    },
    {
        id: 'P018',
        name: 'Lê Văn Sơn',
        gender: 'Nam',
        dob: '1990-01-19',
        job: 'Luật sư',
        dod: null
    },
    {
        id: 'P019',
        name: 'Huỳnh Gia Bảo',
        gender: 'Nam',
        dob: '2015-04-12',
        job: 'Học sinh',
        dod: null
    },
    {
        id: 'P020',
        name: 'Lê Minh Châu',
        gender: 'Nữ',
        dob: '2018-09-01',
        job: 'Học sinh',
        dod: null
    }
] AS person

MERGE (p:Person {personId: person.id})

SET
    p.fullName = person.name,
    p.gender = person.gender,
    p.dateOfBirth = date(person.dob),
    p.occupation = person.job,
    p.dateOfDeath =
        CASE
            WHEN person.dod IS NULL THEN null
            ELSE date(person.dod)
        END;


// ============================================================
// 3. TẠO QUAN HỆ CHA/MẸ RUỘT -> CON
// ============================================================

UNWIND [

    // --------------------------------------------------------
    // THẾ HỆ 1 -> THẾ HỆ 2
    // Huỳnh Văn Hòa + Lê Thị Mai
    // có 4 người con:
    // Thành, Lan, Bình, Hương
    // --------------------------------------------------------

    {
        parent: 'P001',
        child: 'P003',
        since: '1963-05-10'
    },
    {
        parent: 'P002',
        child: 'P003',
        since: '1963-05-10'
    },

    {
        parent: 'P001',
        child: 'P007',
        since: '1971-06-16'
    },
    {
        parent: 'P002',
        child: 'P007',
        since: '1971-06-16'
    },

    {
        parent: 'P001',
        child: 'P005',
        since: '1974-12-03'
    },
    {
        parent: 'P002',
        child: 'P005',
        since: '1974-12-03'
    },

    {
        parent: 'P001',
        child: 'P009',
        since: '1977-08-09'
    },
    {
        parent: 'P002',
        child: 'P009',
        since: '1977-08-09'
    },


    // --------------------------------------------------------
    // Huỳnh Văn Thành + Phạm Thị Hạnh
    // -> Huỳnh Minh Đức, Huỳnh Ngọc Anh
    // --------------------------------------------------------

    {
        parent: 'P003',
        child: 'P011',
        since: '1988-03-15'
    },
    {
        parent: 'P004',
        child: 'P011',
        since: '1988-03-15'
    },

    {
        parent: 'P003',
        child: 'P012',
        since: '1992-10-02'
    },
    {
        parent: 'P004',
        child: 'P012',
        since: '1992-10-02'
    },


    // --------------------------------------------------------
    // Huỳnh Văn Bình + Trần Thị Thu
    // -> Huỳnh Hoàng Nam, Huỳnh Thu Hà
    // --------------------------------------------------------

    {
        parent: 'P005',
        child: 'P013',
        since: '1999-07-11'
    },
    {
        parent: 'P006',
        child: 'P013',
        since: '1999-07-11'
    },

    {
        parent: 'P005',
        child: 'P014',
        since: '2003-02-25'
    },
    {
        parent: 'P006',
        child: 'P014',
        since: '2003-02-25'
    },


    // --------------------------------------------------------
    // Huỳnh Thị Lan + Võ Văn Quang
    // -> Huỳnh Minh Khang
    // --------------------------------------------------------

    {
        parent: 'P007',
        child: 'P015',
        since: '1996-06-05'
    },
    {
        parent: 'P008',
        child: 'P015',
        since: '1996-06-05'
    },


    // --------------------------------------------------------
    // Huỳnh Thị Hương + Đặng Văn Phúc
    // -> Đặng Thảo Vy
    // --------------------------------------------------------

    {
        parent: 'P009',
        child: 'P016',
        since: '2001-09-17'
    },
    {
        parent: 'P010',
        child: 'P016',
        since: '2001-09-17'
    },


    // --------------------------------------------------------
    // THẾ HỆ 3 -> THẾ HỆ 4
    // Huỳnh Minh Đức + Đỗ Thị Ngọc
    // -> Huỳnh Gia Bảo
    // --------------------------------------------------------

    {
        parent: 'P011',
        child: 'P019',
        since: '2015-04-12'
    },
    {
        parent: 'P017',
        child: 'P019',
        since: '2015-04-12'
    },


    // --------------------------------------------------------
    // Huỳnh Ngọc Anh + Lê Văn Sơn
    // -> Huỳnh Minh Châu
    // --------------------------------------------------------

    {
        parent: 'P012',
        child: 'P020',
        since: '2018-09-01'
    },
    {
        parent: 'P018',
        child: 'P020',
        since: '2018-09-01'
    }

] AS rel

MATCH (parent:Person {personId: rel.parent})
MATCH (child:Person {personId: rel.child})

MERGE (parent)-[r:BIOLOGICAL_PARENT_OF]->(child)

SET r.since = date(rel.since);


// ============================================================
// 4. TẠO QUAN HỆ HÔN NHÂN
// ============================================================

UNWIND [

    // Huỳnh Văn Hòa - Lê Thị Mai
    // Kết thúc hôn nhân do ông Hòa mất
    {
        person1: 'P001',
        person2: 'P002',
        startDate: '1961-01-15',
        endDate: '2018-11-03',
        endReason: 'DEATH'
    },


    // Huỳnh Văn Thành - Phạm Thị Hạnh
    // Hiện vẫn là vợ chồng
    {
        person1: 'P003',
        person2: 'P004',
        startDate: '1986-12-20',
        endDate: null,
        endReason: null
    },


    // Huỳnh Văn Bình - Trần Thị Thu
    // Đã ly hôn
    {
        person1: 'P005',
        person2: 'P006',
        startDate: '1997-03-16',
        endDate: '2008-08-15',
        endReason: 'DIVORCE'
    },


    // Huỳnh Thị Lan - Võ Văn Quang
    // Kết thúc do ông Quang mất
    {
        person1: 'P007',
        person2: 'P008',
        startDate: '1994-01-25',
        endDate: '2022-06-30',
        endReason: 'DEATH'
    },


    // Huỳnh Thị Hương - Đặng Văn Phúc
    {
        person1: 'P009',
        person2: 'P010',
        startDate: '1999-04-18',
        endDate: null,
        endReason: null
    },


    // Huỳnh Minh Đức - Đỗ Thị Ngọc
    {
        person1: 'P011',
        person2: 'P017',
        startDate: '2012-11-10',
        endDate: null,
        endReason: null
    },


    // Huỳnh Ngọc Anh - Lê Văn Sơn
    {
        person1: 'P012',
        person2: 'P018',
        startDate: '2015-05-24',
        endDate: null,
        endReason: null
    }

] AS marriage

MATCH (a:Person {personId: marriage.person1})
MATCH (b:Person {personId: marriage.person2})

MERGE (a)-[r:MARRIED_TO]->(b)

SET
    r.startDate = date(marriage.startDate),

    r.endDate =
        CASE
            WHEN marriage.endDate IS NULL THEN null
            ELSE date(marriage.endDate)
        END,

    r.endReason = marriage.endReason;


// ============================================================
// 5. HIỂN THỊ TOÀN BỘ CÂY GIA ĐÌNH SAU KHI TẠO
// ============================================================

MATCH p=(:Person)-[r]-(:Person)
RETURN p;