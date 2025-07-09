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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class PDFPrintService {

    public boolean printPDFToPrinter(String fileId) {
        long startTime = System.currentTimeMillis();

        String googleExportPdfUrl = "https://docs.google.com/document/d/" + fileId + "/export?format=pdf";
        String tempFilePath = System.getProperty("java.io.tmpdir") + "/doc_print_file.pdf";
        String targetPrinterName = "Canon iR C3226 (d6:9f:78) (56:c8:ad) (3)";

        try {
            System.out.println("Step 1: Starting PDF download...");
            long downloadStart = System.currentTimeMillis();

            HttpURLConnection connection = (HttpURLConnection) new URL(googleExportPdfUrl).openConnection();
            connection.setRequestMethod("GET");

            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(tempFilePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            long downloadEnd = System.currentTimeMillis();
            System.out.println("Step 1 Complete: PDF downloaded in " + (downloadEnd - downloadStart) + " ms");

            File pdfFile = new File(tempFilePath);
            if (!pdfFile.exists()) {
                System.err.println("Downloaded file not found: " + tempFilePath);
                return false;
            }

            System.out.println("Step 2: Loading and rendering PDF...");
            long renderStart = System.currentTimeMillis();

            PDDocument document = PDDocument.load(pdfFile);
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 300);

            long renderEnd = System.currentTimeMillis();
            System.out.println("Step 2 Complete: PDF rendered in " + (renderEnd - renderStart) + " ms");

            System.out.println("Step 3: Finding target printer...");
            long printerSearchStart = System.currentTimeMillis();

            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService selectedPrinter = null;
            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(targetPrinterName)) {
                    selectedPrinter = service;
                    break;
                }
            }

            long printerSearchEnd = System.currentTimeMillis();
            System.out.println("Step 3 Complete: Printer search took " + (printerSearchEnd - printerSearchStart) + " ms");

            if (selectedPrinter == null) {
                System.err.println("Printer not found: " + targetPrinterName);
                document.close();
                return false;
            }

            System.out.println("Step 4: Sending job to printer...");
            long printStart = System.currentTimeMillis();

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

            long printEnd = System.currentTimeMillis();
            System.out.println("Step 4 Complete: Printing took " + (printEnd - printStart) + " ms");

            long totalEndTime = System.currentTimeMillis();
            System.out.println("Total time for printPDFToPrinter(): " + (totalEndTime - startTime) + " ms");

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
