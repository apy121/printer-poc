package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        String fileType = "pdf";
        String userHome = System.getProperty("user.home");
        String localPath = userHome + "\\Downloads\\test." + fileType;  // Windows Downloads folder
        String targetPrinterName = "Your Printer Name"; // 🔁 Replace with your printer name

        try {
            File pdfFile = new File(localPath);
            if (!pdfFile.exists()) {
                System.err.println("File not found: " + localPath);
                return;
            }

            // Load PDF and render first page as image
            PDDocument document = PDDocument.load(pdfFile);
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 300); // first page @ 300 DPI

            // Find the specific printer
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService selectedPrinter = null;

            for (PrintService service : services) {
                System.out.println("Found printer: " + service.getName());
                if (service.getName().equalsIgnoreCase(targetPrinterName)) {
                    selectedPrinter = service;
                    break;
                }
            }

            if (selectedPrinter == null) {
                System.err.println("Printer not found: " + targetPrinterName);
                document.close();
                return;
            }

            // Setup print job
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

            System.out.println("Print job submitted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
