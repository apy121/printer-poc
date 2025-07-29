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

//curl -X POST http://localhost:8080/job \
//        -H "Content-Type: application/json" \
//        -d @payload.json


// echo '{"fieldId": "1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg"}' > payload.json

// curl -X POST http://localhost:9100/api/v1/print/job -H "Content-Type: application/json" -d "{\"fileId\": \"1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg\"}"


//curl -X POST http://localhost:8080/job \
//        -H "Content-Type: application/json" \
//        -d '{
//        "fileData": "'"$(base64 -i ~/Downloads/test.pdf)"'",
//        "printerName": "My_Printer_Name"
//        }'

// curl -X POST http://localhost:8080/job -H "Content-Type: application/json" -d '{"fileData":"'"$(base64 -i ~/Downloads/test.pdf)"'","printerName":"My_Printer_Name"}'

// curl -Uri http://localhost:9100/job -Method POST -Headers @{"Content-Type"="application/json"} -Body (@{fileData = [Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\Downloads\test.pdf")); printerName = "My_Printer_Name"} | ConvertTo-Json -Compress)

// curl -X POST http://localhost:9100/api/v1/print/job -H "Content-Type: application/json" -d "{`"fileData`":`"`"$( [Convert]::ToBase64String([IO.File]::ReadAllBytes(`"$env:USERPROFILE\Downloads\test.pdf`")) )`"`",`"printerName`":`"My_Printer_Name`"}"