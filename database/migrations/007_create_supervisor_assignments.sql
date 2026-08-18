CREATE TABLE IF NOT EXISTS supervisor_assignments (
  id INT AUTO_INCREMENT PRIMARY KEY,

  supervisor_id INT NOT NULL,
  exam_id INT NOT NULL,
  classroom_id INT NOT NULL,

  role VARCHAR(100) NOT NULL,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT uq_supervisor_assignments_supervisor_exam
    UNIQUE (supervisor_id, exam_id),

  CONSTRAINT fk_supervisor_assignments_supervisor
    FOREIGN KEY (supervisor_id)
    REFERENCES supervisors(id),

  CONSTRAINT fk_supervisor_assignments_exam
    FOREIGN KEY (exam_id)
    REFERENCES exams(id),

  CONSTRAINT fk_supervisor_assignments_classroom
    FOREIGN KEY (classroom_id)
    REFERENCES classrooms(id)
);

CREATE INDEX idx_supervisor_assignments_supervisor
  ON supervisor_assignments (supervisor_id);

CREATE INDEX idx_supervisor_assignments_exam
  ON supervisor_assignments (exam_id);

CREATE INDEX idx_supervisor_assignments_classroom
  ON supervisor_assignments (classroom_id);

CREATE INDEX idx_supervisor_assignments_exam_classroom
  ON supervisor_assignments (exam_id, classroom_id);
