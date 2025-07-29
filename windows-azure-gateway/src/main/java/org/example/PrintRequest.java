package org.example;

import java.util.Arrays;

public class PrintRequest {
    private byte[] fileData;
    private String printerName;

    // Default constructor
    public PrintRequest() {}

    // Constructor
    public PrintRequest(byte[] fileData, String printerName) {
        this.fileData = fileData != null ? fileData.clone() : null;
        this.printerName = printerName;
    }

    public byte[] getFileData() {
        return fileData != null ? fileData.clone() : null;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData != null ? fileData.clone() : null;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }
}

//# Step 1: Convert PDF to Base64
//$filePath = "$env:USERPROFILE\Downloads\test.pdf"
//$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($filePath))
//
//        # Step 2: Prepare JSON body and write to temp file
//$json = @{
//    fileData = $base64
//    printerName = "My_Printer_Name"
//} | ConvertTo-Json -Compress
//
//# Save JSON to a file
//        $tempFile = "$env:TEMP\printjob.json"
//Set-Content -Path $tempFile -Value $json -Encoding UTF8
//
//# Step 3: Send using curl.exe
//curl.exe -X POST http://localhost:9100/api/v1/print/job `
//        -H "Content-Type: application/json" `
//        --data "@$tempFile"
