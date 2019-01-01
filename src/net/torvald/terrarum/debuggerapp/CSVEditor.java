package net.torvald.terrarum.debuggerapp;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.StringReader;
import java.util.Properties;

/**
 * Should be made into its own artifact to build.
 *
 * Created by minjaesong on 2019-01-01.
 */
public class CSVEditor extends JFrame {

    /** Default columns. When you open existing csv, it should overwrite this. */
    private String[] columns = new String[]{"id", "drop", "name", "shdr", "shdg", "shdb", "shduv", "str", "dsty", "mate", "solid", "plat", "wall", "fall", "dlfn", "fv", "fr", "lumr", "lumg", "lumb", "lumuv"};
    private final int FOUR_DIGIT = 42;
    private final int SIX_DIGIT = 50;
    private final int TWO_DIGIT = 30;
    private final int ARBITRARY = 240;
    private int[] colWidth = new int[]{FOUR_DIGIT, FOUR_DIGIT, ARBITRARY, SIX_DIGIT, SIX_DIGIT, SIX_DIGIT, SIX_DIGIT, TWO_DIGIT, FOUR_DIGIT, FOUR_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, SIX_DIGIT, SIX_DIGIT, SIX_DIGIT, SIX_DIGIT};
    private String[][] dummyData = new String[128][columns.length];

    private JPanel panelSpreadSheet = new JPanel();
    private JPanel panelComment = new JPanel();
    private JSplitPane panelWorking = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSpreadSheet, panelComment);

    private JMenuBar menuBar = new JMenuBar();
    private JTable spreadsheet = new JTable(dummyData, columns);
    private JTextPane caption = new JTextPane();
    private JTextPane comment = new JTextPane();
    private JLabel statBar = new JLabel("Creating a new CSV. You can still open existing file.");

    private Properties props = new Properties();

    public CSVEditor() {
        // setup application properties //
        try {
            props.load(new StringReader(captionProperties));
        }
        catch (Throwable e) {

        }

        // setup layout //

        this.setLayout(new BorderLayout());
        panelSpreadSheet.setLayout(new BorderLayout());
        panelComment.setLayout(new BorderLayout());

        spreadsheet.setVisible(true);

        caption.setVisible(true);
        caption.setEditable(false);
        caption.setContentType("text/html");
        caption.setText("<span style=\"font:sans-serif; color:#888888; font-style:italic;\">Description of the selected column will be displayed here.</span>");

        comment.setVisible(true);
        comment.setPreferredSize(new Dimension(100, 220));
        comment.setText("# This is a comment section.\n# All the comment must begin with this '#' mark.");

        panelSpreadSheet.add(menuBar, BorderLayout.NORTH);
        panelSpreadSheet.add(new JScrollPane(spreadsheet, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        ), BorderLayout.CENTER);
        panelComment.add(caption, BorderLayout.NORTH);
        panelComment.add(new JScrollPane(comment, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        ), BorderLayout.CENTER);
        this.add(statBar, BorderLayout.SOUTH);
        this.add(panelWorking, BorderLayout.CENTER);
        this.add(menuBar, BorderLayout.NORTH);

        this.setTitle("Terrarum CSV Editor");
        this.setVisible(true);
        this.setSize(1154, 768);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        // setup menubar //

        menuBar.add(new JMenu("File") {

        });
        menuBar.add(new JMenu("Edit") {

        });

        // setup spreadsheet //

        // no resize
        spreadsheet.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // set column width
        for (int i = 0; i < columns.length; i++) {
            spreadsheet.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
        }
        // make tables do things
        spreadsheet.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

            }
        });
        // make tables do things
        spreadsheet.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // make caption line working
                JTable table = ((JTable) e.getSource());
                int col = table.getSelectedColumn();
                String colName = table.getColumnName(col);
                String captionText = props.getProperty(colName);

                caption.setText("<span style=\"font:sans-serif;\"><b>" + colName + "</b><span style=\"color:#404040;\">" +
                        ((captionText == null) ? "" : ": " + captionText) +
                        "</span></span>"
                );
            }
        });
    }

    public static void main(String[] args) {
        new CSVEditor();
    }


    private String captionProperties =
            "" + // dummy string to make IDE happy with the auto indent
                    "id=Block ID\n" +
                    "drop=Drop ID\n" +
                    "name=String identifier of the block\n" +
                    "shdr=Shade Red (light absorption). Valid range 0.0-4.0\n" +
                    "shdg=Shade Green (light absorption). Valid range 0.0-4.0\n" +
                    "shdb=Shade Blue (light absorption). Valid range 0.0-4.0\n" +
                    "shduv=Shade UV (light absorbtion). Valid range 0.0-4.0\n" +
                    "lumr=Luminosity Red (light intensity). Valid range 0.0-4.0\n" +
                    "lumg=Luminosity Green (light intensity). Valid range 0.0-4.0\n" +
                    "lumb=Luminosity Blue (light intensity). Valid range 0.0-4.0\n" +
                    "lumuv=Luminosity UV (light intensity). Valid range 0.0-4.0\n" +
                    "str=Strength of the block\n" +
                    "dsty=Density of the block. Water have 1000 in the in-game scale\n" +
                    "mate=Material of the block\n" +
                    "solid=Whether the file has full collision\n" +
                    "plat=Whether the block should behave like a platform\n" +
                    "wall=Whether the block can be used as a wall\n" +
                    "fall=Whether the block should fall through the empty space\n" +
                    "dlfn=Dynamic Light Function. 0=Static. Please see <strong>notes</strong>\n" +
                    "fv=Vertical friction when player slide on the cliff. 0 means not slide-able\n" +
                    "fr=Horizontal friction. &lt;16:slippery 16:regular &gt;16:sticky\n";
}
