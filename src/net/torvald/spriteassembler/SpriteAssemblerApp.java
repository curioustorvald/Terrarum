package net.torvald.spriteassembler;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by minjaesong on 2019-01-05.
 */
public class SpriteAssemblerApp extends JFrame {

    private ImagePanel panelPreview = new ImagePanel();
    private JTree panelProperties = new JTree();
    private JList<String> panelBodypartsList = new JList<String>();
    private JTextPane panelCode = new JTextPane();
    private JTextArea statBar = new JTextArea("Null.");

    public SpriteAssemblerApp() {
        JSplitPane panelDataView = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(panelProperties), new JScrollPane(panelBodypartsList));

        JSplitPane panelTop = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(panelPreview), panelDataView);
        JSplitPane panelMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTop, new JScrollPane(panelCode));

        JMenuBar menu = new JMenuBar();
        menu.add(new JMenu("File"));
        menu.add(new JMenu("Run"));

        this.setLayout(new BorderLayout());
        this.add(menu, BorderLayout.NORTH);
        this.add(panelMain, BorderLayout.CENTER);
        this.add(statBar, BorderLayout.SOUTH);
        this.setTitle("Terrarum Sprite Assembler and Viewer");
        this.setVisible(true);
        this.setSize(1154, 768);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        new SpriteAssemblerApp();
    }
}

class ImagePanel extends JPanel {

    private BufferedImage image;

    public ImagePanel() {
        try {
            image = ImageIO.read(new File("image name and path"));
        } catch (IOException ex) {
            // handle exception...
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this); // see javadoc for more info on the parameters
    }

}