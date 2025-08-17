import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.text.DecimalFormat;

public class ScientificCalculator extends JFrame implements ActionListener, KeyListener {
    private final JTextField display = new JTextField("0");

    // Calculator state
    private double current = 0.0;
    private String pendingOp = null;
    private boolean startNewNumber = true;
    private boolean degMode = true; // true = degrees, false = radians
    private double memory = 0.0;

    private final DecimalFormat fmt = new DecimalFormat("###############.###############");

    public ScientificCalculator() {
        super("Scientific Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setFont(new Font("Consolas", Font.PLAIN, 28));
        display.setEditable(false);
        display.addKeyListener(this);
        add(display, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(7, 6, 6, 6));

        // Row 1: Memory + mode
        addButtons(buttons,
                "MC", "MR", "M+", "M-", "DEG", "AC");

        // Row 2: constants & edits
        addButtons(buttons,
                "π", "e", "x²", "√", "1/x", "DEL");

        // Row 3: trig & logs
        addButtons(buttons,
                "sin", "cos", "tan", "ln", "log", "!" );

        // Row 4: parentheses-like ops (we'll use +/- as sign), power and divide
        addButtons(buttons,
                "+/-", "^", "/", "*", "-", "+");

        // Row 5-7: digits and controls
        addButtons(buttons,
                "7", "8", "9", "%", "C", "​" /* spacer */);
        addButtons(buttons,
                "4", "5", "6", " ", " ", "=" );
        addButtons(buttons,
                "1", "2", "3", "0", ".", "=" );

        // Replace spacers with disabled labels for alignment
        for (Component c : buttons.getComponents()) {
            if (c instanceof JButton) {
                JButton b = (JButton) c;
                if (b.getText().trim().isEmpty()) {
                    b.setEnabled(false);
                }
            }
        }

        add(buttons, BorderLayout.CENTER);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        getRootPane().setDefaultButton(new JButton("=")); // for visual consistency

        // Global key bindings for Enter = equals, Escape = clear
        InputMap im = buttons.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = buttons.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "EQUALS");
        am.put("EQUALS", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { equalsAction(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLEAR");
        am.put("CLEAR", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { clearEntry(); }
        });
    }

    private void addButtons(JPanel panel, String... labels) {
        for (String label : labels) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Inter", Font.PLAIN, 18));
            btn.setFocusPainted(false);
            btn.addActionListener(this);
            panel.add(btn);
        }
    }

    // Helpers
    private double getDisplayValue() {
        try {
            return Double.parseDouble(display.getText());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private void setDisplay(double value) {
        if (Double.isNaN(value)) {
            display.setText("NaN");
        } else if (Double.isInfinite(value)) {
            display.setText(value > 0 ? "∞" : "-∞");
        } else {
            display.setText(fmt.format(value));
        }
        startNewNumber = true;
    }

    private void appendDigit(String d) {
        if (startNewNumber || display.getText().equals("0")) {
            if (d.equals(".")) {
                display.setText("0.");
            } else {
                display.setText(d);
            }
            startNewNumber = false;
        } else {
            if (d.equals(".") && display.getText().contains(".")) return;
            display.setText(display.getText() + d);
        }
    }

    private void clearAll() {
        current = 0.0;
        pendingOp = null;
        startNewNumber = true;
        display.setText("0");
    }

    private void clearEntry() {
        startNewNumber = true;
        display.setText("0");
    }

    private void backspace() {
        if (!startNewNumber) {
            String t = display.getText();
            if (t.length() > 1) {
                display.setText(t.substring(0, t.length() - 1));
            } else {
                display.setText("0");
                startNewNumber = true;
            }
        }
    }

    private void setOperator(String op) {
        double x = getDisplayValue();
        if (pendingOp == null) {
            current = x;
        } else {
            current = applyBinary(pendingOp, current, x);
        }
        pendingOp = op;
        startNewNumber = true;
        setDisplay(current);
        startNewNumber = true; // keep ready for next number
    }

    private void equalsAction() {
        if (pendingOp != null) {
            double x = getDisplayValue();
            double res = applyBinary(pendingOp, current, x);
            setDisplay(res);
            current = res;
            pendingOp = null;
            startNewNumber = true;
        }
    }

    private double toRadians(double v) { return degMode ? Math.toRadians(v) : v; }

    private double applyBinary(String op, double a, double b) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return b == 0 ? Double.NaN : a / b;
            case "^": return Math.pow(a, b);
            case "%": return a % b;
            default: return b;
        }
    }

    private double factorial(double n) {
        if (n < 0) return Double.NaN;
        if (Math.abs(n - Math.rint(n)) > 1e-12) return Double.NaN; // only integers
        int k = (int)Math.rint(n);
        if (k > 20) return Double.NaN; // avoid overflow
        double r = 1.0;
        for (int i = 2; i <= k; i++) r *= i;
        return r;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = ((JButton)e.getSource()).getText();
        switch (cmd) {
            // digits
            case "0": case "1": case "2": case "3": case "4":
            case "5": case "6": case "7": case "8": case "9":
                appendDigit(cmd); break;
            case ".": appendDigit("."); break;

            // binary operators
            case "+": case "-": case "*": case "/": case "^": case "%":
                setOperator(cmd); break;

            case "=": equalsAction(); break;

            // memory
            case "MC": memory = 0.0; break;
            case "MR": setDisplay(memory); break;
            case "M+": memory += getDisplayValue(); break;
            case "M-": memory -= getDisplayValue(); break;

            // mode toggle
            case "DEG": degMode = !degMode; ((JButton)e.getSource()).setText(degMode ? "DEG" : "RAD"); break;

            // edits
            case "AC": clearAll(); break;
            case "C": clearEntry(); break;
            case "DEL": backspace(); break;

            // constants
            case "π": setDisplay(Math.PI); break;
            case "e": setDisplay(Math.E); break;

            // unary
            case "x²": setDisplay(Math.pow(getDisplayValue(), 2)); break;
            case "√": setDisplay(getDisplayValue() < 0 ? Double.NaN : Math.sqrt(getDisplayValue())); break;
            case "1/x": {
                double v = getDisplayValue();
                setDisplay(v == 0 ? Double.NaN : 1.0 / v);
                break;
            }
            case "+/-": setDisplay(-getDisplayValue()); break;
            case "sin": setDisplay(Math.sin(toRadians(getDisplayValue()))); break;
            case "cos": setDisplay(Math.cos(toRadians(getDisplayValue()))); break;
            case "tan": setDisplay(Math.tan(toRadians(getDisplayValue()))); break;
            case "ln": {
                double v = getDisplayValue();
                setDisplay(v <= 0 ? Double.NaN : Math.log(v));
                break;
            }
            case "log": {
                double v = getDisplayValue();
                setDisplay(v <= 0 ? Double.NaN : Math.log10(v));
                break;
            }
            case "!": setDisplay(factorial(getDisplayValue())); break;

            default: break; // ignore spacers
        }
    }

    // Basic keyboard input support
    @Override public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (Character.isDigit(c)) { appendDigit(String.valueOf(c)); return; }
        switch (c) {
            case '.': appendDigit("."); break;
            case '+': case '-': case '*': case '/': setOperator(String.valueOf(c)); break;
            case '^': setOperator("^"); break;
            case '\n': case '=': equalsAction(); break;
            case '\b': backspace(); break;
        }
    }
    @Override public void keyPressed(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScientificCalculator().setVisible(true));
    }
}
