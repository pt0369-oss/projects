import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class NumberPuzzleGame extends JFrame implements ActionListener {

    private static final int GRID_SIZE = 4; // 4x4 grid
    private JPanel gridPanel;
    private JButton[][] buttons = new JButton[GRID_SIZE][GRID_SIZE];
    private JButton emptyButton; // Reference to the empty button
    private Point emptyPos; // Position (row, col) of the empty button
    private JButton shuffleButton;
    private JLabel statusLabel;

    // private List<Integer> initialTileOrder; // Stores the shuffled order for reset - Removed as shuffle logic changed

    public NumberPuzzleGame() {
        super("Number Puzzle Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 450); // Adjusted size for status/shuffle
        setLocationRelativeTo(null); // Center window

        // Initialize gridPanel first
        gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE, 5, 5)); // Add gaps
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding

        // --- Bottom Panel for Shuffle and Status ---
        // MOVED THIS SECTION UP to ensure statusLabel exists before shuffleTiles()
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        shuffleButton = new JButton("Shuffle / New Game");
        shuffleButton.addActionListener(e -> shuffleTiles()); // Lambda for shuffle

        // *** Initialize statusLabel HERE, before shuffleTiles is called ***
        statusLabel = new JLabel("Click a tile next to the empty space to move it.", SwingConstants.CENTER);

        bottomPanel.add(shuffleButton, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        // --- Initialize the grid buttons ---
        initializeGrid(); // Now initializeGrid is called

        // --- Shuffle the tiles ---
        shuffleTiles(); // NOW shuffleTiles() is called, AFTER statusLabel exists

        // --- Add Panels to Frame ---
        add(gridPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH); // Add bottom panel HERE

        setVisible(true);
    }

    private void initializeGrid() {
        Font buttonFont = new Font("Arial", Font.BOLD, 24);
        int tileNumber = 1;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (row == GRID_SIZE - 1 && col == GRID_SIZE - 1) {
                    // Last button is empty
                    buttons[row][col] = new JButton("");
                    buttons[row][col].setEnabled(false); // Make it visually distinct and non-clickable
                    buttons[row][col].setBackground(Color.LIGHT_GRAY); // Different background
                    emptyButton = buttons[row][col];
                    emptyPos = new Point(row, col);
                } else {
                    buttons[row][col] = new JButton(String.valueOf(tileNumber++));
                    buttons[row][col].setFont(buttonFont);
                    buttons[row][col].addActionListener(this);
                    buttons[row][col].setFocusPainted(false); // Remove focus border
                }
                gridPanel.add(buttons[row][col]);
            }
        }
    }

    // Shuffle by making random valid moves from the solved state
    private void shuffleTiles() {
        // Now statusLabel should not be null here
        statusLabel.setText("Click a tile next to the empty space to move it.");
        statusLabel.setForeground(Color.BLACK);

        // Reset to solved state first
        int num = 1;
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                boolean isEmpty = (r == GRID_SIZE - 1 && c == GRID_SIZE - 1);
                // Make sure buttons[r][c] exists before accessing it
                if (buttons[r][c] == null) continue; // Should not happen if initializeGrid runs first

                buttons[r][c].setText(isEmpty ? "" : String.valueOf(num++));
                buttons[r][c].setEnabled(!isEmpty);
                // Set default background, avoid potential null UIManager value
                Color defaultBg = UIManager.getColor("Button.background");
                buttons[r][c].setBackground(isEmpty ? Color.LIGHT_GRAY : (defaultBg != null ? defaultBg : Color.WHITE)); // Fallback color
                if(isEmpty){
                    emptyButton = buttons[r][c];
                    emptyPos = new Point(r, c);
                }
            }
        }


        // Make a large number of random valid moves
        Random random = new Random();
        int shuffles = GRID_SIZE * GRID_SIZE * 10; // Number of random moves
        for (int i = 0; i < shuffles; i++) {
            List<Point> neighbors = getValidNeighbors(emptyPos);
            if (!neighbors.isEmpty()) {
                Point randomNeighbor = neighbors.get(random.nextInt(neighbors.size()));
                swapButtons(emptyPos, randomNeighbor);
                emptyPos = randomNeighbor; // Update empty position
            }
        }
    }

    // Get valid neighbors (up, down, left, right) of a position
    private List<Point> getValidNeighbors(Point pos) {
        List<Point> neighbors = new ArrayList<>();
        int r = pos.x;
        int c = pos.y;
        int[] dr = {-1, 1, 0, 0}; // Row changes
        int[] dc = {0, 0, -1, 1}; // Col changes

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                neighbors.add(new Point(nr, nc));
            }
        }
        return neighbors;
    }

    // Find the position (Point) of a button in the grid
    private Point findButtonPosition(JButton button) {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (buttons[row][col] == button) {
                    return new Point(row, col);
                }
            }
        }
        return null; // Should not happen
    }

    // Swap the text and appearance of two buttons
    private void swapButtons(Point pos1, Point pos2) {
        JButton button1 = buttons[pos1.x][pos1.y];
        JButton button2 = buttons[pos2.x][pos2.y];

        // Basic null checks before proceeding
        if (button1 == null || button2 == null) {
            System.err.println("Error: Attempting to swap null buttons.");
            return;
        }


        String tempText = button1.getText();
        button1.setText(button2.getText());
        button2.setText(tempText);

        boolean button1Enabled = button1.isEnabled();
        button1.setEnabled(button2.isEnabled());
        button2.setEnabled(button1Enabled);

        Color tempBg = button1.getBackground();
        button1.setBackground(button2.getBackground());
        button2.setBackground(tempBg);

        // Update emptyButton reference if needed
        if (button1 == emptyButton) emptyButton = button2;
        else if (button2 == emptyButton) emptyButton = button1;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        JButton clickedButton = (JButton) e.getSource();
        Point clickedPos = findButtonPosition(clickedButton);

        if (clickedPos == null) return;

        // Check if the clicked button is adjacent to the empty spot
        int rDiff = Math.abs(clickedPos.x - emptyPos.x);
        int cDiff = Math.abs(clickedPos.y - emptyPos.y);

        if ((rDiff == 1 && cDiff == 0) || (rDiff == 0 && cDiff == 1)) {
            // It's adjacent, swap them
            swapButtons(clickedPos, emptyPos);
            emptyPos = clickedPos; // Update the empty position

            // Check for win after the move
            if (isSolved()) {
                statusLabel.setText("Congratulations! You solved it!");
                statusLabel.setForeground(Color.GREEN.darker());
                // Disable all buttons except shuffle
                for(int r=0; r<GRID_SIZE; r++){
                    for(int c=0; c<GRID_SIZE; c++){
                         // Make sure buttons[r][c] exists before accessing it
                         if(buttons[r][c] != null && buttons[r][c] != emptyButton) {
                             buttons[r][c].setEnabled(false);
                         }
                    }
                }
            } else {
                 statusLabel.setText("Click a tile next to the empty space to move it.");
                 statusLabel.setForeground(Color.BLACK);
            }
        }
    }

    // Check if the puzzle is in the solved state
    private boolean isSolved() {
        int expectedNumber = 1;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                // Skip the last cell (should be empty)
                if (row == GRID_SIZE - 1 && col == GRID_SIZE - 1) {
                     // Make sure buttons[row][col] exists
                    return buttons[row][col] != null && buttons[row][col] == emptyButton; // Check if the last one is indeed empty
                }
                // Make sure buttons[row][col] exists
                if (buttons[row][col] == null || !buttons[row][col].getText().equals(String.valueOf(expectedNumber++))) {
                    return false; // Found a mismatch or null button
                }
            }
        }
        // This line should technically not be reached if GRID_SIZE > 0, but added for safety
        return false;
    }


    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new NumberPuzzleGame());
    }
}

