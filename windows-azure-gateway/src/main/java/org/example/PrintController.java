package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/print")  // Base path
public class PrintController {

    @Autowired
    private PDFPrintService printService;

    @PostMapping("/job")
    public ResponseEntity<String> printJob(@RequestBody PrintRequest request) {
        System.out.println("Received request: " + request.getFileId());

        if (request.getFileId() == null || request.getFileId().isEmpty()) {
            return ResponseEntity.badRequest().body("FileId is required");
        }

        boolean success = printService.printPDFToPrinter(request.getFileId());
        return success ?
                ResponseEntity.ok("Print job submitted successfully.") :
                ResponseEntity.status(500).body("Print job failed.");
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


    // Add a simple health check endpoint
    @GetMapping("/health")
    public String health() {
        return "Application is running!";
    }
}
