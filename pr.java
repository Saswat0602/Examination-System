import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.InputMismatchException;

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

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        submitExam();
    }

    // Method to submit the exam and store the obtained marks in the database
    private void submitExam() {
        obtainedMarks = 0;
        for (Question question : questions) {
            obtainedMarks += question.getObtainedMarks();
        }
        System.out.println("\nSubmitted! Obtained Marks: " + obtainedMarks + "/" + totalMarks);

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO student_marks (student_name, obtained_marks) VALUES (?, ?)")) {
            statement.setString(1, studentName);
            statement.setInt(2, obtainedMarks);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to display all student marks from the database
    public void displayAllStudentMarks() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM student_marks")) {
            System.out.println("Student Marks:");
            System.out.println("---------------");
            while (resultSet.next()) {
                String studentName = resultSet.getString("student_name");
                int obtainedMarks = resultSet.getInt("obtained_marks");
                System.out.println("Student Name: " + studentName + ", Obtained Marks: " + obtainedMarks);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Method to validate user input within a specified range
    private int getValidatedInput(String prompt, int min, int max) {
        int input = -1;
        Scanner sc = new Scanner(System.in);
        boolean isValidInput = false;

        while (!isValidInput) {
            try {
                System.out.print(prompt);
                input = sc.nextInt();
                if (input >= min && input <= max) {
                    isValidInput = true;
                } else {
                    System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                sc.nextLine(); // Clear the input buffer
            }
        }

        return input;
    }

    // Setter and Getter methods
    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getTotalMarks() {
        return totalMarks;
    }
}

// Main class for the Examination System
 class ExaminationSystem {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n*** Welcome to the Examination System ***");
            System.out.println("1. Admin Login");
            System.out.println("2. Student Login");
            System.out.println("3. Exit");

            int choice = getValidatedInput("Enter your choice (1-3): ", 1, 3);

            if (choice == 1) {
                System.out.print("Enter Admin Username: ");
                String adminUsername = sc.nextLine();
                System.out.print("Enter Admin Password: ");
                String adminPassword = sc.nextLine();

                // Check admin credentials here

                System.out.println("\n*** Admin Menu ***");
                System.out.println("1. Add Question");
                System.out.println("2. Display All Student Marks");
                System.out.println("3. Logout");

                int adminChoice = getValidatedInput("Enter your choice (1-3): ", 1, 3);

                if (adminChoice == 1) {
                    // Add question logic
                } else if (adminChoice == 2) {
                    Exam exam = new Exam();
                    exam.displayAllStudentMarks();
                } else if (adminChoice == 3) {
                    System.out.println("Logged out successfully!");
                }
            } else if (choice == 2) {
                System.out.println("\n*** Student Menu ***");
                System.out.println("1. Start Exam");
                System.out.println("2. Logout");

                int studentChoice = getValidatedInput("Enter your choice (1-2): ", 1, 2);

                if (studentChoice == 1) {
                    // Start exam logic
                } else if (studentChoice == 2) {
                    System.out.println("Logged out successfully!");
                }
            } else if (choice == 3) {
                System.out.println("Thank you for using the Examination System. Goodbye!");
                break;
            }
        }
    }

    // Method to validate user input within a specified range
    private static int getValidatedInput(String prompt, int min, int max) {
        int input = -1;
        Scanner sc = new Scanner(System.in);
        boolean isValidInput = false;

        while (!isValidInput) {
            try {
                System.out.print(prompt);
                input = sc.nextInt();
                if (input >= min && input <= max) {
                    isValidInput = true;
                } else {
                    System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                sc.nextLine(); // Clear the input buffer
            }
        }

        return input;
    }
}
