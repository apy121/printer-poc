package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class PDFPrintService {

    public boolean printPDFToPrinter(byte[] fileData, String printerName) {
        long startTime = System.currentTimeMillis();

        try (InputStream pdfStream = new ByteArrayInputStream(fileData)) {
            System.out.println("Step 1: Loading PDF from stream...");
            long loadStart = System.currentTimeMillis();

            PDDocument document = PDDocument.load(pdfStream);

            long loadEnd = System.currentTimeMillis();
            System.out.println("Step 1 Complete: PDF loaded in " + (loadEnd - loadStart) + " ms");

            System.out.println("Step 2: Rendering PDF...");
            long renderStart = System.currentTimeMillis();

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 300);

            long renderEnd = System.currentTimeMillis();
            System.out.println("Step 2 Complete: PDF rendered in " + (renderEnd - renderStart) + " ms");

            System.out.println("Step 3: Finding target printer...");
            long printerSearchStart = System.currentTimeMillis();

            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
            PrintService selectedPrinter = null;
            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(printerName)) {
                    selectedPrinter = service;
                    break;
                }
            }

            long printerSearchEnd = System.currentTimeMillis();
            System.out.println("Step 3 Complete: Printer search took " + (printerSearchEnd - printerSearchStart) + " ms");

            if (selectedPrinter == null) {
                System.err.println("Printer not found: " + printerName);
                document.close();
                return false;
            }

            System.out.println("Step 4: Sending job to printer...");
            long printStart = System.currentTimeMillis();

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(selectedPrinter);
            job.setCopies(1);

            PageFormat pageFormat = job.defaultPage();
            pageFormat.setOrientation(PageFormat.PORTRAIT);

            Paper paper = new Paper();
            double inch = 72;
            double width = 8.27 * inch;
            double height = 11.69 * inch;

            paper.setSize(width, height);
            paper.setImageableArea(36, 36, width - 72, height - 72);
            pageFormat.setPaper(paper);

            Book book = new Book();
            PDFPrintable printable = new PDFPrintable(document, Scaling.SHRINK_TO_FIT, false);
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