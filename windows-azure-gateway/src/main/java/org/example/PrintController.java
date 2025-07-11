package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/print")
public class PrintController {

    @Autowired
    private PDFPrintService printService;

    @PostMapping(value = "/job", consumes = "multipart/form-data")
    public ResponseEntity<String> printJob(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("PDF file is required");
        }

        try {
            boolean success = printService.printPDFToPrinter(file.getInputStream());
            return success ?
                    ResponseEntity.ok("Print job submitted successfully.") :
                    ResponseEntity.status(500).body("Print job failed.");
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error processing file: " + e.getMessage());
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
}