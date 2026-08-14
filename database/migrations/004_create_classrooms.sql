CREATE TABLE IF NOT EXISTS classrooms (
  id INT AUTO_INCREMENT PRIMARY KEY,
  room_number VARCHAR(50) NOT NULL,
  building VARCHAR(100) NULL,
  floor INT NULL,
  capacity INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_classrooms_room_number UNIQUE (room_number),
  CONSTRAINT chk_classrooms_capacity_positive CHECK (capacity > 0)
);

CREATE INDEX idx_classrooms_capacity ON classrooms (capacity);
