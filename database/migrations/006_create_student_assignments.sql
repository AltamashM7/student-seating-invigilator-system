CREATE TABLE IF NOT EXISTS student_assignments (
  id INT AUTO_INCREMENT PRIMARY KEY,

  student_id INT NOT NULL,
  exam_id INT NOT NULL,
  classroom_id INT NOT NULL,

  seat_number VARCHAR(20) NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT uq_student_assignments_seat
    UNIQUE (exam_id, classroom_id, seat_number),

  CONSTRAINT uq_student_assignments_student_exam
    UNIQUE (student_id, exam_id),

  CONSTRAINT fk_student_assignments_student
    FOREIGN KEY (student_id)
    REFERENCES students(id),

  CONSTRAINT fk_student_assignments_exam
    FOREIGN KEY (exam_id)
    REFERENCES exams(id),

  CONSTRAINT fk_student_assignments_classroom
    FOREIGN KEY (classroom_id)
    REFERENCES classrooms(id)
);

CREATE INDEX idx_student_assignments_student
  ON student_assignments (student_id);

CREATE INDEX idx_student_assignments_exam
  ON student_assignments (exam_id);

CREATE INDEX idx_student_assignments_classroom
  ON student_assignments (classroom_id);

CREATE INDEX idx_student_assignments_exam_classroom
  ON student_assignments (exam_id, classroom_id);
