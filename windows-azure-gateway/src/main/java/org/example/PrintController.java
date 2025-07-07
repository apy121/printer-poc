package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class PrintController {

    @Autowired
    private PDFPrintService printService;

    @PostMapping("/job")
    public String printJob(@RequestBody PrintRequest request) {
        boolean success = printService.printPDFToPrinter(request.getFileId());
        return success ? "Print job submitted successfully." : "Print job failed.";
    }
}
