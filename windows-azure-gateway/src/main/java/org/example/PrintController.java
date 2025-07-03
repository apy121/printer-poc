package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PrintController {

    @Autowired
    private PrintService printService;

    @PostMapping("/job")
    public String printJob() {
        boolean success = printService.printPDFToPrinter();
        return success ? "Print job submitted successfully." : "Print job failed.";
    }
}
