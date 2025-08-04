package org.example;

import jcifs.CIFSContext;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import jcifs.config.PropertyConfiguration;
import jcifs.Configuration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        String googleDocId = "1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg";
        String downloadUrl = "https://docs.google.com/document/d/" + googleDocId + "/export?format=pdf";
        String tempFilePath = System.getProperty("java.io.tmpdir") + File.separator + "printDoc.pdf";
        String postscriptPath = System.getProperty("java.io.tmpdir") + File.separator + "printDoc.ps";

        String printerShare = "smb://172.16.14.46/asd";
        String domain = "dpworld";
        String username = "dpworldpc";
        String password = "asdasd";

        try {
            // Step 1: Download file
            logger.info("Downloading Google Doc to {}", tempFilePath);
            downloadFile(downloadUrl, tempFilePath);

            // Verify downloaded file
            File downloadedFile = new File(tempFilePath);
            if (!downloadedFile.exists() || downloadedFile.length() == 0) {
                throw new IOException("Downloaded file is missing or empty: " + tempFilePath);
            }
            logger.info("Downloaded file size: {} bytes", downloadedFile.length());

            // Verify PDF integrity
            try (PDDocument document = PDDocument.load(new File(tempFilePath))) {
                logger.info("PDF is valid with {} pages", document.getNumberOfPages());
            }

            // Step 2: Try direct PDF printing first
            logger.info("Attempting to send PDF directly to printer");
            boolean pdfSent = sendPdfDirectly(tempFilePath, printerShare, domain, username, password);
            if (pdfSent) {
                logger.info("PDF sent successfully, skipping PostScript conversion");
                return;
            }

            // Step 3: Convert PDF to simplified PostScript
            logger.info("Converting PDF to simplified PostScript");
            convertPdfToSimplePostScript(tempFilePath, postscriptPath);

            // Step 4: Send PostScript to printer
            logger.info("Sending PostScript to printer via SMB");
            sendToSmbPrinterFixed(tempFilePath, printerShare, domain, username, password);

        } catch (Exception e) {
            logger.error("Print job failed: {} - {}", e.getClass().getName(), e.getMessage(), e);
            // Fallback to Java Print Service
            logger.info("Attempting fallback printing via Java Print Service");
            printPdfWithJavaPrintService(tempFilePath, "AdminCabin2");
        } finally {
            // Clean up
        }
    }

    private static void downloadFile(String url, String tempFilePath) throws IOException {
        // Determine local directory based on OS
        String localDir = System.getProperty("os.name").toLowerCase().contains("win")
                ? "C:\\Downloads"
                : System.getProperty("user.home") + File.separator + "Downloads";

        // Create unique filename with timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String localFilePath = localDir + File.separator + "GoogleDoc_" + timestamp + ".pdf";

        // Ensure directories exist
        File tempFile = new File(tempFilePath);
        File tempParentDir = tempFile.getParentFile();
        if (tempParentDir != null && !tempParentDir.exists()) {
            tempParentDir.mkdirs();
        }

        File localParentDir = new File(localDir);
        if (!localParentDir.exists()) {
            localParentDir.mkdirs();
        }

        // Download and save to both temp and local paths
        try (InputStream in = new URL(url).openStream();
             FileOutputStream tempFos = new FileOutputStream(tempFilePath);
             FileOutputStream localFos = new FileOutputStream(localFilePath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                tempFos.write(buffer, 0, bytesRead); // Write to temp file
                localFos.write(buffer, 0, bytesRead); // Write to local file
                totalBytes += bytesRead;
            }

            tempFos.flush();
            localFos.flush();
            logger.info("File downloaded successfully to temp: {} ({} bytes)", tempFilePath, totalBytes);
            logger.info("File saved to local: {} ({} bytes)", localFilePath, totalBytes);

            // Verify local file
            File localFile = new File(localFilePath);
            if (!localFile.exists() || localFile.length() == 0) {
                logger.warn("Local file is missing or empty: {}", localFilePath);
            }
        } catch (IOException e) {
            logger.error("Failed to download file from {}: {}", url, e.getMessage());
            throw e;
        }
    }

    private static void convertPdfToSimplePostScript(String pdfPath, String psPath) throws Exception {
        try (PDDocument document = PDDocument.load(new File(pdfPath));
             PrintWriter writer = new PrintWriter(new FileWriter(psPath, java.nio.charset.StandardCharsets.UTF_8))) {

            // Write minimal PostScript header
            writer.println("%!PS-Adobe-3.0");
            writer.println("%%Title: Simple Print Job");
            writer.println("%%Pages: " + document.getNumberOfPages());
            writer.println("%%DocumentData: Clean7Bit");
            writer.println("%%LanguageLevel: 2");
            writer.println("%%EndComments");
            writer.println("/Times-Roman findfont 10 scalefont setfont"); // Standard font size: 10 points

            // Process each page
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setLineSeparator("\n");

            for (int pageNum = 1; pageNum <= document.getNumberOfPages(); pageNum++) {
                writer.println("%%Page: " + pageNum + " " + pageNum);
                writer.println("<< /PageSize [595 842] >> setpagedevice"); // A4 size
                writer.println("50 750 moveto"); // Start near top-left

                // Extract text for this page
                textStripper.setStartPage(pageNum);
                textStripper.setEndPage(pageNum);
                String pageText = textStripper.getText(document);

                // Write text lines
                String[] lines = pageText.split("\n");
                int lineHeight = 10; // Adjusted to match font size
                int currentY = 750;

                for (String line : lines) {
                    if (line.trim().isEmpty()) {
                        currentY -= lineHeight / 2;
                        continue;
                    }

                    // Basic escaping for PostScript
                    String escapedLine = line.replace("(", "\\(").replace(")", "\\)");
                    if (escapedLine.length() > 80) {
                        escapedLine = escapedLine.substring(0, 80);
                    }

                    writer.printf("50 %d moveto%n", currentY);
                    writer.printf("(%s) show%n", escapedLine);
                    currentY -= lineHeight;

                    if (currentY < 50) {
                        break;
                    }
                }

                writer.println("showpage");
            }

            writer.println("%%EOF");
            logger.info("PDF converted to simplified PostScript with {} pages", document.getNumberOfPages());
        }
    }

    private static boolean sendPdfDirectly(String pdfPath, String printerShare,
                                           String domain, String username, String password) throws Exception {
        Properties props = new Properties();
        props.setProperty("jcifs.smb.client.connTimeout", "60000");
        props.setProperty("jcifs.smb.client.soTimeout", "60000");
        props.setProperty("jcifs.smb.client.enableSMB2", "true");
        props.setProperty("jcifs.smb.client.responseTimeout", "60000");
        props.setProperty("jcifs.smb.client.logLevel", "4");


        Configuration config = new PropertyConfiguration(props);
        CIFSContext baseContext = new BaseContext(config);
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(baseContext, domain, username, password);
        CIFSContext authedContext = baseContext.withCredentials(auth);

        String jobName = "PDFJob_" + System.currentTimeMillis() + ".pdf";
        String fullPrinterPath = printerShare + "/" + jobName;
        SmbFile printer = new SmbFile(fullPrinterPath, authedContext);

        try (SmbFileOutputStream smbOut = new SmbFileOutputStream(printer);
             FileInputStream fis = new FileInputStream(pdfPath);
             BufferedOutputStream bos = new BufferedOutputStream(smbOut, 8192)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            bos.flush();
            logger.info("PDF job sent successfully: {} ({} bytes)", jobName, totalBytes);
            return true;

        } catch (Exception e) {
            logger.warn("Direct PDF printing failed: {} - {}, falling back to PostScript", e.getClass().getName(), e.getMessage());
            return false;
        }
    }

    private static void sendToSmbPrinterFixed(String pdfPath, String printerShare,
                                              String domain, String username, String password) throws Exception {
        Properties props = new Properties();
        props.setProperty("jcifs.smb.client.connTimeout", "60000000000");
        props.setProperty("jcifs.smb.client.soTimeout", "6000000000");
        props.setProperty("jcifs.smb.client.responseTimeout", "600000000");
        props.setProperty("jcifs.smb.client.logLevel", "4");


        Configuration config = new PropertyConfiguration(props);
        CIFSContext baseContext = new BaseContext(config);
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(baseContext, domain, username, password);
        CIFSContext authedContext = baseContext.withCredentials(auth);

        String jobName = "PSJob_" + System.currentTimeMillis() + ".ps";
        String fullPrinterPath = printerShare + "/" + jobName;
        SmbFile printer = new SmbFile(fullPrinterPath, authedContext);

        try (SmbFileOutputStream smbOut = new SmbFileOutputStream(printer);
             FileInputStream fis = new FileInputStream(pdfPath);
             BufferedOutputStream bos = new BufferedOutputStream(smbOut, 8192)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            bos.flush();
            logger.info("PostScript job sent successfully: {} ({} bytes)", jobName, totalBytes);

        } catch (Exception e) {
            logger.error("Failed to send PostScript job: {} - {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    private static void printPdfWithJavaPrintService(String pdfPath, String printerName) throws Exception {
        try {
            FileInputStream fis = new FileInputStream(pdfPath);
            DocFlavor flavor = DocFlavor.INPUT_STREAM.PDF;
            PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, null);
            PrintService targetPrinter = null;

            for (PrintService service : services) {
                if (service.getName().contains(printerName)) {
                    targetPrinter = service;
                    break;
                }
            }

            if (targetPrinter == null) {
                throw new Exception("Printer not found: " + printerName);
            }

            DocPrintJob job = targetPrinter.createPrintJob();
            Doc doc = new SimpleDoc(fis, flavor, null);
            HashPrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(new Copies(1));
            job.print(doc, attrs);
            fis.close();
            logger.info("PDF sent to printer via Java Print Service: {}", printerName);
        } catch (Exception e) {
            logger.error("Java Print Service failed: {} - {}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    private static void cleanupFile(String filePath) {
        try {
            if (new File(filePath).delete()) {
                logger.info("Cleaned up: {}", filePath);
            }
        } catch (Exception e) {
            logger.warn("Failed to clean up {}: {}", filePath, e.getMessage());
        }
    }
}