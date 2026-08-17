package org.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class PDFPrintService {


    public void printPDFToPrinter(byte[] fileData, String printerName) {
        long startTime = System.currentTimeMillis();

        System.out.println("==================================================");
        System.out.println("START PDF PRINT");
        System.out.println("==================================================");

        try {
            // ---------------------------------------------------------
            // 1. Basic input validation
            // ---------------------------------------------------------
            if (fileData == null || fileData.length == 0) {
                throw new IllegalArgumentException("PDF fileData is empty");
            }

            if (printerName == null || printerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Printer name is empty");
            }

            System.out.println("Printer requested : " + printerName);
            System.out.println("PDF size          : " + fileData.length + " bytes");

            // ---------------------------------------------------------
            // 2. Load PDF
            // ---------------------------------------------------------
            long pdfLoadStart = System.currentTimeMillis();

            try (InputStream pdfStream = new ByteArrayInputStream(fileData);
                 PDDocument document = PDDocument.load(pdfStream)) {

                long pdfLoadEnd = System.currentTimeMillis();

                System.out.println(
                        "PDF loaded successfully in "
                                + (pdfLoadEnd - pdfLoadStart)
                                + " ms"
                );

                int numberOfPages = document.getNumberOfPages();

                System.out.println("PDF page count    : " + numberOfPages);

                // -----------------------------------------------------
                // 3. Log PDF page dimensions
                // -----------------------------------------------------
                for (int i = 0; i < numberOfPages; i++) {

                    PDPage pdfPage = document.getPage(i);

                    float mediaWidth = pdfPage.getMediaBox().getWidth();
                    float mediaHeight = pdfPage.getMediaBox().getHeight();

                    System.out.println(
                            "PDF Page " + (i + 1)
                                    + " dimensions      : "
                                    + mediaWidth
                                    + " x "
                                    + mediaHeight
                                    + " points"
                    );

                    System.out.println(
                            "PDF Page " + (i + 1)
                                    + " dimensions      : "
                                    + (mediaWidth / 72.0)
                                    + " x "
                                    + (mediaHeight / 72.0)
                                    + " inches"
                    );

                    System.out.println(
                            "PDF Page " + (i + 1)
                                    + " dimensions      : "
                                    + (mediaWidth * 25.4 / 72.0)
                                    + " x "
                                    + (mediaHeight * 25.4 / 72.0)
                                    + " mm"
                    );

                    System.out.println(
                            "PDF Page " + (i + 1)
                                    + " rotation         : "
                                    + pdfPage.getRotation()
                    );
                }

                // -----------------------------------------------------
                // 4. Find printer
                // -----------------------------------------------------
                System.out.println("Searching for printers...");

                long printerSearchStart = System.currentTimeMillis();

                PrintService[] services =
                        PrintServiceLookup.lookupPrintServices(null, null);

                long printerSearchEnd = System.currentTimeMillis();

                System.out.println(
                        "Printer lookup took "
                                + (printerSearchEnd - printerSearchStart)
                                + " ms"
                );

                System.out.println(
                        "Number of printers found: "
                                + services.length
                );

                PrintService selectedPrinter = null;

                for (PrintService service : services) {

                    System.out.println(
                            "Available printer      : "
                                    + service.getName()
                    );

                    if (service.getName().equalsIgnoreCase(printerName)) {
                        selectedPrinter = service;
                    }
                }

                if (selectedPrinter == null) {
                    throw new IllegalArgumentException(
                            "Printer not found: " + printerName
                    );
                }

                System.out.println(
                        "Selected printer       : "
                                + selectedPrinter.getName()
                );

                // -----------------------------------------------------
                // 5. Printer details
                // -----------------------------------------------------
                System.out.println("Printer class          : "
                        + selectedPrinter.getClass().getName());

                System.out.println("Printer description    : "
                        + selectedPrinter);

                // -----------------------------------------------------
                // 6. Create PrinterJob
                // -----------------------------------------------------
                PrinterJob job = PrinterJob.getPrinterJob();

                System.out.println(
                        "PrinterJob created     : "
                                + job
                );

                job.setPrintService(selectedPrinter);

                System.out.println(
                        "PrintService assigned successfully"
                );

                // -----------------------------------------------------
                // 7. IMPORTANT:
                //    Inspect driver's DEFAULT PageFormat
                // -----------------------------------------------------
                PageFormat driverDefaultPageFormat =
                        job.defaultPage();

                System.out.println("");
                System.out.println("==================================================");
                System.out.println("ZEBRA DRIVER DEFAULT PAGE FORMAT");
                System.out.println("==================================================");

                logPageFormat(
                        "DRIVER DEFAULT",
                        driverDefaultPageFormat
                );

                // -----------------------------------------------------
                // 8. Create our 3 x 2 inch label
                // -----------------------------------------------------
                //
                // 3 inches = 216 points
                // 2 inches = 144 points
                //
                double labelWidth = 3.0 * 72.0;
                double labelHeight = 2.0 * 72.0;

                System.out.println("");
                System.out.println("==================================================");
                System.out.println("CUSTOM ZEBRA LABEL CONFIGURATION");
                System.out.println("==================================================");

                System.out.println(
                        "Label width          : "
                                + labelWidth
                                + " points"
                );

                System.out.println(
                        "Label height         : "
                                + labelHeight
                                + " points"
                );

                System.out.println(
                        "Label width          : "
                                + (labelWidth / 72.0)
                                + " inches"
                );

                System.out.println(
                        "Label height         : "
                                + (labelHeight / 72.0)
                                + " inches"
                );

                // -----------------------------------------------------
                // 9. Create Paper
                // -----------------------------------------------------
                Paper paper = new Paper();

                paper.setSize(
                        labelWidth,
                        labelHeight
                );

                paper.setImageableArea(
                        0,
                        0,
                        labelWidth,
                        labelHeight
                );

                // -----------------------------------------------------
                // 10. Create PageFormat
                // -----------------------------------------------------
                PageFormat pageFormat = new PageFormat();

                pageFormat.setPaper(paper);

                pageFormat.setOrientation(
                        PageFormat.PORTRAIT
                );

                System.out.println("");
                System.out.println(
                        "CUSTOM PAGE FORMAT"
                );

                logPageFormat(
                        "CUSTOM 3x2",
                        pageFormat
                );

                // -----------------------------------------------------
                // 11. Validate our custom PageFormat
                // -----------------------------------------------------
                System.out.println("");
                System.out.println(
                        "PageFormat validation"
                );

                System.out.println(
                        "Page width           : "
                                + pageFormat.getWidth()
                );

                System.out.println(
                        "Page height          : "
                                + pageFormat.getHeight()
                );

                System.out.println(
                        "Imageable width      : "
                                + pageFormat.getImageableWidth()
                );

                System.out.println(
                        "Imageable height     : "
                                + pageFormat.getImageableHeight()
                );

                if (pageFormat.getImageableWidth() <= 0) {
                    throw new IllegalStateException(
                            "Imageable width is invalid: "
                                    + pageFormat.getImageableWidth()
                    );
                }

                if (pageFormat.getImageableHeight() <= 0) {
                    throw new IllegalStateException(
                            "Imageable height is invalid: "
                                    + pageFormat.getImageableHeight()
                    );
                }

                // -----------------------------------------------------
                // 12. PDF Renderer
                // -----------------------------------------------------
                PDFRenderer renderer =
                        new PDFRenderer(document);

                // -----------------------------------------------------
                // 13. Printable
                // -----------------------------------------------------
                Printable printable =
                        (Graphics g, PageFormat pf, int page) -> {

                            long pageStart =
                                    System.currentTimeMillis();

                            System.out.println("");
                            System.out.println(
                                    "--------------------------------------------------"
                            );

                            System.out.println(
                                    "PRINTABLE CALLBACK"
                            );

                            System.out.println(
                                    "Page requested       : "
                                            + (page + 1)
                            );

                            System.out.println(
                                    "Total pages          : "
                                            + numberOfPages
                            );

                            if (page >= numberOfPages) {

                                System.out.println(
                                        "NO_SUCH_PAGE returned"
                                );

                                return Printable.NO_SUCH_PAGE;
                            }

                            try {

                                // -------------------------------------
                                // Render at Zebra's actual resolution
                                // -------------------------------------
                                final int dpi = 203;

                                System.out.println(
                                        "Rendering PDF page at : "
                                                + dpi
                                                + " DPI"
                                );

                                long renderStart =
                                        System.currentTimeMillis();

                                BufferedImage image =
                                        renderer.renderImageWithDPI(
                                                page,
                                                dpi
                                        );

                                long renderEnd =
                                        System.currentTimeMillis();

                                System.out.println(
                                        "PDF rendering took    : "
                                                + (renderEnd - renderStart)
                                                + " ms"
                                );

                                System.out.println(
                                        "Rendered image width  : "
                                                + image.getWidth()
                                );

                                System.out.println(
                                        "Rendered image height : "
                                                + image.getHeight()
                                );

                                System.out.println(
                                        "Rendered image type   : "
                                                + image.getType()
                                );

                                // -------------------------------------
                                // PageFormat diagnostics
                                // -------------------------------------
                                System.out.println(
                                        "PageFormat width      : "
                                                + pf.getWidth()
                                );

                                System.out.println(
                                        "PageFormat height     : "
                                                + pf.getHeight()
                                );

                                System.out.println(
                                        "Imageable X           : "
                                                + pf.getImageableX()
                                );

                                System.out.println(
                                        "Imageable Y           : "
                                                + pf.getImageableY()
                                );

                                System.out.println(
                                        "Imageable width       : "
                                                + pf.getImageableWidth()
                                );

                                System.out.println(
                                        "Imageable height      : "
                                                + pf.getImageableHeight()
                                );

                                System.out.println(
                                        "Orientation           : "
                                                + getOrientationName(
                                                pf.getOrientation()
                                        )
                                );

                                // -------------------------------------
                                // Graphics diagnostics
                                // -------------------------------------
                                if (!(g instanceof Graphics2D)) {

                                    throw new IllegalStateException(
                                            "Graphics is not Graphics2D. Actual class: "
                                                    + g.getClass().getName()
                                    );
                                }

                                Graphics2D g2d =
                                        (Graphics2D) g;

                                // -------------------------------------
                                // Calculate scaling
                                // -------------------------------------
                                double scaleX =
                                        pf.getImageableWidth()
                                                / image.getWidth();

                                double scaleY =
                                        pf.getImageableHeight()
                                                / image.getHeight();

                                double scale =
                                        Math.min(
                                                scaleX,
                                                scaleY
                                        );

                                System.out.println(
                                        "Scale X              : "
                                                + scaleX
                                );

                                System.out.println(
                                        "Scale Y              : "
                                                + scaleY
                                );

                                System.out.println(
                                        "Selected scale       : "
                                                + scale
                                );

                                if (scale <= 0 ||
                                        Double.isNaN(scale) ||
                                        Double.isInfinite(scale)) {

                                    throw new IllegalStateException(
                                            "Invalid scale calculated: "
                                                    + scale
                                    );
                                }

                                // -------------------------------------
                                // Calculate final printed dimensions
                                // -------------------------------------
                                double finalWidth =
                                        image.getWidth()
                                                * scale;

                                double finalHeight =
                                        image.getHeight()
                                                * scale;

                                System.out.println(
                                        "Final printed width  : "
                                                + finalWidth
                                );

                                System.out.println(
                                        "Final printed height : "
                                                + finalHeight
                                );

                                // -------------------------------------
                                // Draw
                                // -------------------------------------
                                g2d.translate(
                                        pf.getImageableX(),
                                        pf.getImageableY()
                                );

                                g2d.scale(
                                        scale,
                                        scale
                                );

                                g2d.drawImage(
                                        image,
                                        0,
                                        0,
                                        null
                                );

                                long pageEnd =
                                        System.currentTimeMillis();

                                System.out.println(
                                        "Page printing callback completed in "
                                                + (pageEnd - pageStart)
                                                + " ms"
                                );

                                return Printable.PAGE_EXISTS;

                            } catch (Exception e) {

                                System.err.println(
                                        "ERROR while rendering page "
                                                + (page + 1)
                                );

                                System.err.println(
                                        "Error type: "
                                                + e.getClass().getName()
                                );

                                System.err.println(
                                        "Error message: "
                                                + e.getMessage()
                                );

                                e.printStackTrace();

                                throw new RuntimeException(
                                        "Failed to render PDF page "
                                                + (page + 1),
                                        e
                                );
                            }
                        };

                // -----------------------------------------------------
                // 14. Assign Printable + PageFormat
                // -----------------------------------------------------
                System.out.println("");
                System.out.println(
                        "Setting Printable + custom PageFormat"
                );

                job.setPrintable(
                        printable,
                        pageFormat
                );

                System.out.println(
                        "Printable configured successfully"
                );

                // -----------------------------------------------------
                // 15. Print
                // -----------------------------------------------------
                System.out.println("");
                System.out.println("==================================================");
                System.out.println("STARTING PRINT JOB");
                System.out.println("==================================================");

                long printStart =
                        System.currentTimeMillis();

                job.print();

                long printEnd =
                        System.currentTimeMillis();

                System.out.println("");
                System.out.println(
                        "Print job completed successfully"
                );

                System.out.println(
                        "Printing duration    : "
                                + (printEnd - printStart)
                                + " ms"
                );

                System.out.println(
                        "Total duration       : "
                                + (System.currentTimeMillis()
                                - startTime)
                                + " ms"
                );

                System.out.println(
                        "=================================================="
                );
                System.out.println("END PDF PRINT");
                System.out.println("==================================================");
            }

        } catch (IllegalArgumentException e) {

            System.err.println(
                    "IllegalArgumentException during printing: "
                            + e.getMessage()
            );

            e.printStackTrace();

            throw e;

        } catch (Exception e) {

            System.err.println(
                    "ERROR during PDF printing"
            );

            System.err.println(
                    "Error type    : "
                            + e.getClass().getName()
            );

            System.err.println(
                    "Error message : "
                            + e.getMessage()
            );

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to print PDF: "
                            + e.getMessage(),
                    e
            );
        }
    }


    /**
     * Logs complete PageFormat/Paper information.
     */
    private void logPageFormat(
            String label,
            PageFormat pageFormat
    ) {
        System.out.println(
                "----- " + label + " -----"
        );

        System.out.println(
                "Width              : "
                        + pageFormat.getWidth()
                        + " points"
        );

        System.out.println(
                "Height             : "
                        + pageFormat.getHeight()
                        + " points"
        );

        System.out.println(
                "Width              : "
                        + (pageFormat.getWidth() / 72.0)
                        + " inches"
        );

        System.out.println(
                "Height             : "
                        + (pageFormat.getHeight() / 72.0)
                        + " inches"
        );

        System.out.println(
                "Imageable X        : "
                        + pageFormat.getImageableX()
        );

        System.out.println(
                "Imageable Y        : "
                        + pageFormat.getImageableY()
        );

        System.out.println(
                "Imageable Width    : "
                        + pageFormat.getImageableWidth()
        );

        System.out.println(
                "Imageable Height   : "
                        + pageFormat.getImageableHeight()
        );

        System.out.println(
                "Imageable Width    : "
                        + (pageFormat.getImageableWidth() / 72.0)
                        + " inches"
        );

        System.out.println(
                "Imageable Height   : "
                        + (pageFormat.getImageableHeight() / 72.0)
                        + " inches"
        );

        System.out.println(
                "Orientation        : "
                        + getOrientationName(
                        pageFormat.getOrientation()
                )
        );

        Paper paper = pageFormat.getPaper();

        System.out.println(
                "Paper Width        : "
                        + paper.getWidth()
                        + " points"
        );

        System.out.println(
                "Paper Height       : "
                        + paper.getHeight()
                        + " points"
        );

        System.out.println(
                "Paper Imageable X  : "
                        + paper.getImageableX()
        );

        System.out.println(
                "Paper Imageable Y  : "
                        + paper.getImageableY()
        );

        System.out.println(
                "Paper Imageable W  : "
                        + paper.getImageableWidth()
        );

        System.out.println(
                "Paper Imageable H  : "
                        + paper.getImageableHeight()
        );
    }


    private String getOrientationName(int orientation) {

        switch (orientation) {

            case PageFormat.PORTRAIT:
                return "PORTRAIT";

            case PageFormat.LANDSCAPE:
                return "LANDSCAPE";

            case PageFormat.REVERSE_LANDSCAPE:
                return "REVERSE_LANDSCAPE";

            default:
                return "UNKNOWN(" + orientation + ")";
        }
    }

    public String getPrintJobStatusFromCUPS(String jobId) {
        try {
            ProcessBuilder pb = new ProcessBuilder("curl", "http://localhost:9191/jobs/" + jobId);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                return "Failed to fetch job status. Exit code: " + exitCode;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error while getting job status: " + e.getMessage();
        }
    }

    public List<String> getAllPrinters() {
        List<String> printers = new ArrayList<>();
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            printers.add(service.getName());
        }
        return printers;
    }

    public String getDetailedPrintersInfo() {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell.exe",
                    "-Command",
                    "Get-Printer | Select-Object Name,ShareName,Location,DriverName,PortName,Comment,PrinterStatus | ConvertTo-Json"
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            process.waitFor();
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to get printer details: " + e.getMessage();
        }
    }
}