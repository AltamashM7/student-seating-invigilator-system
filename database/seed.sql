USE exam_seating;

INSERT IGNORE INTO departments(code, name) VALUES
('CSE', 'Computer Science and Engineering'),
('IT', 'Information Technology'),
('ENTC', 'Electronics and Telecommunication');

INSERT IGNORE INTO users(username, password_hash, role)
VALUES ('admin', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', 'ADMIN');

INSERT IGNORE INTO admins(user_id, name, email)
SELECT id, 'System Admin', 'admin@example.com'
FROM users
WHERE username = 'admin';
