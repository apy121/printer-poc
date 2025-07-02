package org.example;

import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PrintService {

    private static final Logger log = LoggerFactory.getLogger(PrintService.class);
    private static final int BYTE_CONVERSION_SIZE = 8192;
    private static final int DEFAULT_PORT = 631;

    public String printDocument(PrintRequest request) throws Exception {
        try {
            // Validate temporary file base path
            String tempFileBasePath = request.getTempFileBasePath();
            if (!tempFileBasePath.endsWith("/")) {
                tempFileBasePath += "/";
            }
            String localPath = tempFileBasePath + UUID.randomUUID() + "." + request.getFileType();
            log.info("Temporary file path: {}", localPath);

            // Download file from Google Drive
            String downloadUrl = "https://docs.google.com/document/d/"
                    + request.getFileIdentifier() + "/export?format="
                    + request.getFileType();
            log.info("Downloading file from Google Drive: {}", downloadUrl);
            try (InputStream in = new URL(downloadUrl).openStream();
                 FileOutputStream fos = new FileOutputStream(localPath)) {
                byte[] buffer = new byte[BYTE_CONVERSION_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                log.info("File downloaded to: {}", localPath);
            } catch (IOException e) {
                throw new IOException("Failed to download file from Google Drive: " + e.getMessage(), e);
            }

            // Configure SSL for PaperCut server
            log.info("Configuring SSL with trust store: {}", request.getSslTrustStorePath());
            System.setProperty("javax.net.ssl.trustStore", request.getSslTrustStorePath());
            System.setProperty("javax.net.ssl.trustStorePassword", request.getSslTrustStorePassword());

            // Parse IPPS URI manually
            String ippsUri = request.getIppsUri();
            if (!ippsUri.startsWith("ipps://")) {
                throw new IllegalArgumentException("Invalid IPPS URI: Must start with 'ipps://'");
            }

            // Remove 'ipps://' prefix
            String uriWithoutProtocol = ippsUri.substring("ipps://".length());
            String host;
            int port = DEFAULT_PORT;
            String printerPath;

            // Split host:port and path
            int slashIndex = uriWithoutProtocol.indexOf('/');
            if (slashIndex == -1) {
                throw new IllegalArgumentException("Invalid IPPS URI: No path found in " + ippsUri);
            }
            String hostPort = uriWithoutProtocol.substring(0, slashIndex);
            printerPath = uriWithoutProtocol.substring(slashIndex);

            // Split host and port
            int colonIndex = hostPort.indexOf(':');
            if (colonIndex != -1) {
                host = hostPort.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(hostPort.substring(colonIndex + 1));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port in IPPS URI: " + hostPort, e);
                }
            } else {
                host = hostPort;
            }

            if (host.isEmpty()) {
                throw new IllegalArgumentException("Invalid hostname in IPPS URI: " + ippsUri);
            }
            if (port <= 0) {
                throw new IllegalArgumentException("Invalid port in IPPS URI: " + port);
            }
            log.info("Connecting to PaperCut IPPS queue: host={}, port={}", host, port);

            // Connect to PaperCut IPPS queue
            CupsClient client = new CupsClient(host, port, request.getUsername());

            CupsPrinter printer = client.getPrinters().stream()
                    .filter(p -> p.getName().equals(request.getPrinterName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Printer not found: " + request.getPrinterName()));

            // Create print job
            FileInputStream fis = new FileInputStream(new File(localPath));
            Map<String, String> attributes = new HashMap<>();
            if (request.getFileType().equalsIgnoreCase("zpl")) {
                attributes.put("document-format", "application/octet-stream"); // ZPL for Zebra printers
            } else {
                attributes.put("document-format", "application/pdf"); // PDF for reports
            }

            PrintJob.Builder jobBuilder = new PrintJob.Builder(fis)
                    .userName(request.getUsername())
                    .jobName("Print Job " + UUID.randomUUID())
                    .copies(1)
                    .attributes(attributes);

            // Send print job
            String jobId = String.valueOf(printer.print(jobBuilder.build()).getJobId());
            log.info("Print job sent successfully, Job ID: {}", jobId);

            // Clean up
            fis.close();
            new File(localPath).delete();
            return jobId;

        } catch (Exception e) {
            log.error("Error sending print job to PaperCut: {}, {}", e.getMessage(), e.getStackTrace());
            throw new RuntimeException("Failed to send print job: " + e.getMessage(), e);
        }
    }
}