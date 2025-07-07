package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.print.PrintServiceLookup;
import javax.print.PrintService;
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
    public boolean printPDFToPrinter() {
        String googleDownloadUrl = "https://drive.google.com/uc?export=download&id=" + "1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg";
        String tempFilePath = System.getProperty("java.io.tmpdir") + "/downloaded_file.pdf";
        String targetPrinterName = "HP408_POC_CIC_IRP";

        try {
            // Step 1: Download the file
            HttpURLConnection connection = (HttpURLConnection) new URL(googleDownloadUrl).openConnection();
            connection.setRequestMethod("GET");

            try (InputStream in = connection.getInputStream(); FileOutputStream out = new FileOutputStream(tempFilePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            File pdfFile = new File(tempFilePath);
            if (!pdfFile.exists()) {
                System.err.println("Downloaded file not found: " + tempFilePath);
                return false;
            }

            // Step 2: Load and render PDF
            PDDocument document = PDDocument.load(pdfFile);
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 300);

            // Step 3: Select printer
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

            // Step 4: Print logic
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
