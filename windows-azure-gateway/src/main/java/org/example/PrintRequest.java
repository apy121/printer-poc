package org.example;

public class PrintRequest {
    private String fileId;

    public String getFileId() {
        fileId = "1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg";
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}

// curl -X POST http://172.19.34.34:9100/job   -H "Content-Type: application/json"   -d '{"fileId": "1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg"}'
//