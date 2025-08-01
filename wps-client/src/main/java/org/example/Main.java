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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Properties;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        String googleDocId = "1jQy-SHyx3O4Bqej2Gac-4JfK8br2VxVAJnIvY-EDqjg";
        String downloadUrl = "https://docs.google.com/document/d/" + googleDocId + "/export?format=pdf";
        String tempFilePath = System.getProperty("java.io.tmpdir") + File.separator + "printDoc.pdf";
        String postscriptPath = System.getProperty("java.io.tmpdir") + File.separator + "printDoc.ps";

        String printerShare = "smb://172.16.14.252/AdminCabin";
        String domain = "dpworld";
        String username = "dpworldpc";
        String password = "asdasd";

        try {
            // Step 1: Download file
            logger.info("Downloading Google Doc to {}", tempFilePath);
            downloadFile(downloadUrl, tempFilePath);

            // Step 2: Convert PDF to lightweight PostScript
            logger.info("Converting PDF to lightweight PostScript");
            convertPdfToSimplePostScript(tempFilePath, postscriptPath);

            // Step 3: Send PostScript to printer (fixed method)
            logger.info("Sending PostScript to printer via SMB");
            sendToSmbPrinterFixed(postscriptPath, printerShare, domain, username, password);

        } catch (Exception e) {
            logger.error("Print job failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Clean up
            cleanupFile(tempFilePath);
            cleanupFile(postscriptPath);
        }
    }

    private static void downloadFile(String url, String filePath) throws IOException {
        try (InputStream in = new URL(url).openStream();
             FileOutputStream fos = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            logger.info("File downloaded successfully to: {}", filePath);
        }
    }

    private static void convertPdfToSimplePostScript(String pdfPath, String psPath) throws Exception {
        try (PDDocument document = PDDocument.load(new File(pdfPath));
             PrintWriter writer = new PrintWriter(new FileWriter(psPath, java.nio.charset.StandardCharsets.UTF_8))) {

            // Write PostScript header
            writer.println("%!PS-Adobe-3.0");
            writer.println("%%Creator: Java PDF to PS Converter");
            writer.println("%%Title: PDF Print Job");
            writer.println("%%Pages: " + document.getNumberOfPages());
            writer.println("%%DocumentData: Clean7Bit");
            writer.println("%%LanguageLevel: 2");
            writer.println("%%EndComments");
            writer.println();

            // PostScript setup
            writer.println("%%BeginProlog");
            writer.println("/Times-Roman findfont 10 scalefont setfont");
            writer.println("%%EndProlog");
            writer.println();

            // Extract and format text from PDF
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setLineSeparator("\n");

            for (int pageNum = 1; pageNum <= document.getNumberOfPages(); pageNum++) {
                logger.info("Processing page {} of {}", pageNum, document.getNumberOfPages());

                writer.println("%%Page: " + pageNum + " " + pageNum);
                writer.println("%%BeginPageSetup");

                // Get page size
                PDPage page = document.getPage(pageNum - 1);
                PDRectangle pageSize = page.getMediaBox();
                float pageWidth = pageSize.getWidth();
                float pageHeight = pageSize.getHeight();

                writer.printf("<< /PageSize [%.1f %.1f] >> setpagedevice%n", pageWidth, pageHeight);
                writer.println("%%EndPageSetup");
                writer.println();

                // Extract text from this page
                textStripper.setStartPage(pageNum);
                textStripper.setEndPage(pageNum);
                String pageText = textStripper.getText(document);

                // Clean up the text and convert to PostScript
                writer.println("gsave");
                writer.println("50 " + (pageHeight - 50) + " moveto"); // Start near top-left

                // Split text into lines and render each line
                String[] lines = pageText.split("\n");
                int lineHeight = 12;
                int currentY = (int)(pageHeight - 50);

                for (String line : lines) {
                    if (line.trim().isEmpty()) {
                        currentY -= lineHeight / 2; // Half line space for empty lines
                        continue;
                    }

                    // Escape special PostScript characters
                    String escapedLine = line.replace("\\", "\\\\")
                            .replace("(", "\\(")
                            .replace(")", "\\)")
                            .replace("\r", "");

                    // Truncate very long lines
                    if (escapedLine.length() > 80) {
                        escapedLine = escapedLine.substring(0, 77) + "...";
                    }

                    writer.printf("50 %d moveto%n", currentY);
                    writer.printf("(%s) show%n", escapedLine);

                    currentY -= lineHeight;

                    // Start new page if we're getting too low
                    if (currentY < 50) {
                        break;
                    }
                }

                writer.println("grestore");
                writer.println("showpage");
                writer.println();
            }

            writer.println("%%EOF");
            logger.info("PDF converted to simple PostScript with {} pages", document.getNumberOfPages());
        }
    }

    private static void sendToSmbPrinterFixed(String filePath, String printerShare,
                                              String domain, String username, String password) throws Exception {
        Properties props = new Properties();
        props.setProperty("jcifs.smb.client.connTimeout", "60000");
        props.setProperty("jcifs.smb.client.soTimeout", "60000");
        props.setProperty("jcifs.smb.client.enableSMB2", "true");
        props.setProperty("jcifs.smb.client.responseTimeout", "60000");

        Configuration config = new PropertyConfiguration(props);
        CIFSContext baseContext = new BaseContext(config);
        NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(baseContext, domain, username, password);
        CIFSContext authedContext = baseContext.withCredentials(auth);

        // Create PostScript job name
        String jobName = "PSJob_" + System.currentTimeMillis() + ".ps";
        String fullPrinterPath = printerShare + "/" + jobName;

        // Test printer accessibility
        SmbFile printerDir = new SmbFile(printerShare + "/", authedContext);
        if (!printerDir.exists()) {
            throw new Exception("Printer share not accessible: " + printerShare);
        }

        SmbFile printer = new SmbFile(fullPrinterPath, authedContext);

        // Send file directly without modification
        try (SmbFileOutputStream smbOut = new SmbFileOutputStream(printer);
             FileInputStream fis = new FileInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(fis, 8192);
             BufferedOutputStream bos = new BufferedOutputStream(smbOut, 8192)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            bos.flush();
            logger.info("PostScript job sent successfully: {} ({} bytes)", jobName, totalBytes);

        } catch (Exception e) {
            logger.error("Failed to send PostScript job: {}", e.getMessage());
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