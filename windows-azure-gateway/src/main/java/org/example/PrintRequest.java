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