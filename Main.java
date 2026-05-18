import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Inicia a interface gráfica na thread correta
        SwingUtilities.invokeLater(() -> {
            JanelaJogo jogo = new JanelaJogo();
            jogo.setVisible(true);
        });
    }
}