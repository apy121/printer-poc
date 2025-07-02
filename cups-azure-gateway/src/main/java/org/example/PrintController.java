package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/print")
public class PrintController {

    @Autowired
    private PrintService printService;

    @PostMapping("/job")
    public ResponseEntity<String> printDocument(@RequestBody PrintRequest request) {
        try {
            String jobId = printService.printDocument(request);
            return ResponseEntity.ok(jobId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to send print job: " + e.getMessage());
        }
    }
}