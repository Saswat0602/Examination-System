CREATE TABLE students (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(50) NOT NULL
);

CREATE TABLE admins (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(50) NOT NULL
);

CREATE TABLE exam_results (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  obtained_marks INT NOT NULL,
  total_marks INT NOT NULL,
  FOREIGN KEY (student_id) REFERENCES students(id)
);


CREATE TABLE options (
  option_id INT PRIMARY KEY AUTO_INCREMENT,
  question_id INT,
  option_text VARCHAR(255),
  is_correct BOOLEAN,
  FOREIGN KEY (question_id) REFERENCES questions(question_id)
);

CREATE TABLE questions (
  question_id INT PRIMARY KEY AUTO_INCREMENT,
  question_text VARCHAR(255),
  marks INT
);
