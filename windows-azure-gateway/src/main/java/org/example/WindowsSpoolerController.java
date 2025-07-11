package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/spooler")
public class WindowsSpoolerController {

    @Autowired
    private WindowsSpoolerService spoolerService;

    @GetMapping("/jobs")
    public ResponseEntity<?> getAllPrintJobs() {
        String result = spoolerService.getAllPrintJobs();
        return ResponseEntity.ok(result);
    }
}
