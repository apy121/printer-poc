package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("/api/v1/print")
public class PrintController {

    @Autowired
    private PDFPrintService printService;

    @PostMapping("/job")
    public ResponseEntity<String> printJob() {
        try {
            // Hardcoded values
            String printerName = "Canon iR C3226 (d6:9f:78) (56:c8:ad) (3)";
            String googleDocFileId = "1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg"; // Replace with your actual Google Doc file ID

            // Download Google Doc as PDF
            byte[] fileData = downloadGoogleDocAsPDF(googleDocFileId);

            if (fileData == null || fileData.length == 0) {
                return ResponseEntity.badRequest().body("Failed to download Google Doc");
            }

            boolean success = printService.printPDFToPrinter(fileData, printerName);
            return success ?
                    ResponseEntity.ok("Print job submitted successfully.") :
                    ResponseEntity.status(500).body("Print job failed.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error processing print job: " + e.getMessage());
        }
    }

    @GetMapping("/printers")
    public ResponseEntity<?> getAllPrinters() {
        return ResponseEntity.ok(printService.getAllPrinters());
    }

    @GetMapping("/printers/detailed")
    public ResponseEntity<?> getDetailedPrinters() {
        return ResponseEntity.ok(printService.getDetailedPrintersInfo());
    }

    @GetMapping("/job/status/{jobId}")
    public ResponseEntity<?> getPrintJobStatus(@PathVariable String jobId) {
        String status = printService.getPrintJobStatusFromCUPS(jobId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    public String health() {
        return "Application is running!";
    }

    private byte[] downloadGoogleDocAsPDF(String fileId) throws Exception {
        String googleExportPdfUrl = "https://docs.google.com/document/d/" + fileId + "/export?format=pdf";
        System.out.println("Step 1: Starting PDF download...");
        long downloadStart = System.currentTimeMillis();

        HttpURLConnection connection = (HttpURLConnection) new URL(googleExportPdfUrl).openConnection();
        connection.setRequestMethod("GET");
        // Note: You may need to add authentication headers if required by your Google Docs setup
        // connection.setRequestProperty("Authorization", "Bearer YOUR_ACCESS_TOKEN");

        try (InputStream in = connection.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            long downloadEnd = System.currentTimeMillis();
            System.out.println("Step 1 Complete: PDF downloaded in " + (downloadEnd - downloadStart) + " ms");
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }
}