import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

public class LudoGame extends JFrame {
    private static final int CELL = 40;
    private JPanel boardPanel;
    private JLabel messageLabel;
    private DicePanel dicePanel;
    private JPanel playerPanel;
    private JButton rollButton;

    private Map<String, List<Token>> tokens;
    private List<String> activeColors;
    private int currentPlayerIndex = 0;
    private int diceValue = 1;
    private String winner = null;
    private boolean isRolling = false;
    private boolean canMove = false;
    private boolean capturedToken = false; // NEW: Track if token was captured

    private static final String[] COLORS = {"red", "green", "yellow", "blue"};
    private static final Color[] AWTCOLORS = {
        new Color(220, 20, 60), new Color(34, 139, 34),
        new Color(255, 215, 0), new Color(30, 144, 255)
    };

    private static final int[][][] HOME_POS = {
        {{10,1},{12,1},{10,3},{12,3}},
        {{1,1},{3,1},{1,3},{3,3}},
        {{1,10},{3,10},{1,12},{3,12}},
        {{10,10},{12,10},{10,12},{12,12}}
    };

    private static final int[][] SAFE_SPOTS = {
        {8,1},{1,6},{6,13},{13,8},
        {12,6},{6,2},{2,8},{8,12}
    };

    private static int[][][] PATHS;

    static {
        int[][] redPath = {
            {8,1},{8,2},{8,3},{8,4},{8,5},
            {9,6},{10,6},{11,6},{12,6},{13,6},{14,6},
            {14,7},{14,8},
            {13,8},{12,8},{11,8},{10,8},{9,8},
            {8,9},{8,10},{8,11},{8,12},{8,13},{8,14},
            {7,14},{6,14},
            {6,13},{6,12},{6,11},{6,10},{6,9},
            {5,8},{4,8},{3,8},{2,8},{1,8},{0,8},
            {0,7},{0,6},
            {1,6},{2,6},{3,6},{4,6},{5,6},
            {6,5},{6,4},{6,3},{6,2},{6,1},{6,0},
            {7,0},
            {7,1},{7,2},{7,3},{7,4},{7,5},{7,6}
        };

        PATHS = new int[4][57][2];
        for (int i = 0; i < 57; i++) {
            PATHS[0][i] = redPath[i].clone();
        }

        for (int p = 1; p < 4; p++) {
            for (int i = 0; i < 57; i++) {
                int x = PATHS[0][i][0];
                int y = PATHS[0][i][1];
                for (int r = 0; r < p; r++) {
                    int temp = x;
                    x = y;
                    y = 14 - temp;
                }
                PATHS[p][i] = new int[]{x, y};
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new LudoGame();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LudoGame() {
        initGame();
        initUI();
    }

    private void initGame() {
        activeColors = Arrays.asList(COLORS);
        tokens = new HashMap<>();

        for (int c = 0; c < 4; c++) {
            List<Token> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                list.add(new Token(HOME_POS[c][i][0], HOME_POS[c][i][1], c));
            }
            tokens.put(COLORS[c], list);
        }

        currentPlayerIndex = 0;
        diceValue = 1;
        winner = null;
        canMove = false;
        capturedToken = false;
    }

    private void initUI() {
        setTitle("Ludo Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(26, 26, 46));

        messageLabel = new JLabel("RED's turn - Roll the dice!", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 18));
        messageLabel.setForeground(AWTCOLORS[0]);
        messageLabel.setOpaque(true);
        messageLabel.setBackground(new Color(26, 26, 46));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));

        boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBoard((Graphics2D) g);
            }
        };
        boardPanel.setPreferredSize(new Dimension(15 * CELL, 15 * CELL));
        boardPanel.setBackground(new Color(26, 26, 46));
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (canMove && !isRolling) {
                    handleClick(e.getX(), e.getY());
                }
            }
        });

        dicePanel = new DicePanel();
        dicePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        rollButton = new JButton("Roll Dice");
        rollButton.setFont(new Font("Arial", Font.BOLD, 16));
        rollButton.setForeground(Color.WHITE);
        rollButton.setFocusPainted(false);
        rollButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        rollButton.setBackground(AWTCOLORS[currentPlayerIndex]);
        rollButton.setPreferredSize(new Dimension(120, 45));

        rollButton.addActionListener(e -> {
            if (!isRolling && !canMove && winner == null) {
                rollDice();
            }
        });
        updateRollButton();

        JPanel diceAndButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        diceAndButtonPanel.setBackground(new Color(26, 26, 46));
        diceAndButtonPanel.add(dicePanel);
        diceAndButtonPanel.add(rollButton);

        playerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        playerPanel.setBackground(new Color(26, 26, 46));
        for (int i = 0; i < 4; i++) {
            JLabel lbl = new JLabel(COLORS[i].toUpperCase());
            lbl.setFont(new Font("Arial", Font.BOLD, 12));
            lbl.setOpaque(true);
            lbl.setBackground(i == 0 ? AWTCOLORS[i] : new Color(50, 50, 50));
            lbl.setForeground(i == 0 ? Color.WHITE : new Color(150, 150, 150));
            lbl.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
            playerPanel.add(lbl);
        }

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(new Color(26, 26, 46));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 0));
        controlPanel.add(diceAndButtonPanel);
        controlPanel.add(Box.createVerticalStrut(10));
        controlPanel.add(playerPanel);

        add(messageLabel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }

    private void updateRollButton() {
        if (winner != null || canMove) {
            rollButton.setEnabled(false);
            rollButton.setBackground(new Color(100, 100, 100));
            rollButton.setText(winner != null ? "Game Over" : "Select Token");
            dicePanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else {
            rollButton.setEnabled(!isRolling);
            rollButton.setBackground(AWTCOLORS[currentPlayerIndex]);
            rollButton.setText(isRolling ? "Rolling..." : "Roll Dice");
            dicePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private void rollDice() {
        isRolling = true;
        capturedToken = false; // Reset capture flag
        updateRollButton();

        final int[] count = {0};
        final Random rand = new Random();

        Timer timer = new Timer(80, null);
        timer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count[0] < 10) {
                    diceValue = rand.nextInt(6) + 1;
                    dicePanel.setValue(diceValue);
                    dicePanel.repaint();
                    count[0]++;
                } else {
                    timer.stop();
                    diceValue = rand.nextInt(6) + 1;
                    dicePanel.setValue(diceValue);
                    dicePanel.repaint();
                    isRolling = false;
                    afterRoll();
                    updateRollButton();
                }
            }
        });
        timer.start();
    }

    private void afterRoll() {
        String color = COLORS[currentPlayerIndex];
        boolean hasMove = false;

        for (Token t : tokens.get(color)) {
            if (canTokenMove(t)) {
                hasMove = true;
                break;
            }
        }

        if (hasMove) {
            canMove = true;
            setMessage(color.toUpperCase() + " rolled " + diceValue + " - Select token!");
            updateRollButton();
        } else {
            setMessage(color.toUpperCase() + " rolled " + diceValue + " - No moves");
            Timer t = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((Timer)e.getSource()).stop();
                    if (diceValue != 6) {
                        nextPlayer();
                    } else {
                        setMessage(COLORS[currentPlayerIndex].toUpperCase() + " - Roll again!");
                    }
                    updateRollButton();
                }
            });
            t.start();
        }
    }

    private boolean canTokenMove(Token t) {
        if (t.isHome) return false;
        if (t.pathIndex == -1 && diceValue == 6) return true;
        if (t.pathIndex >= 0 && t.pathIndex + diceValue <= 56) return true;
        return false;
    }

    private void handleClick(int mx, int my) {
        String color = COLORS[currentPlayerIndex];
        List<Token> list = tokens.get(color);

        for (int i = 0; i < list.size(); i++) {
            Token t = list.get(i);
            if (t.isHome) continue;

            int tx, ty;
            if (t.pathIndex == -1) {
                tx = t.homeX * CELL + CELL / 2;
                ty = t.homeY * CELL + CELL / 2;
            } else {
                tx = PATHS[t.colorIndex][t.pathIndex][0] * CELL + CELL / 2;
                ty = PATHS[t.colorIndex][t.pathIndex][1] * CELL + CELL / 2;
            }

            double dist = Math.sqrt((mx - tx) * (mx - tx) + (my - ty) * (my - ty));
            if (dist <= CELL / 2 && canTokenMove(t)) {
                moveToken(t);
                return;
            }
        }
    }

    private void moveToken(Token t) {
        canMove = false;
        String color = COLORS[currentPlayerIndex];

        if (t.pathIndex == -1) {
            t.pathIndex = 0;
        } else {
            t.pathIndex += diceValue;
        }

        if (t.pathIndex >= 56) {
            t.isHome = true;
            t.pathIndex = 56; // FIXED: Keep pathIndex at 56 so token stays visible at home position
            setMessage("Token reached home!");

            boolean allHome = true;
            for (Token tk : tokens.get(color)) {
                if (!tk.isHome) allHome = false;
            }
            if (allHome) {
                winner = color;
                setMessage(color.toUpperCase() + " WINS! 🏆");
                boardPanel.repaint();
                updateRollButton();
                return;
            }
        } else if (t.pathIndex < 51) {
            int[] pos = PATHS[t.colorIndex][t.pathIndex];
            boolean safe = isSafe(pos[0], pos[1]);

            if (!safe) {
                for (int c = 0; c < 4; c++) {
                    if (c == currentPlayerIndex) continue;
                    for (Token ot : tokens.get(COLORS[c])) {
                        if (!ot.isHome && ot.pathIndex >= 0 && ot.pathIndex < 51) {
                            int[] opos = PATHS[ot.colorIndex][ot.pathIndex];
                            if (opos[0] == pos[0] && opos[1] == pos[1]) {
                                ot.pathIndex = -1;
                                capturedToken = true; // FIXED: Set flag when capturing
                                setMessage(color.toUpperCase() + " captured " + COLORS[c].toUpperCase() + "! ⚔");
                            }
                        }
                    }
                }
            }
        }

        boardPanel.repaint();

        Timer timer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((Timer)e.getSource()).stop();
                if (winner != null) return;
                
                // FIXED: Roll again if got 6, captured token, or reached home
                if (diceValue == 6 || capturedToken || (t.isHome && t.pathIndex == 56)) {
                    setMessage(COLORS[currentPlayerIndex].toUpperCase() + " - Roll again!");
                } else {
                    nextPlayer();
                }
                updateRollButton();
            }
        });
        timer.start();
    }

    private boolean isSafe(int x, int y) {
        for (int[] s : SAFE_SPOTS) {
            if (s[0] == x && s[1] == y) return true;
        }
        return false;
    }

    private void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        setMessage(COLORS[currentPlayerIndex].toUpperCase() + "'s turn - Roll the dice!");
        dicePanel.borderColor = AWTCOLORS[currentPlayerIndex];
        dicePanel.repaint();
        updatePlayers();
        updateRollButton();
    }

    private void setMessage(String msg) {
        messageLabel.setText(msg);
        messageLabel.setForeground(AWTCOLORS[currentPlayerIndex]);
    }

    private void updatePlayers() {
        for (int i = 0; i < 4; i++) {
            JLabel lbl = (JLabel) playerPanel.getComponent(i);
            lbl.setBackground(i == currentPlayerIndex ? AWTCOLORS[i] : new Color(50, 50, 50));
            lbl.setForeground(i == currentPlayerIndex ? Color.WHITE : new Color(150, 150, 150));
        }
    }

    private void drawBoard(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(42, 42, 58));
        g.fillRect(0, 0, 15 * CELL, 15 * CELL);

        g.setColor(new Color(245, 245, 220));
        for (int y = 6; y <= 8; y++) {
            for (int x = 0; x < 6; x++) g.fillRect(x * CELL, y * CELL, CELL, CELL);
            for (int x = 9; x < 15; x++) g.fillRect(x * CELL, y * CELL, CELL, CELL);
        }
        for (int x = 6; x <= 8; x++) {
            for (int y = 0; y < 6; y++) g.fillRect(x * CELL, y * CELL, CELL, CELL);
            for (int y = 9; y < 15; y++) g.fillRect(x * CELL, y * CELL, CELL, CELL);
        }

        int[][] qpos = {{9,0},{0,0},{0,9},{9,9}};
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(AWTCOLORS[i].getRed(), AWTCOLORS[i].getGreen(), AWTCOLORS[i].getBlue(), 200));
            g.fillRect(qpos[i][0] * CELL, qpos[i][1] * CELL, 6 * CELL, 6 * CELL);
            g.setColor(new Color(34, 34, 50));
            g.fillRoundRect(qpos[i][0] * CELL + CELL/2, qpos[i][1] * CELL + CELL/2, 5 * CELL, 5 * CELL, 15, 15);
        }

        Color[] light = {new Color(255,150,150), new Color(150,255,150), new Color(255,255,150), new Color(150,200,255)};
        g.setColor(light[0]); for (int y = 1; y <= 5; y++) g.fillRect(7*CELL+3, y*CELL+3, CELL-6, CELL-6);
        g.setColor(light[1]); for (int x = 1; x <= 5; x++) g.fillRect(x*CELL+3, 7*CELL+3, CELL-6, CELL-6);
        g.setColor(light[2]); for (int y = 9; y <= 13; y++) g.fillRect(7*CELL+3, y*CELL+3, CELL-6, CELL-6);
        g.setColor(light[3]); for (int x = 9; x <= 13; x++) g.fillRect(x*CELL+3, 7*CELL+3, CELL-6, CELL-6);

        int cx = 6 * CELL;
        int cy = 6 * CELL;
        int s = 3 * CELL;
        int centerX = cx + s / 2;
        int centerY = cy + s / 2;
        
        g.setColor(AWTCOLORS[0]);
        g.fillPolygon(new int[]{cx, cx + s, centerX}, new int[]{cy, cy, centerY}, 3);
        
        g.setColor(AWTCOLORS[1]);
        g.fillPolygon(new int[]{cx, cx, centerX}, new int[]{cy, cy + s, centerY}, 3);
        
        g.setColor(AWTCOLORS[2]);
        g.fillPolygon(new int[]{cx, cx + s, centerX}, new int[]{cy + s, cy + s, centerY}, 3);
        
        g.setColor(AWTCOLORS[3]);
        g.fillPolygon(new int[]{cx + s, cx + s, centerX}, new int[]{cy, cy + s, centerY}, 3);

        g.setColor(new Color(80, 80, 100));
        for (int i = 0; i <= 15; i++) {
            g.drawLine(0, i*CELL, 15*CELL, i*CELL);
            g.drawLine(i*CELL, 0, i*CELL, 15*CELL);
        }

        g.setColor(new Color(255, 215, 0));
        for (int[] sp : SAFE_SPOTS) drawStar(g, sp[0]*CELL+CELL/2, sp[1]*CELL+CELL/2, 12);

        Map<String, List<Token>> cellOccupancy = new HashMap<>();
        for (int c = 0; c < 4; c++) {
            for (Token t : tokens.get(COLORS[c])) {
                // FIXED: Draw tokens even if they're home (isHome = true)
                int tx, ty;
                if (t.pathIndex == -1) {
                    tx = t.homeX * CELL + CELL/2;
                    ty = t.homeY * CELL + CELL/2;
                } else {
                    tx = PATHS[t.colorIndex][t.pathIndex][0] * CELL + CELL/2;
                    ty = PATHS[t.colorIndex][t.pathIndex][1] * CELL + CELL/2;
                }
                
                String key = tx + "," + ty;
                cellOccupancy.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
            }
        }
        
        for (Map.Entry<String, List<Token>> entry : cellOccupancy.entrySet()) {
            List<Token> stack = entry.getValue();
            int stackSize = stack.size();
            for(int i = 0; i < stackSize; i++) {
                Token t = stack.get(i);
                int tx, ty;
                if (t.pathIndex == -1) {
                    tx = t.homeX * CELL + CELL/2;
                    ty = t.homeY * CELL + CELL/2;
                } else {
                    tx = PATHS[t.colorIndex][t.pathIndex][0] * CELL + CELL/2;
                    ty = PATHS[t.colorIndex][t.pathIndex][1] * CELL + CELL/2;
                }
                
                int offset = stackSize > 1 ? (i - stackSize / 2) * 5 : 0;
                drawMarker(g, tx + offset, ty + offset, AWTCOLORS[t.colorIndex], canTokenMove(t) && t.colorIndex == currentPlayerIndex);
            }
        }
    }

    private void drawStar(Graphics2D g, int x, int y, int r) {
        int[] px = new int[10], py = new int[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI/2 + Math.PI*i/5;
            int rad = (i%2==0) ? r : r/2;
            px[i] = x + (int)(rad * Math.cos(a));
            py[i] = y + (int)(rad * Math.sin(a));
        }
        g.setColor(new Color(255, 215, 0));
        g.fillPolygon(px, py, 10);
        g.setColor(new Color(184, 134, 11));
        g.setStroke(new BasicStroke(1));
        g.drawPolygon(px, py, 10);
    }

    private void drawMarker(Graphics2D g, int x, int y, Color col, boolean selectable) {
        g.setColor(new Color(0,0,0,80));
        g.fillOval(x-8, y+8, 16, 6);

        Path2D p = new Path2D.Double();
        p.moveTo(x, y-16);
        p.curveTo(x+12, y-12, x+12, y, x, y+10);
        p.curveTo(x-12, y, x-12, y-12, x, y-16);
        p.closePath();

        g.setColor(col);
        g.fill(p);
        g.setColor(col.darker());
        g.setStroke(new BasicStroke(2));
        g.draw(p);
        g.setColor(new Color(255,255,255,180));
        g.fillOval(x-5, y-12, 10, 10);
        
        if (selectable) {
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 3}, System.currentTimeMillis() % 1000 / 100.0f));
            g.setColor(Color.WHITE);
            Ellipse2D selectableRing = new Ellipse2D.Double(x - 14, y - 14, 28, 28);
            g.draw(selectableRing);
        }
    }

    class Token {
        int homeX, homeY, colorIndex;
        int pathIndex = -1;
        boolean isHome = false;

        Token(int hx, int hy, int ci) {
            homeX = hx; homeY = hy; colorIndex = ci;
        }
    }

    class DicePanel extends JPanel {
        int value = 1;
        Color borderColor = AWTCOLORS[0];

        DicePanel() {
            setPreferredSize(new Dimension(70, 70));
            setMaximumSize(new Dimension(70, 70));
            setBackground(new Color(26, 26, 46));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setValue(int v) { value = v; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(5, 5, 60, 60, 12, 12);
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(5, 5, 60, 60, 12, 12);

            g2.setColor(Color.BLACK);
            int[][] dots;
            switch (value) {
                case 1: dots = new int[][]{{35,35}}; break;
                case 2: dots = new int[][]{{20,20},{50,50}}; break;
                case 3: dots = new int[][]{{20,20},{35,35},{50,50}}; break;
                case 4: dots = new int[][]{{20,20},{50,20},{20,50},{50,50}}; break;
                case 5: dots = new int[][]{{20,20},{50,20},{35,35},{20,50},{50,50}}; break;
                case 6: dots = new int[][]{{20,20},{20,35},{20,50},{50,20},{50,35},{50,50}}; break;
                default: dots = new int[][]{{35,35}};
            }
            for (int[] d : dots) g2.fillOval(d[0]-5, d[1]-5, 10, 10);
        }
    }
}