import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
// Import FocusListener for saving option text when focus is lost
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;


public class QuizApp extends JFrame {

    // --- Data Storage (In-Memory) ---
    // Stores quizzes: Key = Quiz Code, Value = Quiz Object
    private static Map<String, Quiz> quizzes = new HashMap<>();
    // Stores user credentials (for basic login/signup)
    private static Map<String, String[]> users = new HashMap<>(); // Key=username, Value=[password, role]

    // --- Quiz and Question Structures ---
    static class Question {
        String questionText;
        String[] options = new String[4];
        int correctOptionIndex;

        Question(String text, String o1, String o2, String o3, String o4, int correctIndex) {
            questionText = text;
            options[0] = o1;
            options[1] = o2;
            options[2] = o3;
            options[3] = o4;
            correctOptionIndex = correctIndex;
        }
    }

    static class Quiz {
        String code;
        String title;
        List<Question> questions = new ArrayList<>();
        // In a real app, you'd add start time, duration etc. here

        Quiz(String code, String title) {
            this.code = code;
            this.title = title;
        }
    }

    // --- UI Components ---
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LoginPanel loginPanel;
    private SignupPanel signupPanel; // Added Signup Panel
    private TeacherPanel teacherPanel;
    private AddQuestionsPanel addQuestionsPanel;
    private StudentPanel studentPanel;
    private TakeQuizPanel takeQuizPanel;
    private ResultsPanel resultsPanel;

    // --- State Variables ---
    private String currentEditingQuizCode = null; // Which quiz the teacher is adding questions to
    private Quiz currentTakingQuiz = null; // Which quiz the student is taking
    private int currentQuestionIndex = 0;
    private int studentScore = 0;
    private List<Integer> studentAnswers = new ArrayList<>(); // Store student's chosen index for each question


    public QuizApp() {
        super("Quiz Master Application");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600); // Increased height slightly for new layout
        setLocationRelativeTo(null); // Center the window

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create panels
        loginPanel = new LoginPanel();
        signupPanel = new SignupPanel(); // Instantiate Signup Panel
        teacherPanel = new TeacherPanel();
        addQuestionsPanel = new AddQuestionsPanel();
        studentPanel = new StudentPanel();
        takeQuizPanel = new TakeQuizPanel();
        resultsPanel = new ResultsPanel();


        // Add panels to the CardLayout
        mainPanel.add(loginPanel, "Login");
        mainPanel.add(signupPanel, "Signup"); // Add Signup Panel to layout
        mainPanel.add(teacherPanel, "Teacher");
        mainPanel.add(addQuestionsPanel, "AddQuestions");
        mainPanel.add(studentPanel, "Student");
        mainPanel.add(takeQuizPanel, "TakeQuiz");
        mainPanel.add(resultsPanel, "Results");


        add(mainPanel); // Add the main panel to the JFrame
        cardLayout.show(mainPanel, "Login"); // Show login panel first
    }

    // --- Utility Methods ---
    private void showPanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
    }

    private String generateQuizCode() {
        // Simple 6-digit code generator
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    // --- Panel Classes ---

    // 1. Login Panel (Modified to include Signup button)
    class LoginPanel extends JPanel implements ActionListener {
        JTextField usernameField;
        JPasswordField passwordField;
        JButton loginButton;
        JButton signupButton; // Added Signup button
        JLabel messageLabel;

        LoginPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.gridwidth = GridBagConstraints.REMAINDER; // Each component on a new line
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;

            JLabel titleLabel = new JLabel("Quiz Master Login", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            add(titleLabel, gbc);

            usernameField = new JTextField(20);
            passwordField = new JPasswordField(20);
            loginButton = new JButton("Login");
            signupButton = new JButton("Go to Sign Up"); // Button text
            messageLabel = new JLabel(" ", SwingConstants.CENTER);
            messageLabel.setForeground(Color.RED);

            add(new JLabel("Username:"), gbc);
            add(usernameField, gbc);
            add(new JLabel("Password:"), gbc);
            add(passwordField, gbc);

            // Panel for buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            buttonPanel.add(loginButton);
            buttonPanel.add(signupButton);
            gbc.fill = GridBagConstraints.NONE; // Don't stretch the button panel
            add(buttonPanel, gbc);

            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(messageLabel, gbc);

            loginButton.addActionListener(this);
            signupButton.addActionListener(this); // Add listener for signup button
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == loginButton) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                messageLabel.setText(" "); // Clear message

                if (users.containsKey(username)) {
                    String[] userData = users.get(username);
                    if (userData[0].equals(password)) {
                        // Login successful
                        if (userData[1].equals("teacher")) {
                            teacherPanel.refreshQuizList(); // Update teacher's quiz list
                            showPanel("Teacher");
                        } else if (userData[1].equals("student")) {
                            showPanel("Student");
                        } else {
                            messageLabel.setText("Unknown user role.");
                        }
                        // Clear fields after successful login attempt
                        usernameField.setText("");
                        passwordField.setText("");
                    } else {
                        messageLabel.setText("Invalid password.");
                    }
                } else {
                    messageLabel.setText("Username not found.");
                }
            } else if (e.getSource() == signupButton) {
                // Clear fields and message before switching
                usernameField.setText("");
                passwordField.setText("");
                messageLabel.setText(" ");
                signupPanel.clearFields(); // Clear signup fields as well
                showPanel("Signup"); // Switch to Signup panel
            }
        }
    }

    // NEW Signup Panel
    class SignupPanel extends JPanel implements ActionListener {
        JTextField usernameField;
        JPasswordField passwordField;
        JRadioButton teacherRadio;
        JRadioButton studentRadio;
        ButtonGroup roleGroup;
        JButton signupButton;
        JButton backToLoginButton;
        JLabel messageLabel;

        SignupPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;

            JLabel titleLabel = new JLabel("Create New Account", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            add(titleLabel, gbc);

            usernameField = new JTextField(20);
            passwordField = new JPasswordField(20);
            teacherRadio = new JRadioButton("Teacher");
            studentRadio = new JRadioButton("Student", true); // Default to student
            roleGroup = new ButtonGroup();
            roleGroup.add(teacherRadio);
            roleGroup.add(studentRadio);

            JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            radioPanel.add(new JLabel("Role:"));
            radioPanel.add(studentRadio);
            radioPanel.add(teacherRadio);

            signupButton = new JButton("Sign Up");
            backToLoginButton = new JButton("Back to Login");
            messageLabel = new JLabel(" ", SwingConstants.CENTER);
            messageLabel.setForeground(Color.RED); // Use red for errors initially

            add(new JLabel("Username:"), gbc);
            add(usernameField, gbc);
            add(new JLabel("Password:"), gbc);
            add(passwordField, gbc);
            add(radioPanel, gbc);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            buttonPanel.add(signupButton);
            buttonPanel.add(backToLoginButton);
            gbc.fill = GridBagConstraints.NONE;
            add(buttonPanel, gbc);

            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(messageLabel, gbc);

            signupButton.addActionListener(this);
            backToLoginButton.addActionListener(this);
        }

        void clearFields() {
             usernameField.setText("");
             passwordField.setText("");
             studentRadio.setSelected(true); // Reset role selection
             messageLabel.setText(" ");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == signupButton) {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                String role = studentRadio.isSelected() ? "student" : "teacher";
                messageLabel.setText(" "); // Clear previous message

                if (username.isEmpty() || password.isEmpty()) {
                    messageLabel.setForeground(Color.RED);
                    messageLabel.setText("Username and password cannot be empty.");
                    return;
                }
                if (username.equals("teacher") || username.equals("student")) {
                     messageLabel.setForeground(Color.RED);
                     messageLabel.setText("Cannot use 'teacher' or 'student' as username.");
                     return;
                }

                if (users.containsKey(username)) {
                    messageLabel.setForeground(Color.RED);
                    messageLabel.setText("Username already exists. Please choose another.");
                } else {
                    // Add user to the in-memory map
                    users.put(username, new String[]{password, role});
                    messageLabel.setForeground(Color.GREEN); // Success message color
                    messageLabel.setText("Account created successfully! Please log in.");
                    // Optionally clear fields after success
                    // usernameField.setText("");
                    // passwordField.setText("");
                    // studentRadio.setSelected(true);
                }
            } else if (e.getSource() == backToLoginButton) {
                clearFields(); // Clear fields before going back
                showPanel("Login"); // Switch back to Login panel
            }
        }
    }


    // 2. Teacher Panel (Create Quiz, View List)
    class TeacherPanel extends JPanel implements ActionListener {
        JTextField quizTitleField;
        JButton createQuizButton;
        JList<String> quizListDisplay; // Display "Title (Code)"
        DefaultListModel<String> quizListModel;
        JButton addQuestionsButton;
        JButton logoutButton;
        JLabel messageLabel;

        TeacherPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // --- Top Panel: Create Quiz ---
            JPanel createPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            createPanel.setBorder(BorderFactory.createTitledBorder("Create New Quiz"));
            quizTitleField = new JTextField(25);
            createQuizButton = new JButton("Create Quiz");
            messageLabel = new JLabel(" ");
            messageLabel.setForeground(Color.BLUE);

            createPanel.add(new JLabel("Quiz Title:"));
            createPanel.add(quizTitleField);
            createPanel.add(createQuizButton);
            createPanel.add(messageLabel);

            // --- Center Panel: Quiz List ---
            JPanel listPanel = new JPanel(new BorderLayout(5, 5));
            listPanel.setBorder(BorderFactory.createTitledBorder("My Quizzes"));
            quizListModel = new DefaultListModel<>();
            quizListDisplay = new JList<>(quizListModel);
            quizListDisplay.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            listPanel.add(new JScrollPane(quizListDisplay), BorderLayout.CENTER);

            // --- Bottom Panel: Actions ---
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            addQuestionsButton = new JButton("Add/View Questions for Selected Quiz");
            logoutButton = new JButton("Logout");
            actionPanel.add(addQuestionsButton);
            actionPanel.add(logoutButton);


            add(createPanel, BorderLayout.NORTH);
            add(listPanel, BorderLayout.CENTER);
            add(actionPanel, BorderLayout.SOUTH);

            // Action Listeners
            createQuizButton.addActionListener(this);
            addQuestionsButton.addActionListener(this);
            logoutButton.addActionListener(this);
        }

        void refreshQuizList() {
            quizListModel.clear();
            messageLabel.setText(" ");
            // In a real app, filter by teacher ID
            for (Quiz quiz : quizzes.values()) {
                quizListModel.addElement(quiz.title + " (" + quiz.code + ")");
            }
            if (quizListModel.isEmpty()) {
                 quizListModel.addElement("No quizzes created yet.");
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            messageLabel.setText(" "); // Clear message
            if (e.getSource() == createQuizButton) {
                String title = quizTitleField.getText().trim();
                if (title.isEmpty()) {
                    messageLabel.setText("Please enter a quiz title.");
                    return;
                }
                String code = generateQuizCode();
                while (quizzes.containsKey(code)) { // Ensure code is unique
                    code = generateQuizCode();
                }
                Quiz newQuiz = new Quiz(code, title);
                quizzes.put(code, newQuiz);
                quizTitleField.setText(""); // Clear input
                messageLabel.setText("Quiz '" + title + "' created with code: " + code);
                refreshQuizList(); // Update the list
            } else if (e.getSource() == addQuestionsButton) {
                int selectedIndex = quizListDisplay.getSelectedIndex();
                if (selectedIndex != -1 && !quizListModel.getElementAt(selectedIndex).startsWith("No quizzes")) {
                    String selectedItem = quizListModel.getElementAt(selectedIndex);
                    // Extract code from "Title (Code)"
                    currentEditingQuizCode = selectedItem.substring(selectedItem.lastIndexOf('(') + 1, selectedItem.lastIndexOf(')'));
                    Quiz selectedQuiz = quizzes.get(currentEditingQuizCode);
                    if (selectedQuiz != null) {
                        addQuestionsPanel.loadQuizData(selectedQuiz);
                        showPanel("AddQuestions");
                    } else {
                         messageLabel.setText("Error: Could not find selected quiz data.");
                    }
                } else {
                    messageLabel.setText("Please select a quiz from the list first.");
                }
            } else if (e.getSource() == logoutButton) {
                // In a real app, clear session data
                showPanel("Login");
            }
        }
    }


    // 3. Add Questions Panel (MODIFIED UI and logic)
    class AddQuestionsPanel extends JPanel implements ActionListener {
        JLabel quizTitleLabel;
        JTextArea questionTextArea;
        JButton[] editOptionButtons = new JButton[4]; // Buttons to trigger editing
        JRadioButton[] correctOptionButtons = new JRadioButton[4]; // Radio buttons next to edit buttons
        ButtonGroup correctOptionGroup;
        JLabel editingOptionLabel; // Label indicating which option is being edited
        JTextField optionEditField; // Single field to edit option text
        JButton addQuestionButton;
        JButton doneButton;
        JLabel messageLabel;
        JTextArea addedQuestionsArea;

        // Temporary storage for option texts while editing
        private String[] currentOptionTexts = {"", "", "", ""};
        private int currentlyEditingOption = -1; // Index of option being edited, -1 if none

        AddQuestionsPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            quizTitleLabel = new JLabel("Adding Questions to: ", SwingConstants.CENTER);
            quizTitleLabel.setFont(new Font("Arial", Font.BOLD, 16));

            // --- Input Form Panel ---
            JPanel formPanel = new JPanel();
            formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS)); // Vertical layout
            formPanel.setBorder(BorderFactory.createTitledBorder("New Question Details"));

            questionTextArea = new JTextArea(3, 40);
            questionTextArea.setLineWrap(true);
            questionTextArea.setWrapStyleWord(true);
            formPanel.add(new JLabel("Question Text:"));
            formPanel.add(new JScrollPane(questionTextArea));
            formPanel.add(Box.createVerticalStrut(10));

            // --- Options Editing Section ---
            JPanel optionsEditPanel = new JPanel(new GridLayout(0, 1, 5, 5)); // One column grid for options
            optionsEditPanel.setBorder(BorderFactory.createTitledBorder("Answer Options"));
            correctOptionGroup = new ButtonGroup();

            for (int i = 0; i < 4; i++) {
                JPanel singleOptionLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
                editOptionButtons[i] = new JButton("Edit Option " + (i + 1));
                editOptionButtons[i].setPreferredSize(new Dimension(150, 25)); // Give buttons a fixed size
                editOptionButtons[i].setActionCommand(String.valueOf(i)); // Store index in action command
                editOptionButtons[i].addActionListener(this::handleEditOptionButton); // Use method reference

                correctOptionButtons[i] = new JRadioButton("Correct");
                correctOptionGroup.add(correctOptionButtons[i]);

                singleOptionLine.add(editOptionButtons[i]);
                singleOptionLine.add(correctOptionButtons[i]);
                optionsEditPanel.add(singleOptionLine);
            }
            formPanel.add(optionsEditPanel);
            formPanel.add(Box.createVerticalStrut(5));

            // --- Field for Editing ---
            editingOptionLabel = new JLabel("Click 'Edit Option' to enter text.");
            optionEditField = new JTextField(30);
            optionEditField.setEnabled(false); // Initially disabled
            optionEditField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    saveCurrentOptionText(); // Save when focus leaves the text field
                }
            });
             // Also save when pressing Enter in the edit field
             optionEditField.addActionListener(e -> saveCurrentOptionText());


            JPanel editFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            editFieldPanel.add(editingOptionLabel);
            editFieldPanel.add(optionEditField);
            formPanel.add(editFieldPanel);
            formPanel.add(Box.createVerticalStrut(10));


            addQuestionButton = new JButton("Add This Question");
            addQuestionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            formPanel.add(addQuestionButton);

            messageLabel = new JLabel(" ");
            messageLabel.setForeground(Color.BLUE);
            messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            formPanel.add(messageLabel);

            // --- Added Questions Display ---
            JPanel addedPanel = new JPanel(new BorderLayout(5, 5));
            addedPanel.setBorder(BorderFactory.createTitledBorder("Questions Added So Far"));
            addedQuestionsArea = new JTextArea(10, 40);
            addedQuestionsArea.setEditable(false);
            addedQuestionsArea.setLineWrap(true);
            addedQuestionsArea.setWrapStyleWord(true);
            addedPanel.add(new JScrollPane(addedQuestionsArea), BorderLayout.CENTER);

            // --- Done Button ---
            doneButton = new JButton("Done Adding Questions (Back to Quiz List)");
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            bottomPanel.add(doneButton);

            add(quizTitleLabel, BorderLayout.NORTH);
            add(formPanel, BorderLayout.CENTER);
            add(addedPanel, BorderLayout.EAST);
            add(bottomPanel, BorderLayout.SOUTH);

            addQuestionButton.addActionListener(this);
            doneButton.addActionListener(this);
        }

        // Action listener for the "Edit Option X" buttons
        private void handleEditOptionButton(ActionEvent e) {
            saveCurrentOptionText(); // Save any text from the previously edited option first

            int indexToEdit = Integer.parseInt(e.getActionCommand());
            currentlyEditingOption = indexToEdit;

            editingOptionLabel.setText("Editing Option " + (indexToEdit + 1) + ":");
            optionEditField.setText(currentOptionTexts[indexToEdit]); // Load current text
            optionEditField.setEnabled(true); // Enable editing
            optionEditField.requestFocusInWindow(); // Set focus to the text field
        }

        // Helper to save text from the edit field to temporary storage AND update button text
        private void saveCurrentOptionText() {
            if (currentlyEditingOption != -1 && optionEditField.isEnabled()) {
                String newText = optionEditField.getText().trim();
                currentOptionTexts[currentlyEditingOption] = newText;

                // Update the corresponding button's text
                String buttonText = "Option " + (currentlyEditingOption + 1);
                if (!newText.isEmpty()) {
                    // Show snippet of text
                    buttonText += ": " + (newText.length() > 15 ? newText.substring(0, 12) + "..." : newText);
                }
                editOptionButtons[currentlyEditingOption].setText(buttonText);

                System.out.println("Saved text for option " + (currentlyEditingOption + 1) + ": " + currentOptionTexts[currentlyEditingOption]);

                // Disable the field after saving (optional, but prevents accidental edits)
                // optionEditField.setEnabled(false);
                // editingOptionLabel.setText("Click 'Edit Option' to enter text.");
                // currentlyEditingOption = -1;
            }
        }

        void loadQuizData(Quiz quiz) {
            quizTitleLabel.setText("Adding Questions to: " + quiz.title + " (" + quiz.code + ")");
            messageLabel.setText(" ");
            clearFormForNewQuestion(); // Clear form state
            refreshAddedQuestionsDisplay(quiz);
        }

        void clearFormForNewQuestion() {
            questionTextArea.setText("");
            // Clear temporary storage
            currentOptionTexts = new String[]{"", "", "", ""};
            currentlyEditingOption = -1;
            // Reset UI for option editing
            optionEditField.setText("");
            optionEditField.setEnabled(false);
            editingOptionLabel.setText("Click 'Edit Option' to enter text.");
            // Reset button texts
            for(int i=0; i<4; i++) {
                editOptionButtons[i].setText("Edit Option " + (i+1));
            }
            correctOptionGroup.clearSelection();
            messageLabel.setText(" ");
        }


        void refreshAddedQuestionsDisplay(Quiz quiz) {
            addedQuestionsArea.setText(""); // Clear previous
            if (quiz == null) {
                addedQuestionsArea.setText("Error: Could not load quiz data.");
                return;
            }
            if (quiz.questions.isEmpty()) {
                 addedQuestionsArea.setText("No questions added yet for this quiz.");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < quiz.questions.size(); i++) {
                    Question q = quiz.questions.get(i);
                    sb.append("Q").append(i + 1).append(": ").append(q.questionText).append("\n");
                    for(int j=0; j<4; j++) {
                        sb.append("  ").append(j+1).append(") ").append(q.options[j]);
                        if(j == q.correctOptionIndex) {
                            sb.append(" (Correct)");
                        }
                        sb.append("\n");
                    }
                    sb.append("----\n");
                }
                addedQuestionsArea.setText(sb.toString());
                addedQuestionsArea.setCaretPosition(0); // Scroll to top
            }
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            messageLabel.setText(" "); // Clear message
            if (e.getSource() == addQuestionButton) {
                 saveCurrentOptionText(); // Ensure the last edited text is saved

                String qText = questionTextArea.getText().trim();
                int correctIndex = -1;
                boolean optionsFilled = true;

                // Check temporary storage for options
                for (String opt : currentOptionTexts) {
                    if (opt.isEmpty()) {
                        optionsFilled = false;
                        break;
                    }
                }

                // Find which radio button is selected for correct answer
                for (int i = 0; i < 4; i++) {
                     if (correctOptionButtons[i].isSelected()) {
                         correctIndex = i;
                         break;
                     }
                }

                if (qText.isEmpty() || !optionsFilled || correctIndex == -1) {
                    messageLabel.setText("Please fill question, provide text for all 4 options, and select correct answer.");
                    return;
                }

                Quiz currentQuiz = quizzes.get(currentEditingQuizCode);
                if (currentQuiz != null) {
                    // Create the new Question object
                    Question newQuestion = new Question(qText,
                        currentOptionTexts[0], currentOptionTexts[1],
                        currentOptionTexts[2], currentOptionTexts[3],
                        correctIndex);

                    // Add the question to the quiz's list
                    currentQuiz.questions.add(newQuestion);

                    messageLabel.setText("Question added!");
                    refreshAddedQuestionsDisplay(currentQuiz); // Update display RIGHT AFTER adding
                    clearFormForNewQuestion(); // Clear form for next question
                } else {
                     messageLabel.setText("Error: Could not find the current quiz to add to.");
                }

            } else if (e.getSource() == doneButton) {
                saveCurrentOptionText(); // Save any pending text before leaving
                currentEditingQuizCode = null; // Reset editing state
                teacherPanel.refreshQuizList(); // Refresh list in case of changes
                showPanel("Teacher");
            }
        }
    }


    // 4. Student Panel (Enter Code)
    class StudentPanel extends JPanel implements ActionListener {
        JTextField codeField;
        JButton startQuizButton;
        JButton logoutButton;
        JLabel messageLabel;

        StudentPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;


            JLabel titleLabel = new JLabel("Student Portal", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            add(titleLabel, gbc);

            codeField = new JTextField(15);
            startQuizButton = new JButton("Start Quiz");
            logoutButton = new JButton("Logout");
            messageLabel = new JLabel(" ", SwingConstants.CENTER);
            messageLabel.setForeground(Color.RED);

            add(new JLabel("Enter Quiz Code:"), gbc);
            add(codeField, gbc);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0)); // Center buttons with spacing
            buttonPanel.add(startQuizButton);
            buttonPanel.add(logoutButton);
            gbc.fill = GridBagConstraints.NONE;
            add(buttonPanel, gbc);


            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(messageLabel, gbc);


            startQuizButton.addActionListener(this);
            logoutButton.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
             messageLabel.setText(" "); // Clear message
            if (e.getSource() == startQuizButton) {
                String code = codeField.getText().trim();
                if (quizzes.containsKey(code)) {
                    currentTakingQuiz = quizzes.get(code);
                    if (currentTakingQuiz.questions.isEmpty()) {
                        messageLabel.setText("This quiz has no questions yet.");
                        currentTakingQuiz = null; // Reset
                        return;
                    }
                    // Reset quiz state for student
                    currentQuestionIndex = 0;
                    studentScore = 0;
                    studentAnswers.clear();

                    takeQuizPanel.loadQuestion(); // Load the first question
                    showPanel("TakeQuiz");
                    codeField.setText(""); // Clear code field
                } else {
                    messageLabel.setText("Invalid quiz code.");
                }
            } else if (e.getSource() == logoutButton) {
                // Clear quiz state if any
                 currentTakingQuiz = null;
                 codeField.setText("");
                showPanel("Login");
            }
        }
    }


    // 5. Take Quiz Panel
    class TakeQuizPanel extends JPanel implements ActionListener {
        JLabel questionNumberLabel;
        JTextArea questionTextDisplay;
        ButtonGroup optionsGroup;
        JRadioButton[] optionButtons = new JRadioButton[4];
        JButton nextButton;

        TakeQuizPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // More padding

            questionNumberLabel = new JLabel("Question X of Y", SwingConstants.CENTER);
            questionNumberLabel.setFont(new Font("Arial", Font.BOLD, 16));

            questionTextDisplay = new JTextArea(5, 40);
            questionTextDisplay.setEditable(false);
            questionTextDisplay.setLineWrap(true);
            questionTextDisplay.setWrapStyleWord(true);
            questionTextDisplay.setFont(new Font("Arial", Font.PLAIN, 14));
            questionTextDisplay.setBackground(getBackground()); // Match panel background

            JPanel optionsPanel = new JPanel();
            optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS)); // Vertical layout for options
            optionsGroup = new ButtonGroup();
            for (int i = 0; i < 4; i++) {
                optionButtons[i] = new JRadioButton();
                optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 14));
                optionsGroup.add(optionButtons[i]);
                optionsPanel.add(optionButtons[i]);
                optionsPanel.add(Box.createVerticalStrut(5)); // Small space between options
            }

            nextButton = new JButton("Next Question");
            nextButton.addActionListener(this);
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(nextButton);

            add(questionNumberLabel, BorderLayout.NORTH);
            add(new JScrollPane(questionTextDisplay), BorderLayout.CENTER); // Wrap text area
            add(optionsPanel, BorderLayout.EAST); // Put options on the side
            add(buttonPanel, BorderLayout.SOUTH);

        }

        void loadQuestion() {
            if (currentTakingQuiz == null || currentQuestionIndex >= currentTakingQuiz.questions.size()) {
                // Should not happen if logic is correct, but handles error state
                showResults();
                return;
            }

            Question q = currentTakingQuiz.questions.get(currentQuestionIndex);
            questionNumberLabel.setText("Question " + (currentQuestionIndex + 1) + " of " + currentTakingQuiz.questions.size());
            questionTextDisplay.setText(q.questionText);
            optionsGroup.clearSelection(); // Clear previous selection

            for (int i = 0; i < 4; i++) {
                optionButtons[i].setText(q.options[i]);
                optionButtons[i].setVisible(true); // Ensure they are visible
                optionButtons[i].setEnabled(true);
            }

            // Change button text for the last question
            if (currentQuestionIndex == currentTakingQuiz.questions.size() - 1) {
                nextButton.setText("Finish Quiz");
            } else {
                nextButton.setText("Next Question");
            }
             questionTextDisplay.setCaretPosition(0); // Scroll to top
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedOption = -1;
            for (int i = 0; i < 4; i++) {
                if (optionButtons[i].isSelected()) {
                    selectedOption = i;
                    break;
                }
            }

            if (selectedOption == -1) {
                JOptionPane.showMessageDialog(this, "Please select an answer.", "No Answer Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Record student's answer
            studentAnswers.add(selectedOption);

            // Move to next question or finish
            currentQuestionIndex++;
            if (currentQuestionIndex < currentTakingQuiz.questions.size()) {
                loadQuestion();
            } else {
                calculateResults();
                showResults();
            }
        }
    }


    // 6. Results Panel
    class ResultsPanel extends JPanel implements ActionListener {
        JLabel scoreLabel;
        JTextArea summaryArea; // To show correct/incorrect breakdown
        JButton backButton;

        ResultsPanel() {
             setLayout(new BorderLayout(10, 10));
             setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

             JLabel titleLabel = new JLabel("Quiz Results", SwingConstants.CENTER);
             titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

             scoreLabel = new JLabel("Your Score: X / Y", SwingConstants.CENTER);
             scoreLabel.setFont(new Font("Arial", Font.BOLD, 18));

             summaryArea = new JTextArea(15, 40);
             summaryArea.setEditable(false);
             summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Monospaced for alignment

             backButton = new JButton("Back to Student Portal");
             backButton.addActionListener(this);
             JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
             buttonPanel.add(backButton);

             add(titleLabel, BorderLayout.NORTH);
             add(scoreLabel, BorderLayout.CENTER);
             add(new JScrollPane(summaryArea), BorderLayout.EAST); // Summary on the side
             add(buttonPanel, BorderLayout.SOUTH);
        }

        void displayResults() {
             if (currentTakingQuiz == null) return; // Should not happen

             scoreLabel.setText("Your Score: " + studentScore + " / " + currentTakingQuiz.questions.size());

             StringBuilder summary = new StringBuilder("--- Quiz Summary ---\n\n");
             for(int i=0; i < currentTakingQuiz.questions.size(); i++) {
                 Question q = currentTakingQuiz.questions.get(i);
                 int studentAnsIndex = (i < studentAnswers.size()) ? studentAnswers.get(i) : -1; // Get student answer, handle if missing
                 boolean isCorrect = (studentAnsIndex == q.correctOptionIndex);

                 summary.append("Q").append(i+1).append(": ").append(q.questionText).append("\n");
                 summary.append("  Correct Answer: ").append(q.options[q.correctOptionIndex]).append("\n");
                 if (studentAnsIndex != -1) {
                    summary.append("  Your Answer:    ").append(q.options[studentAnsIndex]);
                    summary.append(isCorrect ? " (Correct)\n" : " (Incorrect)\n");
                 } else {
                     summary.append("  Your Answer:    (Not Answered)\n");
                 }
                 summary.append("\n");
             }
             summaryArea.setText(summary.toString());
             summaryArea.setCaretPosition(0); // Scroll to top
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Reset quiz state
            currentTakingQuiz = null;
            currentQuestionIndex = 0;
            studentScore = 0;
            studentAnswers.clear();
            showPanel("Student"); // Go back to student code entry
        }
    }


     // --- Helper Methods to Calculate and Show Results ---
    private void calculateResults() {
        if (currentTakingQuiz == null) return;
        studentScore = 0;
        for (int i = 0; i < currentTakingQuiz.questions.size(); i++) {
            if (i < studentAnswers.size()) { // Check if student answered this question
                 Question q = currentTakingQuiz.questions.get(i);
                 int studentAnsIndex = studentAnswers.get(i);
                 if (studentAnsIndex == q.correctOptionIndex) {
                     studentScore++;
                 }
            }
        }
    }

     private void showResults() {
         resultsPanel.displayResults();
         showPanel("Results");
     }


    // --- Main Method ---
    public static void main(String[] args) {
        // Ensure GUI updates happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            QuizApp app = new QuizApp();
            app.setVisible(true);
        });
    }
}

