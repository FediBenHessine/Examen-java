package IHM;

import javax.swing.SwingUtilities;

public class WhiteboardMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WhiteboardFrame frame = new WhiteboardFrame("HOST");
            frame.setVisible(true);
        });
    }
}
