-- Apply once to an S1-S7 database. Existing academic and seating records are retained.
USE exam_seating;

ALTER TABLE users MODIFY password_hash VARCHAR(255) NOT NULL;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(80) PRIMARY KEY, applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS invigilation_rules (
    id INT PRIMARY KEY, rest_minutes INT NOT NULL DEFAULT 30,
    students_per_invigilator INT NOT NULL DEFAULT 30,
    avoid_own_department BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK(rest_minutes BETWEEN 0 AND 240), CHECK(students_per_invigilator BETWEEN 1 AND 500)
);
INSERT IGNORE INTO invigilation_rules VALUES (1,30,30,FALSE);
CREATE TABLE IF NOT EXISTS faculty_duty_settings (
    faculty_id INT PRIMARY KEY, available BOOLEAN NOT NULL DEFAULT TRUE,
    max_daily INT NOT NULL DEFAULT 2, max_total INT NOT NULL DEFAULT 20,
    FOREIGN KEY(faculty_id) REFERENCES faculty(id) ON DELETE CASCADE,
    CHECK(max_daily BETWEEN 1 AND 20), CHECK(max_total BETWEEN 1 AND 500)
);
CREATE TABLE IF NOT EXISTS faculty_unavailability (
    id INT PRIMARY KEY AUTO_INCREMENT, faculty_id INT NOT NULL,
    unavailable_date DATE NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL,
    reason VARCHAR(250) NOT NULL,
    FOREIGN KEY(faculty_id) REFERENCES faculty(id) ON DELETE CASCADE,
    CHECK(end_time > start_time), INDEX idx_unavailable(faculty_id,unavailable_date)
);
CREATE TABLE IF NOT EXISTS faculty_course_conflicts (
    faculty_id INT NOT NULL, course_id INT NOT NULL,
    PRIMARY KEY(faculty_id,course_id),
    FOREIGN KEY(faculty_id) REFERENCES faculty(id) ON DELETE CASCADE,
    FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS invigilation_assignments (
    id INT PRIMARY KEY AUTO_INCREMENT, examination_id INT NOT NULL, room_id INT NOT NULL,
    slot_number INT NOT NULL, faculty_id INT NOT NULL,
    status ENUM('PENDING','ACCEPTED','DECLINED') NOT NULL DEFAULT 'PENDING',
    is_manual BOOLEAN NOT NULL DEFAULT FALSE, version INT NOT NULL DEFAULT 1,
    reason VARCHAR(250) NOT NULL, response_note VARCHAR(250),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, responded_at TIMESTAMP NULL,
    UNIQUE(examination_id,room_id,slot_number), UNIQUE(examination_id,faculty_id),
    FOREIGN KEY(examination_id,room_id) REFERENCES examination_rooms(examination_id,room_id),
    FOREIGN KEY(faculty_id) REFERENCES faculty(id),
    CHECK(slot_number > 0), INDEX idx_duty_faculty(faculty_id,status)
);
CREATE TABLE IF NOT EXISTS invigilation_history (
    id INT PRIMARY KEY AUTO_INCREMENT, examination_id INT NOT NULL,
    room_id INT NOT NULL, slot_number INT NOT NULL,
    previous_faculty_id INT, new_faculty_id INT, action VARCHAR(30) NOT NULL,
    actor_user_id INT NOT NULL, reason VARCHAR(250) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(examination_id) REFERENCES examinations(id),
    FOREIGN KEY(room_id) REFERENCES rooms(id), FOREIGN KEY(actor_user_id) REFERENCES users(id),
    FOREIGN KEY(previous_faculty_id) REFERENCES faculty(id), FOREIGN KEY(new_faculty_id) REFERENCES faculty(id)
);
CREATE TABLE IF NOT EXISTS notifications (
    id INT PRIMARY KEY AUTO_INCREMENT, recipient_user_id INT,
    recipient_email VARCHAR(120), examination_id INT,
    title VARCHAR(160) NOT NULL, message TEXT NOT NULL,
    read_at TIMESTAMP NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    email_status ENUM('DISABLED','QUEUED','SENDING','SENT','FAILED') NOT NULL DEFAULT 'DISABLED',
    email_attempts INT NOT NULL DEFAULT 0, email_error VARCHAR(250),
    email_claimed_at TIMESTAMP NULL, email_sent_at TIMESTAMP NULL,
    FOREIGN KEY(recipient_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(examination_id) REFERENCES examinations(id) ON DELETE SET NULL,
    INDEX idx_inbox(recipient_user_id,created_at), INDEX idx_email_queue(email_status,id)
);
INSERT IGNORE INTO schema_migrations(version) VALUES ('002_complete_project');
