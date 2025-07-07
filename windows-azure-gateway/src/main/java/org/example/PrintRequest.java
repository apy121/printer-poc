package org.example;

public class PrintRequest {
    private String fileId;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}

//curl -X POST http://localhost:8080/job \
//        -H "Content-Type: application/json" \
//        -d @payload.json


// echo '{"fieldId": "1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg"}' > payload.json