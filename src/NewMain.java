import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ArbolAritmetico_GUI_NetBeans extends JFrame {
    private final JTextField txtExpresion = new JTextField("(3+5)*(2-4)");
    private final JButton btnProcesar = new JButton("Calcular");
    private final JTextArea txtTokens = new JTextArea(3, 40);
    private final JTextArea txtPostfija = new JTextArea(2, 40);
    private final JTextArea txtPrefija = new JTextArea(2, 40);
    private final JTextArea txtInOrden = new JTextArea(2, 40);
    private final JTextArea txtPreOrden = new JTextArea(2, 40);
    private final JTextArea txtPostOrden = new JTextArea(2, 40);
    private final JLabel lblResultado = new JLabel("—");
    private final DibujoArbolPanel panelDibujo = new DibujoArbolPanel();
    private Nodo raizActual = null;

    public ArbolAritmetico_GUI_NetBeans() {
        super("Calculadora de Expresiones por Árbol");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Árbol", crearTabArbol());
        setContentPane(tabs);
        acciones();
    }

    private JPanel crearTabArbol() {
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel entrada = new JPanel(new BorderLayout(8, 8));
        JLabel lbl = new JLabel("Escribe la expresión:");
        lbl.setPreferredSize(new Dimension(120, 30));
        txtExpresion.setToolTipText("Ej.: (3+5)*(2-4)/2  o  10+(2*3)");
        btnProcesar.setPreferredSize(new Dimension(130, 36));

        entrada.add(lbl, BorderLayout.WEST);
        entrada.add(txtExpresion, BorderLayout.CENTER);
        entrada.add(btnProcesar, BorderLayout.EAST);

        JPanel salidas = new JPanel(new GridLayout(0, 1, 6, 6));
        for (JTextArea a : new JTextArea[]{txtTokens, txtPostfija, txtPrefija, txtInOrden, txtPreOrden, txtPostOrden}) {
            a.setLineWrap(true); a.setWrapStyleWord(true); a.setEditable(false);
        }

        salidas.add(wrap("Componentes (Tokens)", new JScrollPane(txtTokens)));
        salidas.add(wrap("Notación Postfija (RPN)", new JScrollPane(txtPostfija)));
        salidas.add(wrap("Notación Prefija", new JScrollPane(txtPrefija)));
        salidas.add(wrap("Recorrido InOrden (con paréntesis)", new JScrollPane(txtInOrden)));
        salidas.add(wrap("Recorrido PreOrden", new JScrollPane(txtPreOrden)));
        salidas.add(wrap("Recorrido PostOrden", new JScrollPane(txtPostOrden)));

        JPanel resultadoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel r = new JLabel("Evaluación:");
        r.setFont(r.getFont().deriveFont(Font.BOLD));
        lblResultado.setFont(lblResultado.getFont().deriveFont(Font.BOLD, 18f));
        resultadoPanel.add(r); 
        resultadoPanel.add(lblResultado);

        JPanel izquierda = new JPanel(new BorderLayout(10, 10));
        izquierda.add(entrada, BorderLayout.NORTH);
        izquierda.add(salidas, BorderLayout.CENTER);
        izquierda.add(resultadoPanel, BorderLayout.SOUTH);

        panelDibujo.setBackground(new Color(250, 250, 250));
        panelDibujo.setBorder(new TitledBorder("Representación del Árbol"));
        JScrollPane scrollArbol = new JScrollPane(panelDibujo);
        scrollArbol.setPreferredSize(new Dimension(500, 500));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, scrollArbol);
        split.setResizeWeight(0.48);
        top.add(split, BorderLayout.CENTER);
        return top;
    }

    private JPanel wrap(String titulo, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(titulo));
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void acciones() {
        btnProcesar.addActionListener(e -> procesar());
        txtExpresion.addActionListener(e -> procesar());
    }

    private void procesar() {
        try {
            String expr = txtExpresion.getText().trim();
            List<String> tokens = ArbolAritmetico.tokenizar(expr);
            txtTokens.setText(String.join(" ", tokens));
            
            List<String> post = ArbolAritmetico.infijaAPostfija(tokens);
            txtPostfija.setText(String.join(" ", post));
            
            raizActual = ArbolAritmetico.construirArbol(post);
            
            txtPrefija.setText(String.join(" ", ArbolAritmetico.aPrefija(raizActual)));
            txtInOrden.setText(ArbolAritmetico.aInfijaConParentesis(raizActual));
            txtPreOrden.setText(String.join(" ", ArbolAritmetico.preOrden(raizActual)));
            txtPostOrden.setText(String.join(" ", ArbolAritmetico.postOrden(raizActual)));
            
            double res = ArbolAritmetico.evaluar(raizActual);
            lblResultado.setText(String.valueOf(res));
            
            panelDibujo.setRaiz(raizActual);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ArbolAritmetico_GUI_NetBeans().setVisible(true));
    }
}

class ArbolAritmetico {
    private static final Set<String> OPS = new HashSet<>(Arrays.asList("+","-","*","/"));

    static boolean esOperador(String c) { return OPS.contains(c); }

    static int prioridad(String op) {
        return switch (op) {
            case "+", "-" -> 1;
            case "*", "/" -> 2;
            default -> -1;
        };
    }

    static List<String> tokenizar(String expr) {
        List<String> out = new ArrayList<>();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char ch = expr.charAt(i);
            if (Character.isWhitespace(ch)) continue;
            if (Character.isDigit(ch) || ch == '.') {
                num.append(ch);
            } else {
                if (num.length() > 0) { out.add(num.toString()); num.setLength(0); }
                if ("()+-*/".indexOf(ch) >= 0) {
                    out.add(String.valueOf(ch));
                } else {
                    throw new IllegalArgumentException("Carácter no válido: " + ch);
                }
            }
        }
        if (num.length() > 0) out.add(num.toString());
        return out;
    }

    static List<String> infijaAPostfija(List<String> tokens) {
        Stack<String> pila = new Stack<>();
        List<String> salida = new ArrayList<>();
        for (String t : tokens) {
            if (t.matches("[0-9]+(\\.[0-9]+)?")) {
                salida.add(t);
            } else if (esOperador(t)) {
                while (!pila.isEmpty() && esOperador(pila.peek()) && prioridad(pila.peek()) >= prioridad(t)) {
                    salida.add(pila.pop());
                }
                pila.push(t);
            } else if ("(".equals(t)) {
                pila.push(t);
            } else if (")".equals(t)) {
                while (!pila.isEmpty() && !"(".equals(pila.peek())) {
                    salida.add(pila.pop());
                }
                if (pila.isEmpty()) throw new IllegalArgumentException("Paréntesis desbalanceados");
                pila.pop();
            } else {
                throw new IllegalArgumentException("Token inesperado: " + t);
            }
        }
        while (!pila.isEmpty()) {
            String x = pila.pop();
            if ("(".equals(x) || ")".equals(x)) throw new IllegalArgumentException("Paréntesis desbalanceados");
            salida.add(x);
        }
        return salida;
    }

    static Nodo construirArbol(List<String> postfija) {
        Stack<Nodo> pila = new Stack<>();
        for (String tk : postfija) {
            if (!esOperador(tk)) {
                pila.push(new Nodo(tk));
            } else {
                Nodo der = pila.pop();
                Nodo izq = pila.pop();
                Nodo op = new Nodo(tk);
                op.izquierdo = izq;
                op.derecho = der;
                pila.push(op);
            }
        }
        return pila.peek();
    }

    static double evaluar(Nodo r) {
        if (r == null) return 0;
        if (!esOperador(r.valor)) return Double.parseDouble(r.valor);
        double a = evaluar(r.izquierdo);
        double b = evaluar(r.derecho);
        return switch (r.valor) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> {
                if (b == 0) throw new ArithmeticException("División entre cero");
                yield a / b;
            }
            default -> 0;
        };
    }

    static List<String> preOrden(Nodo r) {
        List<String> out = new ArrayList<>();
        pre(r, out);
        return out;
    }

    private static void pre(Nodo r, List<String> out) {
        if (r == null) return;
        out.add(r.valor);
        pre(r.izquierdo, out);
        pre(r.derecho, out);
    }

    static List<String> postOrden(Nodo r) {
        List<String> out = new ArrayList<>();
        post(r, out);
        return out;
    }

    private static void post(Nodo r, List<String> out) {
        if (r == null) return;
        post(r.izquierdo, out);
        post(r.derecho, out);
        out.add(r.valor);
    }

    static String aInfijaConParentesis(Nodo r) {
        if (r == null) return "";
        if (!esOperador(r.valor)) return r.valor;
        return "(" + aInfijaConParentesis(r.izquierdo) + " " + r.valor + " " + aInfijaConParentesis(r.derecho) + ")";
    }

    static List<String> aPrefija(Nodo r) { return preOrden(r); }
}

class Nodo {
    String valor;
    Nodo izquierdo, derecho;
    Nodo(String v) { this.valor = v; }
}

class DibujoArbolPanel extends JPanel {
    private Nodo raiz;
    private final int radio = 18;
    private final int vGap = 60;

    public void setRaiz(Nodo r) {
        this.raiz = r;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int ancho = (raiz == null) ? 600 : Math.max(600, (int)(getAnchoSubarbol(raiz) * 40));
        int alto = (raiz == null) ? 400 : getAltura(raiz) * (vGap + 20) + 80;
        return new Dimension(ancho, alto);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (raiz == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int ancho = getWidth();
        dibujarNodo(g2, raiz, ancho / 2, 40, getAnchoSubarbol(raiz) * 40);
    }

    private void dibujarNodo(Graphics2D g2, Nodo n, int x, int y, double hGap) {
        if (n.izquierdo != null) {
            int xIzq = (int)(x - hGap / 2);
            int yH = y + vGap;
            g2.drawLine(x, y, xIzq, yH);
            dibujarNodo(g2, n.izquierdo, xIzq, yH, Math.max(40, hGap / 2));
        }
        if (n.derecho != null) {
            int xDer = (int)(x + hGap / 2);
            int yH = y + vGap;
            g2.drawLine(x, y, xDer, yH);
            dibujarNodo(g2, n.derecho, xDer, yH, Math.max(40, hGap / 2));
        }
        g2.setColor(new Color(245, 245, 245));
        g2.fillOval(x - radio, y - radio, radio * 2, radio * 2);
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new java.awt.BasicStroke(2));
        g2.drawOval(x - radio, y - radio, radio * 2, radio * 2);
        FontMetrics fm = g2.getFontMetrics();
        int sw = fm.stringWidth(n.valor);
        int sh = fm.getAscent();
        g2.drawString(n.valor, x - sw / 2, y + sh / 4);
    }

    private int getAltura(Nodo r) {
        if (r == null) return 0;
        return 1 + Math.max(getAltura(r.izquierdo), getAltura(r.derecho));
    }

    private int getAnchoSubarbol(Nodo r) {
        if (r == null) return 0;
        if (r.izquierdo == null && r.derecho == null) return 1;
        return getAnchoSubarbol(r.izquierdo) + getAnchoSubarbol(r.derecho);
    }
}

//¿Qué son las notaciones postfija, prefija e infija?
//Básicamente, son las tres formas de escribir una operación:
//Infija: Es la que usamos siempre en la escuela. El operador (como +) va en medio de los números.
//Ejemplo: 3 + 5
//Prefija: El operador va antes de los dos números.
//Ejemplo: + 3 5
//Postfija: El operador va después de los dos números. (Esta es la que usa mi programa para hacer el árbol).
//Ejemplo: 3 5 +

//3. ¿Por qué se usa en las operaciones aritméticas?
//Principalmente, porque la notación postfija es mucho más fácil de entender para una computadora que la infija (la que usamos nosotros).
//El problema con la infija (la normal) es que es ambigua. Si le das a la computadora 3 + 5 * 2, no sabe si sumar primero o multiplicar primero, a menos que le programes reglas de prioridad y paréntesis.
//¿Qué relación tienen con los recorridos Preorden, Inorden y Postorden?
//La relación es total, son básicamente lo mismo. El árbol que mi programa construye es la representación visual de la operación.