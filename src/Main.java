import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.Set;

public class Main {

    private JFrame frame;
    private JTextArea textArea1;
    private JTextArea textArea2;
    private JLabel similarityLabel;

    private TextProcessor processor;

    public Main() {
        processor = new TextProcessor();
        initUI();
    }

    private void initUI() {
        frame = new JFrame("Text Similarity Checker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 600);
        frame.setLocationRelativeTo(null);

        textArea1 = new JTextArea();
        textArea2 = new JTextArea();

        JScrollPane scroll1 = new JScrollPane(textArea1);
        JScrollPane scroll2 = new JScrollPane(textArea2);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll1, scroll2);
        splitPane.setDividerLocation(frame.getWidth() / 2);

        JPanel buttonPanel = new JPanel();
        JButton load1Btn = new JButton("Load doc1.txt");
        JButton load2Btn = new JButton("Load doc2.txt");
        JButton computeBtn = new JButton("Compute Similarity");
        JButton exportPdfBtn = new JButton("Export PDF");
        JButton exportTxtBtn = new JButton("Export TXT");
        JButton clearHighlightBtn = new JButton("Clear Highlights");

        similarityLabel = new JLabel("Similarity: N/A");

        buttonPanel.add(load1Btn);
        buttonPanel.add(load2Btn);
        buttonPanel.add(computeBtn);
        buttonPanel.add(exportPdfBtn);
        buttonPanel.add(exportTxtBtn);
        buttonPanel.add(clearHighlightBtn);
        buttonPanel.add(similarityLabel);

        frame.setLayout(new BorderLayout());
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        String currentDir = Paths.get("").toAbsolutePath().toString();

        load1Btn.addActionListener(e -> loadFileWithChooser(textArea1, currentDir));
        load2Btn.addActionListener(e -> loadFileWithChooser(textArea2, currentDir));

        computeBtn.addActionListener(e -> computeAndHighlightSimilarity());

        exportPdfBtn.addActionListener(e -> saveFileWithChooser("PDF Files", "pdf", this::exportPdf));
        exportTxtBtn.addActionListener(e -> saveFileWithChooser("Text Files", "txt", this::exportTxt));

        clearHighlightBtn.addActionListener(e -> {
            clearHighlights(textArea1);
            clearHighlights(textArea2);
        });

        frame.setVisible(true);
    }

    private void loadFileWithChooser(JTextArea textArea, String startDir) {
        JFileChooser chooser = new JFileChooser(startDir);
        chooser.setDialogTitle("Select a text file to load");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                textArea.setText(content);
                clearHighlights(textArea);
                similarityLabel.setText("Similarity: N/A");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Failed to load file: " + e.getMessage());
            }
        }
    }

    private void computeAndHighlightSimilarity() {
        String text1 = textArea1.getText();
        String text2 = textArea2.getText();

        if (text1.isEmpty() || text2.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Both text areas must be filled.");
            return;
        }

        try {
            double cosineSim = processor.computeCosineSimilarity(text1, text2);
            double hybridSim = processor.computeHybridSimilarity(text1, text2);

            similarityLabel.setText(String.format("Cosine: %.2f%% | Hybrid: %.2f%%", cosineSim * 100, hybridSim * 100));

            clearHighlights(textArea1);
            clearHighlights(textArea2);

            Set<String> matchingTokens = processor.getMatchingTokens(text1, text2);

            highlightMatchingWords(textArea1, matchingTokens);
            highlightMatchingWords(textArea2, matchingTokens);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error computing similarity: " + e.getMessage());
        }
    }

    private void highlightMatchingWords(JTextArea textArea, Set<String> wordsToHighlight) {
        Highlighter highlighter = textArea.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        String text = textArea.getText().toLowerCase();

        for (String word : wordsToHighlight) {
            int index = 0;
            String lowerWord = word.toLowerCase();

            while ((index = text.indexOf(lowerWord, index)) >= 0) {
                try {
                    if (isWholeWord(text, index, lowerWord.length())) {
                        highlighter.addHighlight(index, index + lowerWord.length(), painter);
                    }
                    index += lowerWord.length();
                } catch (BadLocationException ignored) {}
            }
        }
    }

    private boolean isWholeWord(String text, int index, int length) {
        boolean startOk = (index == 0) || !Character.isLetterOrDigit(text.charAt(index - 1));
        int endIndex = index + length;
        boolean endOk = (endIndex == text.length()) || !Character.isLetterOrDigit(text.charAt(endIndex));
        return startOk && endOk;
    }

    private void clearHighlights(JTextArea textArea) {
        Highlighter highlighter = textArea.getHighlighter();
        highlighter.removeAllHighlights();
    }

    private void saveFileWithChooser(String description, String extension, SaveHandler handler) {
        JFileChooser chooser = new JFileChooser(Paths.get("").toAbsolutePath().toFile());
        chooser.setDialogTitle("Save " + description);
        chooser.setSelectedFile(new File("SimilarityReport." + extension));
        chooser.setFileFilter(new FileNameExtensionFilter(description, extension));

        int userSelection = chooser.showSaveDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = chooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith("." + extension)) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + "." + extension);
            }
            try {
                handler.save(fileToSave);
                JOptionPane.showMessageDialog(frame, description + " exported successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error exporting " + description + ": " + ex.getMessage());
            }
        }
    }

    private void exportPdf(File file) throws Exception {
        double similarity = processor.computeCosineSimilarity(textArea1.getText(), textArea2.getText());
        String report = String.format("Cosine Similarity: %.2f%%", similarity * 100);
        PdfExporter.export(report, textArea1.getText(), textArea2.getText(), file.getAbsolutePath());
    }

    private void exportTxt(File file) throws IOException {
        double cosine = processor.computeCosineSimilarity(textArea1.getText(), textArea2.getText());
        double hybrid = processor.computeHybridSimilarity(textArea1.getText(), textArea2.getText());
        String report = String.format(
                "Cosine Similarity: %.2f%%\nHybrid Similarity: %.2f%%\n\n--- Text 1 ---\n%s\n\n--- Text 2 ---\n%s\n",
                cosine * 100, hybrid * 100,
                textArea1.getText(), textArea2.getText()
        );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(report);
        }
    }

    @FunctionalInterface
    interface SaveHandler {
        void save(File file) throws Exception;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(Main::new);
    }
}
