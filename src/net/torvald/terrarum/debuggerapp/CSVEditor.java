package net.torvald.terrarum.debuggerapp;

import net.torvald.terrarum.utils.CSVFetcher;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

/**
 * Should be made into its own artifact to build.
 *
 * Only recognisable columns are read and saved, thus this app should be update when new properties are added.
 *
 * Created by minjaesong on 2019-01-01.
 */
public class CSVEditor extends JFrame {

    /** Default columns. When you open existing csv, it should overwrite this. */
    private String[] columns = new String[]{"id", "drop", "spawn", "name", "shdr", "shdg", "shdb", "shduv", "str", "dsty", "mate", "solid", "wall", "grav", "dlfn", "fv", "fr", "lumr", "lumg", "lumb", "lumuv", "colour", "vscs", "refl","tags"};
    private final int FOUR_DIGIT = 42;
    private final int SIX_DIGIT = 50;
    private final int TWO_DIGIT = 30;
    private final int ARBITRARY = 240;
    private int[] colWidth = new int[]{FOUR_DIGIT, FOUR_DIGIT, FOUR_DIGIT, ARBITRARY, SIX_DIGIT, SIX_DIGIT, SIX_DIGIT, SIX_DIGIT, TWO_DIGIT, FOUR_DIGIT, FOUR_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, TWO_DIGIT, SIX_DIGIT, SIX_DIGIT, SIX_DIGIT, SIX_DIGIT, FOUR_DIGIT * 2, TWO_DIGIT, SIX_DIGIT, ARBITRARY};

    private final int UNDO_BUFFER_SIZE = 10;

    private CSVFormat csvFormat = CSVFetcher.INSTANCE.getTerrarumCSVFormat();

    private final int INITIAL_ROWS = 2;

    private JPanel panelSpreadSheet = new JPanel();
    private JPanel panelComment = new JPanel();
    private JSplitPane panelWorking = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelSpreadSheet, panelComment);

    private JMenuBar menuBar = new JMenuBar();
    private JTable spreadsheet = new JTable(new DefaultTableModel(columns, INITIAL_ROWS)); // it MUST be DefaultTableModel because that's what I'm using
    private JTextPane caption = new JTextPane();
    private JTextPane comment = new JTextPane();
    private JLabel statBar = new JLabel("null.");

    private JMenu undoMenu = new JMenu("Undo");
    private JMenu redoMenu = new JMenu("Redo");

    private Properties props = new Properties();
    private Properties lang = new Properties();

    private TraversingCircularArray<Object[][]> undoBuffer = new TraversingCircularArray(UNDO_BUFFER_SIZE);

    public CSVEditor() {
        // setup application properties //
        try {
            props.load(new StringReader(captionProperties));
            lang.load(new StringReader(translations));
        }
        catch (Throwable e) {

        }

        // setup layout //

        panelWorking.setDividerLocation(0.85);

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
        comment.setText("# This is a comment section.\n# All the comment must begin with this '#' mark.\n# null value on the CSV is represented as 'N/A'.");

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
            {
                add("Open...").addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        // let's show generic warning first
                        if (discardAgreed()) {

                            // actually read file
                            JFileChooser fileChooser = new JFileChooser("./") {
                                {
                                    setFileSelectionMode(JFileChooser.FILES_ONLY);
                                    setMultiSelectionEnabled(false);
                                }
                            };

                            fileChooser.showOpenDialog(null);

                            if (fileChooser.getSelectedFile() != null) {
                                if (fileChooser.getSelectedFile().exists()) {
                                    List<CSVRecord> records = CSVFetcher.INSTANCE.readFromFile(
                                            fileChooser.getSelectedFile().getAbsolutePath());

                                    // turn list of records into a spreadsheet

                                    // first dispose of any existing data
                                    ((DefaultTableModel) spreadsheet.getModel()).setRowCount(0);

                                    // then work on the file
                                    for (CSVRecord record : records) {
                                        String[] newRow = new String[columns.length];

                                        // construct newRow
                                        for (String column : columns) {
                                            try {
                                                String value = record.get(column);
                                                if (value == null) {
                                                    value = csvFormat.getNullString();
                                                }

                                                newRow[spreadsheet.getColumnModel().getColumnIndex(column)] = value;
                                            }
                                            catch (IllegalArgumentException mismatchedMapping) {
                                                newRow[spreadsheet.getColumnModel().getColumnIndex(column)] = "";
                                            }
                                        }

                                        ((DefaultTableModel) spreadsheet.getModel()).addRow(newRow);
                                    }

                                    addEventListenersToSpreadsheet();

                                    // then add the comments
                                    // since the Commons CSV simply ignores the comments, we have to read them on our own.
                                    try {
                                        StringBuilder sb = new StringBuilder();
                                        List<String> allTheLines = Files.readAllLines(
                                                fileChooser.getSelectedFile().toPath());

                                        allTheLines.forEach(line -> {
                                            if (line.startsWith("" + csvFormat.getCommentMarker().toString())) {
                                                sb.append(line);
                                                sb.append('\n');
                                            }
                                        });

                                        comment.setText(sb.toString());

                                        statBar.setText(lang.getProperty("STAT_LOAD_SUCCESSFUL"));
                                    }
                                    catch (Throwable fuck) {
                                        displayError("ERROR_INTERNAL", fuck);
                                    }
                                }
                                // if file not found
                                else {
                                    displayMessage("NO_SUCH_FILE");
                                }
                            } // if opening cancelled, do nothing
                        } // if discard cancelled, do nothing
                    }
                });

                add("Save...").addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        JFileChooser fileChooser = new JFileChooser("./") {
                            {
                                setFileSelectionMode(JFileChooser.FILES_ONLY);
                                setMultiSelectionEnabled(false);
                            }
                        };

                        fileChooser.showSaveDialog(null);

                        if (fileChooser.getSelectedFile() != null) {
                            try {
                                FileOutputStream fos = new FileOutputStream(fileChooser.getSelectedFile());

                                fos.write(toCSV().getBytes());

                                fos.flush();
                                fos.close();


                                statBar.setText(lang.getProperty("STAT_SAVE_SUCCESSFUL"));
                            }
                            catch (IOException iofuck) {
                                displayError("WRITE_FAIL", iofuck);
                            }
                        } // if saving cancelled, do nothing
                    }
                });

                add("New").addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (discardAgreed()) {
                            // ask new rows
                            Integer rows = askInteger("NEW_ROWS");

                            if (rows != null) {
                                // first, delete everything
                                ((DefaultTableModel) spreadsheet.getModel()).setRowCount(0);

                                // then add some columns
                                ((DefaultTableModel) spreadsheet.getModel()).setRowCount(rows);
                                addEventListenersToSpreadsheet();

                                // notify the user as well
                                statBar.setText(lang.getProperty("STAT_NEW_FILE"));
                            }
                        }
                    }
                });
            }
        });

        undoMenu.setEnabled(false);
        redoMenu.setEnabled(false);

        menuBar.add(new JMenu("Edit") {
            {
                add("New rows...").addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        Integer rows = askInteger("ADD_ROWS");

                        if (rows != null) {
                            DefaultTableModel tableModel = (DefaultTableModel) spreadsheet.getModel();
                            tableModel.setRowCount(tableModel.getRowCount() + rows);
                        }

                        addEventListenersToSpreadsheet();
                    }
                });
                add("New column...");
                add("Delete current row");
                add("Delete current column");
                addSeparator();
                add(undoMenu);
                add(redoMenu);
                addSeparator();
                add("Sort by ID").addMouseListener(new MouseAdapter() {
                    private String[] getRow(int row, DefaultTableModel table) {
                        String[] v = new String[table.getColumnCount()];
                        for (int k = 0; k < v.length; k++) {
                            v[k] = (String) table.getValueAt(row, k);
                        }
                        return v;
                    }

                    private void setRow(int row, String[] data, DefaultTableModel table) {
                        for (int k = 0; k < data.length; k++) {
                            table.setValueAt(data[k], row, k);
                        }
                    }

                    private int toInt(String s) {
                        int i;
                        try {
                            i = Integer.parseInt(s);

                            if (i == -1) i = 2147483646;
                        }
                        catch (NumberFormatException e) {
                            i = 2147483647;
                        }

                        return i;
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        DefaultTableModel table = (DefaultTableModel) spreadsheet.getModel();
                        int tableLen = table.getRowCount();

                        // perkele, had to get dirty
                        // using insertion sort (should work good enough)
                        int i = 1;
                        while (i < tableLen) {
                            String[] xData = getRow(i, table);
                            int xComparator = toInt(xData[0]); // x <- A[i]

                            int j = i - 1;
                            String[] jData = getRow(j, table);

                            while (j >= 0 && toInt(jData[0]) > xComparator) {
                                // manually set a row
                                setRow(j + 1, jData, table);
                                j -= 1;

                                if (j < 0) break;
                                jData = getRow(j, table);
                            }

                            setRow(j + 1, xData, table);
                            i += 1;
                        }
                    }
                });
            }
        });

        menuBar.revalidate();

        // setup spreadsheet //

        // no resize
        spreadsheet.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        // set column width
        for (int i = 0; i < columns.length; i++) {
            spreadsheet.getColumnModel().getColumn(i).setPreferredWidth(colWidth[i]);
        }
        // make tables do things

        addEventListenersToSpreadsheet();


        statBar.setText(lang.getProperty("STAT_INIT"));

        this.revalidate();
        this.repaint();
    }

    public static void main(String[] args) {
        new CSVEditor();
    }

    private void actuallyShowCaption(AWTEvent e) {
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

    private void actuallyShowCaption(AWTEvent e, int offset) {
        // make caption line working
        JTable table = ((JTable) e.getSource());
        int col = table.getSelectedColumn() + offset;
        if (col >= table.getColumnCount()) col = table.getColumnCount() - 1;
        else if (col < 0) col = 0;
        String colName = table.getColumnName(col);
        String captionText = props.getProperty(colName);

        caption.setText("<span style=\"font:sans-serif;\"><b>" + colName + "</b><span style=\"color:#404040;\">" +
                ((captionText == null) ? "" : ": " + captionText) +
                "</span></span>"
        );
    }

    private void addEventListenersToSpreadsheet() {
        // make tables do things
        spreadsheet.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                actuallyShowCaption(e);
            }
        });
        spreadsheet.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                actuallyShowCaption(e,
                        (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_KP_RIGHT || e.getKeyCode() == KeyEvent.VK_TAB) ? 1 :
                        (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_KP_LEFT) ? -1 : 0
                );
            }
        });
    }

    private String toCSV() {
        StringBuilder sb = new StringBuilder();

        int cols = spreadsheet.getColumnModel().getColumnCount();
        int rows = spreadsheet.getRowCount(); // actual rows, not counting the titles row

        // add all the column titles
        for (int i = 0; i < cols; i++) {
            sb.append('"');
            sb.append(spreadsheet.getColumnName(i));
            sb.append('"');
            if (i + 1 < cols) sb.append(';');
        } sb.append('\n');

        // loop for all the rows
        forEachRow:
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Object rawValue = spreadsheet.getModel().getValueAt(row, col);

                String cell;
                if (rawValue == null)
                    cell = "";
                else
                    cell = ((String) rawValue).toUpperCase();

                // skip if ID cell is empty
                if (col == 0 && cell.isEmpty()) {
                    continue forEachRow;
                }

                sb.append('"');
                sb.append(cell);
                sb.append('"');
                if (col + 1 < cols) sb.append(';');
            }
            sb.append("\n");
        } sb.append("\n\n");

        // add comments
        sb.append(comment.getText());


        return sb.toString();
    }

    private boolean discardAgreed() {
        return 0 == JOptionPane.showOptionDialog(null,
                lang.getProperty("WARNING_YOUR_DATA_WILL_GONE") + " " + lang.getProperty("WARNING_CONTINUE"),
                null,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"OK", "Cancel"},
                "Cancel"
        );
    }
    private boolean confirmedContinue(String messageKey) {
        return 0 == JOptionPane.showOptionDialog(null,
                lang.getProperty(messageKey) + " " + lang.getProperty("WARNING_CONTINUE"),
                null,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"OK", "Cancel"},
                "Cancel"
        );
    }
    private void displayMessage(String messageKey) {
        JOptionPane.showOptionDialog(null,
                lang.getProperty(messageKey),
                null,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new String[]{"OK", "Cancel"},
                "Cancel"
        );
    }
    private void displayError(String messageKey, Throwable cause) {
        JOptionPane.showOptionDialog(null,
                lang.getProperty(messageKey) + "\n" + cause.toString(),
                null,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new String[]{"OK", "Cancel"},
                "Cancel"
        );
    }

    /**
     *
     * @param messageKey
     * @return null if the operation cancelled, nonzero int if the choice was made
     */
    private Integer askInteger(String messageKey) {
        OptionSize optionWindow = new OptionSize(lang.getProperty(messageKey));
        int confirmedStatus = optionWindow.showDialog();

        if (confirmedStatus == JOptionPane.CANCEL_OPTION) {
            return null;
        }
        else {
            return ((Integer) optionWindow.capacity.getValue());
        }
    }

    // dummy string to make IDE happy with the auto indent
    private String captionProperties = """
            id=ID of this block
            drop=Which item the DroppedItem actually adds to your inventory
            spawn=Which item the DroppedItem should impersonate when spawned
            name=String identifier of the block
            shdr=Shade Red (light absorption). Valid range 0.0–1.0+
            shdg=Shade Green (light absorption). Valid range 0.0–1.0+
            shdb=Shade Blue (light absorption). Valid range 0.0–1.0+
            shduv=Shade UV (light absorbtion). Valid range 0.0–1.0+
            lumr=Luminosity Red (light intensity). Valid range 0.0–1.0+
            lumg=Luminosity Green (light intensity). Valid range 0.0–1.0+
            lumb=Luminosity Blue (light intensity). Valid range 0.0–1.0+
            lumuv=Luminosity UV (light intensity). Valid range 0.0–1.0+
            str=Strength of the block
            dsty=Density of the block. Water have 1000 in the in-game scale
            mate=Material of the block
            solid=Whether the file has full collision
            plat=Whether the block should behave like a platform
            wall=Whether the block can be used as a wall
            grav=Whether the block should fall through the empty space. N/A to not make it fall; 0 to fall immediately (e.g. Sand), nonzero to indicate that number of floating blocks can be supported (e.g. Scaffolding)
            dlfn=Dynamic Light Function. 0=Static. Please see <strong>notes</strong>
            fv=Vertical friction when player slide on the cliff. 0 means not slide-able
            fr=Horizontal friction. &lt;16:slippery 16:regular &gt;16:sticky
            colour=[Fluids] Colour of the block in hexadecimal RGBA.
            vscs=[Fluids] Viscocity of the block. 16 for water.
            refl=[NOT Fluids] Reflectance of the block, used by the light calculation. Valid range 0.0–1.0
            tags=Tags used by the crafting system and the game's internals""";

    /**
     * ¤ is used as a \n marker
     */
    private String translations = """
            WARNING_CONTINUE=Continue?
            WARNING_YOUR_DATA_WILL_GONE=Existing edits will be lost.
            OPERATION_CANCELLED=Operation cancelled.
            NO_SUCH_FILE=No such file exists, operation cancelled.
            NEW_ROWS=Enter the number of rows to initialise the new CSV.¤Remember, you can always add or delete rows later.
            ADD_ROWS=Enter the number of rows to add:
            WRITE_FAIL=Writing to file has failed:
            STAT_INIT=Creating a new CSV. You can still open existing file.
            STAT_SAVE_SUCCESSFUL=File saved successfully.
            STAT_NEW_FILE=New CSV created.
            STAT_LOAD_SUCCESSFUL=File loaded successfully.
            ERROR_INTERNAL=Something went wrong.""";
}

class OptionSize {
    JSpinner capacity = new JSpinner(new SpinnerNumberModel(
            10,
            1,
            4096,
            1
    ));
    private JPanel settingPanel = new JPanel();

    OptionSize(String message) {
        settingPanel.add(new JLabel("<html>" + message.replace("¤", "<br />") + "</html>"));
        settingPanel.add(capacity);
    }

    /**
     * returns either JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION
     */
    int showDialog() {
        return JOptionPane.showConfirmDialog(null, settingPanel,
                null, JOptionPane.OK_CANCEL_OPTION);
    }
}
