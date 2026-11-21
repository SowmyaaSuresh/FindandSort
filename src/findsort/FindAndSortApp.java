package findsort;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Find and Sort: an 8x8 turn-based collection game.
 * Data structures used: Stack (undo), Queue-like cyclic turn order,
 * LinkedList (collected tokens/power-ups), ArrayList (history), arrays (scores).
 */
public class FindAndSortApp extends JFrame {
    private static final int N = 8;
    private final JButton[][] cells = new JButton[N][N];
    private final int[][] tokens = new int[N][N];
    private final String[][] specials = new String[N][N];
    private int[] row = new int[2], col = new int[2], scores = new int[2];
    private boolean[] shield = new boolean[2], doubleScore = new boolean[2];
    private int[] extraLives = new int[2];
    private final java.util.List<String> history = new ArrayList<>();
    private final Deque<State> undoStack = new ArrayDeque<>();
    private final java.util.List<Integer> collected = new LinkedList<>();
    private final java.util.List<String> powerUps = new LinkedList<>();
    private final JTextArea historyArea = new JTextArea();
    private final JComboBox<String> playerSelector = new JComboBox<>(new String[]{
            "2 Players", "3 Players", "4 Players", "5 Players",
            "6 Players", "7 Players", "8 Players"});
    private final JLabel turnLabel = new JLabel("Welcome to Find and Sort", SwingConstants.CENTER);
    private final JLabel scoreLabel = new JLabel("Scores will appear here", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("Start a game to begin.", SwingConstants.CENTER);
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);
    private int players = 2, current = 0, turnCount = 0;
    private boolean gameOver = true;
    private final Random random = new Random();

    private static final Color BG = new Color(242, 246, 252);
    private static final Color EMPTY = new Color(239, 244, 251);
    private static final Color TOKEN = new Color(255, 232, 178);
    private static final Color SPECIAL = new Color(207, 239, 220);
    private static final Color PLAYER = new Color(191, 216, 250);

    public FindAndSortApp() {
        super("Find & Sort | Strategy Game");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Keep the complete interface inside the 1024x768 Render virtual desktop.
        setMinimumSize(new Dimension(900, 620));
        setSize(1000, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        JPanel page = new JPanel(new BorderLayout(18, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(22, 26, 18, 26));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("FIND & SORT");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(new Color(30, 48, 78));
        JLabel subtitle = new JLabel("COLLECT • STRATEGIZE • WIN");
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        subtitle.setForeground(new Color(111, 128, 151));
        brand.add(title);
        brand.add(Box.createVerticalStrut(3));
        brand.add(subtitle);
        header.add(brand, BorderLayout.WEST);

        JPanel setup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        setup.setOpaque(false);
        JLabel playerPrompt = new JLabel("PLAYERS");
        playerPrompt.setFont(new Font("SansSerif", Font.BOLD, 11));
        playerPrompt.setForeground(new Color(91, 111, 137));
        playerSelector.setSelectedIndex(0);
        playerSelector.setFont(new Font("SansSerif", Font.PLAIN, 13));
        playerSelector.setPreferredSize(new Dimension(132, 36));
        playerSelector.setFocusable(false);
        JButton startButton = button("Start Game");
        startButton.setPreferredSize(new Dimension(112, 36));
        startButton.addActionListener(e -> startGame(playerSelector.getSelectedIndex() + 2));
        setup.add(playerPrompt);
        setup.add(playerSelector);
        setup.add(startButton);
        header.add(setup, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);

        JPanel boardCard = new JPanel(new BorderLayout(0, 10));
        boardCard.setBackground(Color.WHITE);
        boardCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(222, 229, 239), 1, true),
                new EmptyBorder(16, 16, 16, 16)));
        JLabel boardHeading = new JLabel("GAME BOARD");
        boardHeading.setFont(new Font("SansSerif", Font.BOLD, 14));
        boardHeading.setForeground(new Color(43, 62, 91));
        boardCard.add(boardHeading, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(N, N, 5, 5));
        board.setBackground(new Color(224, 231, 241));
        board.setBorder(new EmptyBorder(2, 2, 2, 2));
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                final int rr = r, cc = c;
                JButton b = new JButton("");
                b.setFont(new Font("SansSerif", Font.BOLD, 16));
                b.setFocusPainted(false);
                b.setMargin(new Insets(2, 2, 2, 2));
                b.setOpaque(true);
                b.setBorder(new LineBorder(new Color(211, 220, 232), 1, true));
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addActionListener(e -> moveTo(rr, cc));
                cells[r][c] = b;
                board.add(b);
            }
        }
        boardCard.add(board, BorderLayout.CENTER);

        JLabel legend = new JLabel("  ● Player     ◆ Token     ★ Special space", SwingConstants.CENTER);
        legend.setFont(new Font("SansSerif", Font.PLAIN, 12));
        legend.setForeground(new Color(105, 119, 140));
        boardCard.add(legend, BorderLayout.SOUTH);
        center.add(boardCard, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(270, 100));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        JPanel statusCard = new JPanel();
        statusCard.setBackground(Color.WHITE);
        statusCard.setBorder(new CompoundBorder(new LineBorder(new Color(222,229,239),1,true),
                new EmptyBorder(12, 12, 12, 12)));
        statusCard.setLayout(new BoxLayout(statusCard, BoxLayout.Y_AXIS));
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        turnLabel.setForeground(new Color(35, 58, 91));
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        scoreLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        scoreLabel.setForeground(new Color(75, 91, 115));
        scoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(91, 111, 137));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusCard.add(turnLabel);
        statusCard.add(Box.createVerticalStrut(10));
        statusCard.add(scoreLabel);
        statusCard.add(Box.createVerticalStrut(10));
        statusCard.add(statusLabel);
        side.add(statusCard);
        side.add(Box.createVerticalStrut(14));

        JLabel moveHeading = sectionLabel("MOVE CONTROLS");
        side.add(moveHeading);
        side.add(Box.createVerticalStrut(8));
        JPanel moveGrid = new JPanel(new GridLayout(2, 2, 7, 7));
        moveGrid.setOpaque(false);
        JButton up = button("↑  Up"), down = button("↓  Down");
        JButton left = button("←  Left"), right = button("Right  →");
        up.addActionListener(e -> moveBy(-1, 0));
        down.addActionListener(e -> moveBy(1, 0));
        left.addActionListener(e -> moveBy(0, -1));
        right.addActionListener(e -> moveBy(0, 1));
        moveGrid.add(up); moveGrid.add(left); moveGrid.add(right); moveGrid.add(down);
        moveGrid.setMaximumSize(new Dimension(270, 82));
        moveGrid.setPreferredSize(new Dimension(270, 82));
        side.add(moveGrid);
        side.add(Box.createVerticalStrut(14));

        JLabel actionHeading = sectionLabel("GAME ACTIONS");
        side.add(actionHeading);
        side.add(Box.createVerticalStrut(8));
        JButton undo = button("↶  Undo Last Move");
        undo.addActionListener(e -> undoMove());
        JButton newGame = button("＋  New Game");
        newGame.addActionListener(e -> startGame(playerSelector.getSelectedIndex() + 2));
        JButton showHistory = button("☷  Show Full History");
        showHistory.addActionListener(e -> showHistoryDialog());
        side.add(undo); side.add(Box.createVerticalStrut(7));
        side.add(newGame); side.add(Box.createVerticalStrut(7));
        side.add(showHistory);
        side.add(Box.createVerticalStrut(14));

        JLabel inventoryTitle = sectionLabel("COLLECTION & POWER-UPS");
        side.add(inventoryTitle);
        side.add(Box.createVerticalStrut(8));
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        historyArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        historyArea.setForeground(new Color(66, 83, 108));
        historyArea.setBackground(Color.WHITE);
        historyArea.setBorder(new EmptyBorder(9, 9, 9, 9));
        JScrollPane invScroll = new JScrollPane(historyArea);
        invScroll.setPreferredSize(new Dimension(260, 105));
        invScroll.setMaximumSize(new Dimension(270, 120));
        invScroll.setBorder(new LineBorder(new Color(222, 229, 239), 1, true));
        side.add(invScroll);

        center.add(side, BorderLayout.EAST);
        page.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("Move one cell at a time  •  Collect tokens  •  Use Undo to reverse your last move",
                SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(new Color(125, 139, 158));
        page.add(footer, BorderLayout.SOUTH);
        add(page, BorderLayout.CENTER);

        // Start a playable default game immediately when the application opens.
        // The player dropdown remains available to choose another count and restart.
        startGame(2);
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(new Color(102, 119, 143));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(270, 36));
        b.setPreferredSize(new Dimension(125, 36));
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(new Color(45, 66, 96));
        b.setBackground(new Color(235, 241, 249));
        b.setBorder(new CompoundBorder(new LineBorder(new Color(211, 222, 237), 1, true),
                new EmptyBorder(7, 9, 7, 9)));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void startGame(int selectedPlayers) {
        players = Math.max(2, Math.min(8, selectedPlayers));
        row = new int[players]; col = new int[players]; scores = new int[players];
        shield = new boolean[players]; doubleScore = new boolean[players]; extraLives = new int[players];
        Arrays.fill(scores, 0);
        Arrays.fill(shield, false);
        Arrays.fill(doubleScore, false);
        Arrays.fill(extraLives, 0);
        Arrays.fill(row, -1);
        Arrays.fill(col, -1);
        for (int r = 0; r < N; r++) {
            Arrays.fill(tokens[r], 0);
            Arrays.fill(specials[r], "");
        }
        history.clear(); historyModel.clear(); undoStack.clear();
        collected.clear(); powerUps.clear();
        current = 0; turnCount = 0; gameOver = false;

        // Place tokens and special spaces, keeping the starting cells clear.
        for (int i = 0; i < 18; i++) {
            int r, c;
            do { r = random.nextInt(N); c = random.nextInt(N); }
            while (tokens[r][c] != 0 || !specials[r][c].isEmpty() || r == 0);
            tokens[r][c] = 1 + random.nextInt(10);
        }
        String[] types = {"SHIELD", "SPEED", "DOUBLE", "LIFE", "CHALLENGE"};
        for (String type : types) {
            int r, c;
            do { r = random.nextInt(N); c = random.nextInt(N); }
            while (tokens[r][c] != 0 || !specials[r][c].isEmpty() || r == 0);
            specials[r][c] = type;
        }
        for (int p = 0; p < players; p++) {
            // Each player begins on a different top-edge cell; the first row is kept clear.
            row[p] = 0;
            col[p] = p;
        }
        addHistory("Game started with " + players + " players.");
        statusLabel.setText("Game started. Move to an adjacent cell.");
        refreshBoard();
        refreshLabels();
    }

    private void moveBy(int dr, int dc) {
        if (gameOver) return;
        moveTo(row[current] + dr, col[current] + dc);
    }

    private void moveTo(int r, int c) {
        if (gameOver) return;
        if (Math.abs(r - row[current]) + Math.abs(c - col[current]) != 1) {
            statusLabel.setText("Choose one adjacent cell only.");
            return;
        }
        if (r < 0 || r >= N || c < 0 || c >= N) {
            statusLabel.setText("That move is outside the board.");
            return;
        }
        saveState();
        int p = current;
        int oldR = row[p], oldC = col[p];
        row[p] = r; col[p] = c;
        turnCount++;
        String event = "P" + (p + 1) + " moved (" + oldR + "," + oldC + ") → (" + r + "," + c + ")";
        if (tokens[r][c] > 0) {
            int value = tokens[r][c];
            scores[p] += doubleScore[p] ? value * 2 : value;
            doubleScore[p] = false;
            collected.add(value);
            event += " and collected token " + value + " (+" + value + ")";
            tokens[r][c] = 0;
        }
        if (!specials[r][c].isEmpty()) {
            String type = specials[r][c];
            specials[r][c] = "";
            switch (type) {
                case "SHIELD" -> { shield[p] = true; powerUps.add("P" + (p+1) + ": Shield"); event += " — gained Shield"; }
                case "SPEED" -> { scores[p] += 2; powerUps.add("P" + (p+1) + ": Speed"); event += " — Speed bonus (+2)"; }
                case "DOUBLE" -> { doubleScore[p] = true; powerUps.add("P" + (p+1) + ": Double Score"); event += " — next token doubled"; }
                case "LIFE" -> { extraLives[p]++; powerUps.add("P" + (p+1) + ": Extra Life"); event += " — gained Extra Life"; }
                case "CHALLENGE" -> {
                    if (shield[p]) { shield[p] = false; event += " — Shield blocked challenge"; }
                    else if (extraLives[p] > 0) { extraLives[p]--; event += " — Extra Life protected you"; }
                    else { scores[p] = Math.max(0, scores[p] - 3); event += " — challenge penalty (-3)"; }
                }
            }
        }
        addHistory(event);
        statusLabel.setText(event);
        current = (current + 1) % players;
        refreshBoard();
        refreshLabels();
        if (turnCount >= 40 || noTokensOrSpecials()) finishGame();
    }

    private boolean noTokensOrSpecials() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (tokens[r][c] > 0 || !specials[r][c].isEmpty()) return false;
        return true;
    }

    private void saveState() {
        undoStack.push(new State(tokens, specials, row, col, scores, shield, doubleScore, extraLives,
                current, turnCount, collected, powerUps, history));
    }

    private void undoMove() {
        if (gameOver || undoStack.isEmpty()) {
            statusLabel.setText("No move available to undo.");
            return;
        }
        State s = undoStack.pop();
        s.restore(tokens, specials, row, col, scores, shield, doubleScore, extraLives);
        current = s.current; turnCount = s.turnCount;
        collected.clear(); collected.addAll(s.collected);
        powerUps.clear(); powerUps.addAll(s.powerUps);
        history.clear(); history.addAll(s.history);
        historyModel.clear();
        for (String h : history) historyModel.addElement(h);
        statusLabel.setText("Last move undone.");
        refreshBoard(); refreshLabels(); refreshInventory();
    }

    private void addHistory(String s) {
        history.add(s);
        historyModel.addElement(s);
    }

    private void refreshBoard() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                StringBuilder label = new StringBuilder();
                Color color = EMPTY;
                if (tokens[r][c] > 0) {
                    label.append(tokens[r][c]);
                    color = TOKEN;
                } else if (!specials[r][c].isEmpty()) {
                    label.append(specials[r][c].equals("CHALLENGE") ? "!" : "★");
                    color = SPECIAL;
                }
                java.util.List<String> occupants = new ArrayList<>();
                for (int p = 0; p < players; p++)
                    if (row[p] == r && col[p] == c) occupants.add("P" + (p + 1));
                if (!occupants.isEmpty()) {
                    if (label.length() > 0) label.append("\n");
                    label.append(String.join("/", occupants));
                    color = PLAYER;
                }
                JButton cell = cells[r][c];
                String newText = "<html><center>" + label.toString().replace("\\n", "<br>") + "</center></html>";
                String newTip = tokens[r][c] > 0 ? "Token worth " + tokens[r][c] :
                        (!specials[r][c].isEmpty() ? "Special: " + specials[r][c] : "Empty cell");

                // Skip unchanged Swing properties to reduce unnecessary repaint work.
                if (!newText.equals(cell.getText())) cell.setText(newText);
                if (!color.equals(cell.getBackground())) cell.setBackground(color);
                if (cell.isEnabled() != !gameOver) cell.setEnabled(!gameOver);
                if (!java.util.Objects.equals(newTip, cell.getToolTipText())) cell.setToolTipText(newTip);
            }
        }
    }

    private void refreshLabels() {
        if (gameOver) return;
        turnLabel.setText("Current turn: Player " + (current + 1));
        StringBuilder sb = new StringBuilder("<html><center>");
        for (int p = 0; p < players; p++) {
            if (p > 0) sb.append("<br>");
            sb.append("P").append(p + 1).append(": ").append(scores[p]).append(" points");
        }
        sb.append("</center></html>");
        scoreLabel.setText(sb.toString());
        refreshInventory();
    }

    private void refreshInventory() {
        StringBuilder s = new StringBuilder();
        s.append("Tokens collected: ").append(collected).append("\n");
        s.append("Power-ups collected:\n");
        if (powerUps.isEmpty()) s.append("  None yet\n");
        else for (String p : powerUps) s.append("  • ").append(p).append("\n");
        for (int p = 0; p < players; p++) {
            s.append("P").append(p + 1).append(": ");
            if (shield[p]) s.append("Shield ");
            if (doubleScore[p]) s.append("Double Score ");
            if (extraLives[p] > 0) s.append("Extra Life x").append(extraLives[p]).append(" ");
            s.append("\n");
        }
        String inventoryText = s.toString();
        if (!inventoryText.equals(historyArea.getText())) {
            historyArea.setText(inventoryText);
            historyArea.setCaretPosition(0);
        }
    }

    private void showHistoryDialog() {
        JTextArea area = new JTextArea(20, 42);
        area.setEditable(false);
        for (int i = 0; i < history.size(); i++) area.append((i + 1) + ". " + history.get(i) + "\n");
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Game History / Replay Log",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void finishGame() {
        gameOver = true;
        refreshBoard();
        int best = Integer.MIN_VALUE;
        for (int p = 0; p < players; p++) best = Math.max(best, scores[p]);
        StringBuilder result = new StringBuilder("Game Completed!\n\n");
        for (int p = 0; p < players; p++) result.append("Player ").append(p + 1)
                .append(": ").append(scores[p]).append(" points\n");
        java.util.List<Integer> winners = new ArrayList<>();
        for (int p = 0; p < players; p++) if (scores[p] == best) winners.add(p + 1);
        result.append("\nWinner").append(winners.size() > 1 ? "s" : "").append(": ");
        for (int i = 0; i < winners.size(); i++) {
            if (i > 0) result.append(", ");
            result.append("Player ").append(winners.get(i));
        }
        turnLabel.setText("Game Completed");
        statusLabel.setText("Start a new game to play again.");
        JOptionPane.showMessageDialog(this, result.toString(), "Final Result", JOptionPane.INFORMATION_MESSAGE);
    }

    private static class State {
        final int[][] tokens;
        final String[][] specials;
        final int[] row, col, scores, extraLives;
        final boolean[] shield, doubleScore;
        final int current, turnCount;
        final java.util.List<Integer> collected;
        final java.util.List<String> powerUps, history;

        State(int[][] t, String[][] sp, int[] r, int[] c, int[] sc, boolean[] sh,
              boolean[] ds, int[] lives, int cur, int tc, java.util.List<Integer> coll,
              java.util.List<String> pu, java.util.List<String> hist) {
            tokens = copy(t); specials = copy(sp);
            row = r.clone(); col = c.clone(); scores = sc.clone();
            shield = sh.clone(); doubleScore = ds.clone(); extraLives = lives.clone();
            current = cur; turnCount = tc;
            collected = new ArrayList<>(coll); powerUps = new ArrayList<>(pu);
            history = new ArrayList<>(hist);
        }
        void restore(int[][] t, String[][] sp, int[] r, int[] c, int[] sc, boolean[] sh,
                     boolean[] ds, int[] lives) {
            copyInto(tokens, t); copyInto(specials, sp);
            System.arraycopy(row, 0, r, 0, row.length);
            System.arraycopy(col, 0, c, 0, col.length);
            System.arraycopy(scores, 0, sc, 0, scores.length);
            System.arraycopy(shield, 0, sh, 0, shield.length);
            System.arraycopy(doubleScore, 0, ds, 0, doubleScore.length);
            System.arraycopy(extraLives, 0, lives, 0, extraLives.length);
        }
        static int[][] copy(int[][] a) { int[][] b = new int[a.length][]; for(int i=0;i<a.length;i++) b[i]=a[i].clone(); return b; }
        static String[][] copy(String[][] a) { String[][] b = new String[a.length][]; for(int i=0;i<a.length;i++) b[i]=a[i].clone(); return b; }
        static void copyInto(int[][] from, int[][] to) { for(int i=0;i<from.length;i++) System.arraycopy(from[i],0,to[i],0,from[i].length); }
        static void copyInto(String[][] from, String[][] to) { for(int i=0;i<from.length;i++) System.arraycopy(from[i],0,to[i],0,from[i].length); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FindAndSortApp().setVisible(true));
    }
}
