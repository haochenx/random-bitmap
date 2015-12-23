package name.haochenxie.randombitmap.ui;

import name.haochenxie.randombitmap.JavaScriptPRNGEngine;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.stream.Stream;

public class MainFrame extends JFrame {

    private static final int DEFAULT_BIT_MAP_WIDTH = 300;
    private static final int DEFAULT_BIT_MAP_HEIGHT = 300;
    private static final int DEFAULT_SCALE = 2;
    public static final String[] EXAMPLE_LIST = {"xorshift.js", "lcg.js"};

    private Random rng = new Random();

    private RandomBitmapPanel randomBitmap;
    private JTextArea txtCode;
    private JTextField txtWidth;
    private JTextField txtHeight;
    private JTextField txtScale;
    private JTextField[] txtSeeds;

    public MainFrame() {
        init();
    }

    private void init() {
        setTitle("RNG Bitmap");

        JPanel mainPane = new JPanel();
        mainPane.setLayout(new BorderLayout());
        getContentPane().add(mainPane, BorderLayout.CENTER);

        JPanel logPane = new JPanel();
        logPane.setLayout(new BorderLayout());
        getContentPane().add(logPane, BorderLayout.SOUTH);

        JPanel imagePane = new JPanel();
        mainPane.add(imagePane, BorderLayout.CENTER);

        JPanel sidePane = new JPanel();
        sidePane.setLayout(new BorderLayout());
        mainPane.add(sidePane, BorderLayout.EAST);

        JPanel codePane = new JPanel();
        sidePane.add(codePane, BorderLayout.CENTER);

        JPanel paramPane = new JPanel();
        sidePane.add(paramPane, BorderLayout.SOUTH);

        { // for imagePane
            imagePane.setBorder(BorderFactory.createTitledBorder("Generated Bitmap"));
            imagePane.setLayout(new BorderLayout());

            {
                randomBitmap = new RandomBitmapPanel(DEFAULT_BIT_MAP_WIDTH, DEFAULT_BIT_MAP_HEIGHT, DEFAULT_SCALE);
                imagePane.add(new JScrollPane(randomBitmap), BorderLayout.CENTER);
            }

            {
                JPanel toolboxPane = new JPanel();
                toolboxPane.setLayout(new FlowLayout(FlowLayout.TRAILING));
                imagePane.add(toolboxPane, BorderLayout.NORTH);

                JButton btnSaveFile = new JButton("File");
                btnSaveFile.addActionListener($ -> handleSaveFile());
                toolboxPane.add(new JLabel("Save: "));
                toolboxPane.add(btnSaveFile);
            }
        }

        { // for codePane
            codePane.setBorder(BorderFactory.createTitledBorder("PRNG Algorithm Code (JavaScript)"));
            codePane.setLayout(new BorderLayout());

            {
                txtCode = new JTextArea();
                txtCode.setFont(new Font("Consolas", Font.PLAIN, 16));
                txtCode.setColumns(72);
                txtCode.setRows(40);
                codePane.add(new JScrollPane(txtCode), BorderLayout.CENTER);
            }

            {
                JPanel toolboxPane = new JPanel();
                toolboxPane.setLayout(new BoxLayout(toolboxPane, BoxLayout.X_AXIS));
                codePane.add(toolboxPane, BorderLayout.NORTH);

                String[] examples = EXAMPLE_LIST;

                toolboxPane.add(new JLabel("Example: "));
                JComboBox<String> cmbExamples = new JComboBox<>(examples);
                cmbExamples.addActionListener($ -> handleLoadExample((String) cmbExamples.getSelectedItem()));
                toolboxPane.add(cmbExamples);
            }
        }

        { // for paramPane
            paramPane.setBorder(BorderFactory.createTitledBorder("Parameters"));

            GroupLayout layout = new GroupLayout(paramPane);
            paramPane.setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);

            JLabel lblSize = new JLabel("Size: ");
            JPanel paneSize = new JPanel();
            {
                paneSize.setLayout(new BoxLayout(paneSize, BoxLayout.LINE_AXIS));
                txtWidth = new JTextField();
                txtHeight = new JTextField();
                paneSize.add(txtWidth);
                paneSize.add(new JLabel("x"));
                paneSize.add(txtHeight);
            }

            JLabel lblScale = new JLabel("Scale: ");
            txtScale = new JTextField();

            JLabel lblSeeds = new JLabel("Seeds: ");
            JPanel paneSeeds = new JPanel();
            {
                txtSeeds = new JTextField[] {new JTextField(), new JTextField(), new JTextField(), new JTextField()};
                paneSeeds.setLayout(new BoxLayout(paneSeeds, BoxLayout.Y_AXIS));
                for (JTextField txtSeed : txtSeeds) {
                    paneSeeds.add(txtSeed);
                }
            }

            JButton btnReseed = new JButton("Reseed");
            JButton btnGenerate = new JButton("Generate Bitmap");

            {
                btnReseed.addActionListener($ -> handleReseed());
                btnGenerate.addActionListener($ -> handleGenerate());
            }

            layout.setHorizontalGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                            .addComponent(lblSize)
                            .addComponent(lblScale)
                            .addComponent(lblSeeds))
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(paneSize)
                            .addComponent(txtScale)
                            .addComponent(paneSeeds)
                            .addGroup(layout.createSequentialGroup()
                                    .addComponent(btnReseed)
                                    .addComponent(btnGenerate))));
            layout.setVerticalGroup(layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(lblSize)
                            .addComponent(paneSize))
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(lblScale)
                            .addComponent(txtScale))
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(lblSeeds)
                            .addComponent(paneSeeds))
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                            .addComponent(btnReseed)
                            .addComponent(btnGenerate)));
        }

        {
            txtWidth.setText(Integer.toString(DEFAULT_BIT_MAP_WIDTH));
            txtHeight.setText(Integer.toString(DEFAULT_BIT_MAP_HEIGHT));
            txtScale.setText(Integer.toString(DEFAULT_SCALE));
            handleReseed();
        }

        handleLoadExample(EXAMPLE_LIST[0]);
        pack();
    }

    private void handleLoadExample(String example) {
        try {
            InputStream stream = getClass().getResource("/prng_examples/" + example).openStream();
            loadCodeSnippet(stream);
        } catch (NullPointerException | IOException ex) {
            showWarning(ex);
        }
    }

    private void loadCodeSnippet(InputStream input) throws IOException {
        txtCode.setText(IOUtils.toString(input));
    }

    private void handleSaveFile() {
        FileDialog fd = new FileDialog(this);
        fd.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".png"));
        fd.setMode(FileDialog.SAVE);
        fd.setFile("prng-bitmap.png");
        fd.setVisible(true);

        if (fd.getFiles().length > 0) {
            System.out.println();
            File file = fd.getFiles()[0];

            try {
                BufferedImage img = randomBitmap.createImage();
                ImageIO.write(img, "PNG", file);
            } catch (IOException e) {
                showWarning(e);
            }
        } else {
            log("File save cancelled");
        }
    }

    private void showWarning(Throwable e) {
        // TODO
        e.printStackTrace();
    }

    private void log(String msg) {
        // TODO
        System.out.println(msg);
    }

    private void handleGenerate() {
        int width = Integer.parseInt(txtWidth.getText());
        int height = Integer.parseInt(txtHeight.getText());
        int scale = Integer.parseInt(txtScale.getText());
        String code = txtCode.getText();

        randomBitmap.setSizeScale(width, height, scale);

        double[] seeds = Stream.of(txtSeeds)
                .map(txt -> txt.getText())
                .mapToDouble(str -> Double.parseDouble(str))
                .toArray();
        JavaScriptPRNGEngine engine = new JavaScriptPRNGEngine(code);

        try {
            byte[] data = engine.getRandomBytes(randomBitmap.getRequiredBytes(), seeds);
            randomBitmap.setData(data);
        } catch (ScriptException | NoSuchMethodException ex) {
            showWarning(ex);
        }
    }

    private void handleReseed() {
        for (JTextField txtSeed : txtSeeds) {
            txtSeed.setText(Double.toString(rng.nextDouble()));
        }
    }

    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

}
