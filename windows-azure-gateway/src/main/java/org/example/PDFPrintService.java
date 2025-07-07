package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;

@Service
public class PDFPrintService {

    public boolean printPDFToPrinter() {
        // Modify this path if your Downloads folder is located elsewhere
        String downloadsPath = System.getProperty("user.home") + "/Downloads/test.pdf";
        String targetPrinterName = "HP408_POC_CIC_IRP";

        try {
            File pdfFile = new File(downloadsPath);
            if (!pdfFile.exists()) {
                System.err.println("File not found at: " + downloadsPath);
                return false;
            }

            // Step 1: Load and render PDF
            PDDocument document = PDDocument.load(pdfFile);
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 300);

            // Step 2: Select printer
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService selectedPrinter = null;
            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(targetPrinterName)) {
                    selectedPrinter = service;
                    break;
                }
            }

            if (selectedPrinter == null) {
                System.err.println("Printer not found: " + targetPrinterName);
                document.close();
                return false;
            }

            // Step 3: Print logic
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(selectedPrinter);
            BufferedImage finalImage = image;

            job.setPrintable((Graphics g, PageFormat pf, int page) -> {
                if (page > 0) return Printable.NO_SUCH_PAGE;

                Graphics2D g2d = (Graphics2D) g;
                g2d.translate(pf.getImageableX(), pf.getImageableY());
                double scaleX = pf.getImageableWidth() / finalImage.getWidth();
                double scaleY = pf.getImageableHeight() / finalImage.getHeight();
                double scale = Math.min(scaleX, scaleY);

                g2d.scale(scale, scale);
                g2d.drawImage(finalImage, 0, 0, null);
                return Printable.PAGE_EXISTS;
            });

            job.print();
            document.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
