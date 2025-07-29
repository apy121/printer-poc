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

//$filePath = "$env:USERPROFILE\Downloads\test.pdf"
//$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($filePath))
//$json = "{`"fileData`": `"$base64`", `"printerName`": `"My_Printer_Name`"}"
//
//curl.exe -X POST http://localhost:9100/api/v1/print/job -H "Content-Type: application/json" -d "$json"
