import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
// import java.util.List;
// import java.util.ArrayList;
// import java.util.Scanner;
// import java.util.InputMismatchException;

// Option class represents an option for a question
class Option {
    private String text;
    private boolean isCorrect;

    public Option(String text, boolean isCorrect) {
        this.text = text;
        this.isCorrect = isCorrect;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return isCorrect;
    }
}

// Question class represents a single question with its options
class Question {
    private int id;
    private String text;
    private List<Option> options;
    private int marks;
    private int obtainedMarks = -1;

    public Question(int id, String text, int marks) {
        this.id = id;
        this.text = text;
        this.marks = marks;
        this.options = new ArrayList<>();
    }

    public void addOption(String text, boolean isCorrect) {
        Option option = new Option(text, isCorrect);
        options.add(option);
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public int getMarks() {
        return marks;
    }

    public List<Option> getOptions() {
        return options;
    }

    public int getObtainedMarks() {
        return obtainedMarks;
    }

    public void setObtainedMarks(int obtainedMarks) {
        this.obtainedMarks = obtainedMarks;
    }
}

// Exam class represents a collection of questions
class Exam {
    private List<Question> questions = new ArrayList<>();
    private String studentName;
    private int totalMarks;
    private int obtainedMarks;

    public void addQuestion(Question question) {
        questions.add(question);
    }

    private static Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/examination";
        String username = "root";
        String password = "Saswat@0602";

        return DriverManager.getConnection(url, username, password);
    }

    // Method to start the exam and prompt for answers
    public void startExam(int duration) {
        Scanner sc = new Scanner(System.in);
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        System.out.print("Enter your name: ");
        studentName = sc.nextLine();

        // USING THREAD FOR TIMER FUNCTIONS
        Runnable timerTask = () -> {
            try {
                TimeUnit.MINUTES.sleep(duration);
                System.out.println("\nTime's up! Your exam will be automatically submitted.");
                submitExam();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        executorService.submit(timerTask);

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            System.out.println("\nQuestion " + (i + 1) + ": " + question.getText());
            List<Option> options = question.getOptions();
            for (int j = 0; j < options.size(); j++) {
                Option option = options.get(j);
                System.out.println((j + 1) + ") " + option.getText());
            }
            int answer = getValidatedInput("Enter your answer (1-" + options.size() + "), or enter 0 to skip: ", 0,
                    options.size());
            if (answer == 0) {
                continue; // Skip the question
            }
            question.setObtainedMarks(options.get(answer - 1).isCorrect() ? question.getMarks() : 0);
        }
//The shutdown() method will allow previously submitted tasks to execute before terminating, while the  shutdownNow() method prevents waiting tasks from starting and attempts to stop currently executing tasks.
        executorService.shutdown();
        submitExam();
    }

    private int getValidatedInput(String message, int min, int max) {
        Scanner sc = new Scanner(System.in);
        int answer;
        while (true) {
            System.out.print(message);
            try {
                answer = sc.nextInt();
                if (answer >= min && answer <= max) {
                    break;
                } else {
                    System.out.println("Invalid input! Please enter a valid option.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input! Please enter a valid option.");
                sc.nextLine(); // Clear the input buffer
            }
        } 
        return answer;
    }

    //give unique id to student or retrive student using id

    private int retrieveOrAssignStudentId() {
        int studentId = -1; // Default value for student ID

        // Check if the student already exists in the database
        try (Connection connection = getConnection()) {
            String selectQuery = "SELECT student_id FROM students WHERE username = ?";
            PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
            selectStmt.setString(1, studentName);
            ResultSet resultSet = selectStmt.executeQuery();

            if (resultSet.next()) {
                // Student exists, retrieve the student ID
                studentId = resultSet.getInt("student_id");
            } else {
                // Student doesn't exist, assign a new student ID
                String insertQuery = "INSERT INTO students (student_name) VALUES (?)";
                PreparedStatement insertStmt = connection.prepareStatement(insertQuery,
                        Statement.RETURN_GENERATED_KEYS);
                insertStmt.setString(1, studentName);
                insertStmt.executeUpdate();

                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    studentId = generatedKeys.getInt(1); 
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return studentId;
    }

    private void submitExam() {
        // Calculate total marks and obtained marks
        totalMarks = getTotalMarks();
        obtainedMarks = getObtainedMarks();

        // Store the exam results in the database
        try (Connection connection = getConnection()) {

            // Retrieve or assign studentId before inserting the exam results
            int studentId = retrieveOrAssignStudentId();

            // Insert the exam results into the database (exam_results )


            String insertMarksQuery = "INSERT INTO exam_results (student_id, student_name, obtained_marks, total_marks) VALUES (?, ?, ?, ?)";

            PreparedStatement insertMarksStmt = connection.prepareStatement(insertMarksQuery);
            insertMarksStmt.setInt(1, studentId);
            insertMarksStmt.setString(2, studentName);
            insertMarksStmt.setInt(3, obtainedMarks);
            insertMarksStmt.setInt(4, totalMarks);
            insertMarksStmt.executeUpdate();

        } catch (SQLException ex) {
            Exception suppressedException = new Exception("Suppressed Exception");
            ex.addSuppressed(suppressedException);
            ex.printStackTrace();

        }

        // Display exam results
        System.out.println("Exam submitted");
        System.out.println("You Can Exit now");
        System.out.println("************************");
        // System.out.println("\nTotal marks: " + totalMarks);
        // System.out.println("Obtained marks: " + obtainedMarks);
        // System.out.println("Percentage: " + ((double) obtainedMarks / totalMarks) *
        // 100 + "%");
    }

    public int getTotalMarks() {
        int total = 0;
        for (Question question : questions) {
            total += question.getMarks();
        }
        return total;
    }

    public int getObtainedMarks() {
        int total = 0;
        for (Question question : questions) {
            if (question.getObtainedMarks() != -1) {
                total += question.getObtainedMarks();
            }
        }
        return total;
    }
}

// ExaminationSystem class contains the main method to start the program

public class ExaminationSystem {
    public static void main(String[] args) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/examination";
        String username = "root";
        String password = "Saswat@0602";
        Scanner sc = new Scanner(System.in);
        System.out.println("==================================================");
        System.out.println("==================================================");
        System.out.println("=== Welcome to the Examination System! ===");
        System.out.println("*********************************************");

        System.out.print("Are you a student or an admin? (student/admin): ");
        String userType = sc.nextLine();

        if (userType.equalsIgnoreCase("student")) {
            // Student login or signup
            System.out.println("=== Student Login/Signup ===");
            System.out.print("Do you have a student account? (y/n): ");
            String haveAccount = sc.nextLine();

            if (haveAccount.equalsIgnoreCase("y")) {
                // Student login
                boolean studentLoggedIn = false;
                while (!studentLoggedIn) {
                    System.out.println("=== Student Login ===");
                    System.out.print("Enter student username: ");
                    String studentUsername = sc.nextLine();
                    System.out.print("Enter student password: ");
                    String studentPassword = sc.nextLine();

                    int studentId = studentLogin(studentUsername, studentPassword);

                    if (studentId != -1) {
                        studentLoggedIn = true;
                        System.out.println("Student login successful.\n");

                        Exam exam = new Exam();

                        // Retrieve questions and options from the database
                        try (Connection connection = getConnection()) {
                            // Retrieve questions
                            String selectQuestionsQuery = "SELECT * FROM questions";
                            PreparedStatement selectQuestionsStmt = connection.prepareStatement(selectQuestionsQuery);
                            ResultSet questionResultSet = selectQuestionsStmt.executeQuery();
                            while (questionResultSet.next()) {
                                int questionId = questionResultSet.getInt("id");
                                String questionText = questionResultSet.getString("text");
                                int marks = questionResultSet.getInt("marks");
                                Question question = new Question(questionId, questionText, marks);

                                // Retrieve options for each question
                                String selectOptionsQuery = "SELECT * FROM options WHERE question_id = ?";
                                PreparedStatement selectOptionsStmt = connection.prepareStatement(selectOptionsQuery);
                                selectOptionsStmt.setInt(1, questionId);
                                ResultSet optionResultSet = selectOptionsStmt.executeQuery();
                                //check the correct answer
                                while (optionResultSet.next()) {
                                    String optionText = optionResultSet.getString("text");
                                    boolean isCorrect = optionResultSet.getBoolean("is_correct");
                                    question.addOption(optionText, isCorrect);
                                }
                                exam.addQuestion(question);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        // Start the exam with a duration of 5 minutes
                        exam.startExam(5);
                    } else {
                        System.out.println("Invalid student credentials. Please try again.");
                    }
                }
            } else if (haveAccount.equalsIgnoreCase("n")) {
                // Student signup
                System.out.println("=================================");
                System.out.println("=== Student Signup ===");
                System.out.print("Create a new student username: ");
                String studentUsername = sc.nextLine();

                // Check if student username is empty
                while (studentUsername.isEmpty()) {
                    System.out.println("Username cannot be empty. Please try again.");
                    System.out.print("Create a new student username: ");
                    studentUsername = sc.nextLine();
                }

                System.out.print("Create a new student password: ");
                String studentPassword = sc.nextLine();

                // Check if student password is empty
                while (studentPassword.isEmpty()) {
                    System.out.println("Password cannot be empty. Please try again.");
                    System.out.print("Create a new student password: ");
                    studentPassword = sc.nextLine();
                }

                if (studentSignup(studentUsername, studentPassword)) {
                    System.out.println("Student signup successful. Please proceed with student login.");

                    // Student login
                    System.out.println("=== Student Login ===");
                    System.out.print("Enter student username: ");
                    String loginUsername = sc.nextLine();
                    System.out.print("Enter student password: ");
                    String loginPassword = sc.nextLine();

                    int studentId = studentLogin(loginUsername, loginPassword);

                    if (studentId != -1) {
                        System.out.println("Student login successful.\n");

                        // start the exam after successfull login

                        Exam exam = new Exam();

                        // Retrieve questions and options from the database
                        try (Connection connection = getConnection()) {
                            // Retrieve questions
                            String selectQuestionsQuery = "SELECT * FROM questions";
                            PreparedStatement selectQuestionsStmt = connection.prepareStatement(selectQuestionsQuery);
                            ResultSet questionResultSet = selectQuestionsStmt.executeQuery();
                            while (questionResultSet.next()) {
                                int questionId = questionResultSet.getInt("id");
                                String questionText = questionResultSet.getString("text");
                                int marks = questionResultSet.getInt("marks");
                                Question question = new Question(questionId, questionText, marks);

                                // Retrieve options for each question
                                String selectOptionsQuery = "SELECT * FROM options WHERE question_id = ?";
                                PreparedStatement selectOptionsStmt = connection.prepareStatement(selectOptionsQuery);
                                selectOptionsStmt.setInt(1, questionId);
                                ResultSet optionResultSet = selectOptionsStmt.executeQuery();
                                while (optionResultSet.next()) {
                                    String optionText = optionResultSet.getString("text");
                                    boolean isCorrect = optionResultSet.getBoolean("is_correct");
                                    question.addOption(optionText, isCorrect);
                                }

                                exam.addQuestion(question);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        // Start the exam with a duration of 5 minutes
                        exam.startExam(5);

                        // ...

                    } else {
                        System.out.println("Invalid student credentials. Exiting the program.");
                    }
                } else {
                    System.out.println("Student signup failed. Exiting the program.");
                }
            }

        }

        else if (userType.equalsIgnoreCase("admin")) {
            // Admin login

            System.out.println("=================================");

            System.out.println("=== Admin Login ===");

            boolean adminLoggedIn = false;
            while (!adminLoggedIn) {
                System.out.print("Enter admin username: ");
                String adminUsername = sc.nextLine();
                System.out.print("Enter admin password: ");
                String adminPassword = sc.nextLine();

                if (adminLogin(adminUsername, adminPassword)) {
                    adminLoggedIn = true;

                    Connection connection = DriverManager.getConnection(url, username, password);
                    System.out.println("Admin login successful.\n");

                    while (true) {
                        System.out.println("====== Admin Menu =======");
                        System.out.println("1. Add Questions");
                        System.out.println("2. Display Student Marks");
                        System.out.println("3. Logout");
                        System.out.print("Enter your choice: ");
                        
                        int choice = sc.nextInt();
                        System.out.println("==========================");
                        sc.nextLine(); // Clear the input buffer

                        switch (choice) {
                            case 1:
                                addQuestions(connection);
                                break;
                            case 2:
                                displayAllStudentMarks();
                                break;
                            case 3:
                                System.out.println("Admin logged out. Exiting the program.");
                                System.out.println("**************************************");

                                return;
                            default:
                                System.out.println("Invalid choice! Please enter a valid option.");
                        }
                    }
                } else {
                    System.out.println("Invalid admin credentials. Please try again.");
                }
            }
        } else {
            System.out.println("Invalid input. Exiting the program.");
        }

    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/examination", "root", "Saswat@0602");
    }
//logic for retireve student login like user name and password from data base
    private static int studentLogin(String username, String password) {
        try (Connection connection = getConnection()) {
            String selectStudentQuery = "SELECT id FROM students WHERE username = ? AND password = ?";
            PreparedStatement selectStudentStmt = connection.prepareStatement(selectStudentQuery);
            selectStudentStmt.setString(1, username);
            selectStudentStmt.setString(2, password);
            ResultSet studentResultSet = selectStudentStmt.executeQuery();

            if (studentResultSet.next()) {
                return studentResultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
//logic for store student login like user name and password from data base

    private static boolean studentSignup(String username, String password) {
        try (Connection connection = getConnection()) {
            String insertStudentQuery = "INSERT INTO students (username, password) VALUES (?, ?)";
            PreparedStatement insertStudentStmt = connection.prepareStatement(insertStudentQuery);
            insertStudentStmt.setString(1, username);
            insertStudentStmt.setString(2, password);
            int rowsAffected = insertStudentStmt.executeUpdate();

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
//logic for match admin crednetials from data base

    private static boolean adminLogin(String username, String password) {
        try (Connection connection = getConnection()) {
            String selectAdminQuery = "SELECT * FROM admins WHERE username = ? AND password = ?";
            PreparedStatement selectAdminStmt = connection.prepareStatement(selectAdminQuery);
            selectAdminStmt.setString(1, username);
            selectAdminStmt.setString(2, password);
            ResultSet adminResultSet = selectAdminStmt.executeQuery();

            return adminResultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

// addd question and mark to question table and option and correct option in  optaion table data base by admin 

private static void addQuestions(Connection connection) {
    try {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== Add Questions ===");

        System.out.print("Enter the question text: ");
        String questionText = sc.nextLine();

        int marks;
        while (true) {
            System.out.print("Enter the marks for this question: ");
            String marksInput = sc.nextLine();
            try {
                marks = Integer.parseInt(marksInput);
                break; // If the input is a valid integer, exit the loop
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number for marks.");
            }
        }

        // Insert the question into the database
        String insertQuestionQuery = "INSERT INTO questions (text, marks) VALUES (?, ?)";
        PreparedStatement insertQuestionStmt = connection.prepareStatement(insertQuestionQuery,
                Statement.RETURN_GENERATED_KEYS);
        insertQuestionStmt.setString(1, questionText);
        insertQuestionStmt.setInt(2, marks);
        insertQuestionStmt.executeUpdate();

        ResultSet generatedKeys = insertQuestionStmt.getGeneratedKeys();
        int questionId;
        if (generatedKeys.next()) {
            questionId = generatedKeys.getInt(1);

            // Add options for the question
            while (true) {
                System.out.print("Enter option text (or 'exit' to finish adding options): ");
                String optionText = sc.nextLine();
                if (optionText.equalsIgnoreCase("exit")) {
                    break;
                }

                boolean isCorrect;
                while (true) {
                    System.out.print("Is this option correct? (true/false): ");
                    String isCorrectInput = sc.nextLine();
                    if (isCorrectInput.equalsIgnoreCase("true") || isCorrectInput.equalsIgnoreCase("false")) {
                        isCorrect = Boolean.parseBoolean(isCorrectInput);
                        break;
                    } else {
                        System.out.println("Invalid input. Please enter 'true' or 'false' for option correctness.");
                    }
                }

                // Insert the option into the database
                String insertOptionQuery = "INSERT INTO options (question_id, text, is_correct) VALUES (?, ?, ?)";
                PreparedStatement insertOptionStmt = connection.prepareStatement(insertOptionQuery);
                insertOptionStmt.setInt(1, questionId);
                insertOptionStmt.setString(2, optionText);
                insertOptionStmt.setBoolean(3, isCorrect);
                insertOptionStmt.executeUpdate();
            }

            System.out.println("Question added successfully!"); 
        } else {
            System.out.println("Failed to add the question.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}



    private static void displayAllStudentMarks() {
        try (Connection connection = getConnection()) {
            String selectMarksQuery = "SELECT * FROM exam_results";
            PreparedStatement selectMarksStmt = connection.prepareStatement(selectMarksQuery);
            ResultSet marksResultSet = selectMarksStmt.executeQuery();
            System.out.println("=================================");
            System.out.println("======== Student Marks ============");
            System.out.println("=================================");

            while (marksResultSet.next()) {
                int studentId = marksResultSet.getInt("student_id");
                String studentName = marksResultSet.getString("student_name");
                int obtainedMarks = marksResultSet.getInt("obtained_marks");
                int totalMarks = marksResultSet.getInt("total_marks");

                System.out.println("=================================");
                System.out.println("Student ID: " + studentId);
                System.out.println("Student Name: " + studentName);
                System.out.println("Obtained Marks: " + obtainedMarks);
                System.out.println("Total Marks: " + totalMarks);
                System.out.println("=================================");

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

