CREATE DATABASE IF NOT EXISTS exam_seating;
USE exam_seating;

CREATE TABLE IF NOT EXISTS departments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash CHAR(64) NOT NULL,
    role ENUM('ADMIN', 'FACULTY') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admins (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS faculty (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT UNIQUE,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    department_id INT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS students (
    id INT PRIMARY KEY AUTO_INCREMENT,
    roll_number VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) UNIQUE,
    department_id INT NOT NULL,
    semester INT NOT NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE IF NOT EXISTS rooms (
    id INT PRIMARY KEY AUTO_INCREMENT,
    room_number VARCHAR(30) NOT NULL UNIQUE,
    capacity INT NOT NULL
);

CREATE TABLE IF NOT EXISTS seats (
    id INT PRIMARY KEY AUTO_INCREMENT,
    room_id INT NOT NULL,
    seat_number VARCHAR(20) NOT NULL,
    UNIQUE (room_id, seat_number),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS courses (
    id INT PRIMARY KEY AUTO_INCREMENT,
    course_code VARCHAR(30) NOT NULL UNIQUE,
    course_name VARCHAR(120) NOT NULL,
    department_id INT NOT NULL,
    semester INT NOT NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE IF NOT EXISTS course_enrollments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    UNIQUE (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS examinations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    course_id INT NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status ENUM('DRAFT', 'SCHEDULED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(id)
);

CREATE TABLE IF NOT EXISTS examination_rooms (
    examination_id INT NOT NULL,
    room_id INT NOT NULL,
    PRIMARY KEY (examination_id, room_id),
    FOREIGN KEY (examination_id) REFERENCES examinations(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE INDEX idx_exam_time ON examinations(exam_date, start_time, end_time);


CREATE TABLE academic_years (id INT PRIMARY KEY AUTO_INCREMENT, label VARCHAR(9) NOT NULL UNIQUE, start_year INT NOT NULL UNIQUE, is_current BOOLEAN NOT NULL DEFAULT FALSE, CHECK(start_year BETWEEN 2000 AND 2100));
INSERT INTO academic_years(label,start_year,is_current) VALUES ('2026-27',2026,TRUE);
CREATE TABLE divisions (id INT PRIMARY KEY AUTO_INCREMENT, academic_year_id INT NOT NULL, department_id INT NOT NULL, study_year INT NOT NULL, code VARCHAR(20) NOT NULL, UNIQUE(academic_year_id,department_id,study_year,code), FOREIGN KEY(academic_year_id) REFERENCES academic_years(id), FOREIGN KEY(department_id) REFERENCES departments(id), CHECK(study_year BETWEEN 1 AND 4));
ALTER TABLE courses ADD study_year INT NOT NULL DEFAULT 1;
UPDATE courses SET study_year=CEIL(semester/2);
ALTER TABLE courses ADD CHECK(semester BETWEEN 1 AND 8 AND study_year=CEIL(semester/2));
CREATE TABLE course_offerings (course_id INT NOT NULL, academic_year_id INT NOT NULL, PRIMARY KEY(course_id,academic_year_id), FOREIGN KEY(course_id) REFERENCES courses(id), FOREIGN KEY(academic_year_id) REFERENCES academic_years(id));
INSERT INTO course_offerings SELECT id,1 FROM courses;
CREATE TABLE student_academic_enrollments (student_id INT NOT NULL, academic_year_id INT NOT NULL, division_id INT NOT NULL, semester INT NOT NULL, PRIMARY KEY(student_id,academic_year_id), FOREIGN KEY(student_id) REFERENCES students(id), FOREIGN KEY(academic_year_id) REFERENCES academic_years(id), FOREIGN KEY(division_id) REFERENCES divisions(id), CHECK(semester BETWEEN 1 AND 8));
INSERT INTO divisions(academic_year_id,department_id,study_year,code) SELECT DISTINCT 1,department_id,CEIL(semester/2),'A' FROM students;
INSERT INTO student_academic_enrollments SELECT s.id,1,d.id,s.semester FROM students s JOIN divisions d ON d.department_id=s.department_id AND d.study_year=CEIL(s.semester/2) AND d.academic_year_id=1;
ALTER TABLE students ADD requires_accessible BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE course_enrollments ADD academic_year_id INT NOT NULL DEFAULT 1;
-- Keep the legacy student foreign key supported while replacing the old two-column uniqueness.
ALTER TABLE course_enrollments ADD INDEX idx_enrollment_student(student_id);
ALTER TABLE course_enrollments DROP INDEX student_id, ADD UNIQUE(student_id,course_id,academic_year_id), ADD FOREIGN KEY(student_id,academic_year_id) REFERENCES student_academic_enrollments(student_id,academic_year_id), ADD FOREIGN KEY(course_id,academic_year_id) REFERENCES course_offerings(course_id,academic_year_id);
ALTER TABLE examinations ADD academic_year_id INT NOT NULL DEFAULT 1, ADD spacing_required BOOLEAN NOT NULL DEFAULT FALSE, ADD FOREIGN KEY(course_id,academic_year_id) REFERENCES course_offerings(course_id,academic_year_id);
ALTER TABLE rooms ADD seat_columns INT NOT NULL DEFAULT 6, ADD is_accessible BOOLEAN NOT NULL DEFAULT TRUE, ADD CHECK(capacity BETWEEN 1 AND 500 AND seat_columns BETWEEN 1 AND 30);
ALTER TABLE seats ADD seat_row INT NOT NULL DEFAULT 1, ADD seat_column INT NOT NULL DEFAULT 1, ADD is_accessible BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE seats SET seat_row=FLOOR((CAST(SUBSTRING(seat_number,2) AS UNSIGNED)-1)/6)+1, seat_column=MOD(CAST(SUBSTRING(seat_number,2) AS UNSIGNED)-1,6)+1, is_accessible=(CAST(SUBSTRING(seat_number,2) AS UNSIGNED)<=2);
CREATE TABLE seating_assignments (id INT PRIMARY KEY AUTO_INCREMENT, examination_id INT NOT NULL, student_id INT NOT NULL, seat_id INT NOT NULL, is_manual BOOLEAN NOT NULL DEFAULT FALSE, UNIQUE(examination_id,student_id), UNIQUE(examination_id,seat_id), FOREIGN KEY(examination_id) REFERENCES examinations(id), FOREIGN KEY(student_id) REFERENCES students(id), FOREIGN KEY(seat_id) REFERENCES seats(id));
CREATE TABLE seating_history (id INT PRIMARY KEY AUTO_INCREMENT, examination_id INT NOT NULL, student_id INT, action VARCHAR(30) NOT NULL, old_seat VARCHAR(100), new_seat VARCHAR(100), actor_user_id INT NOT NULL, reason VARCHAR(250) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(examination_id) REFERENCES examinations(id), FOREIGN KEY(student_id) REFERENCES students(id), FOREIGN KEY(actor_user_id) REFERENCES users(id));
CREATE TABLE planner_lock(id INT PRIMARY KEY);
INSERT INTO planner_lock VALUES(1);
