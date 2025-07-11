package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
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
            job.setCopies(1);

// Create a PageFormat and set to portrait
            PageFormat pageFormat = job.defaultPage();
            pageFormat.setOrientation(PageFormat.PORTRAIT);

            // Adjust paper size to A4
            Paper paper = new Paper();
            double inch = 72; // 1 inch = 72 points
            double width = 8.27 * inch;   // A4 width in points
            double height = 11.69 * inch; // A4 height in points

            // Set the paper size and imageable area (leave 0.5 inch margin)
            paper.setSize(width, height);
            paper.setImageableArea(36, 36, width - 72, height - 72); // 0.5 inch margins
            pageFormat.setPaper(paper);

            // Create a Book with this format and the printable
            Book book = new Book();
            PDFPrintable printable = new PDFPrintable(document, Scaling.SHRINK_TO_FIT, false); // false = do not rotate landscape
            book.append(printable, pageFormat, document.getNumberOfPages());

            job.setPageable(book);
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

    public String getPrintJobStatusFromCUPS(String jobId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("curl", "http://localhost:9191/jobs/" + jobId);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                return "Failed to fetch job status. Exit code: " + exitCode;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error while getting job status: " + e.getMessage();
        }
    }

    public List<String> getAllPrinters() {
        List<String> printers = new ArrayList<>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            printers.add(service.getName());
        }
        return printers;
    }

    public String getDetailedPrintersInfo() {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell.exe",
                    "-Command",
                    "Get-Printer | Select-Object Name,ShareName,Location,DriverName,PortName,Comment,PrinterStatus | ConvertTo-Json"
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            process.waitFor();
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to get printer details: " + e.getMessage();
        }
    }
}
