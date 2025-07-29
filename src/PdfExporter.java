import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class PdfExporter {

    public static void export(String similarityReport, String text1, String text2, String outputFile) throws DocumentException, FileNotFoundException {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputFile));
        document.open();

        document.add(new Paragraph("Text Similarity Report"));
        document.add(new Paragraph("===================================="));
        document.add(new Paragraph(similarityReport));
        document.add(new Paragraph("\n--- Text 1 ---"));
        document.add(new Paragraph(text1));
        document.add(new Paragraph("\n--- Text 2 ---"));
        document.add(new Paragraph(text2));

        document.close();
    }
}
