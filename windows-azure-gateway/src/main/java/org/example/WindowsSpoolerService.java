package org.example;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Service
public class WindowsSpoolerService {

    public String getAllPrintJobs() {
        try {
            // This PowerShell block gets all jobs from all printers
            String script = """
            $allJobs = @()
            Get-Printer | ForEach-Object {
                $jobs = Get-PrintJob -PrinterName $_.Name -ErrorAction SilentlyContinue
                if ($jobs) { $allJobs += $jobs }
            }
            $allJobs | Select-Object PrinterName, ID, Name, JobStatus, Submitter, TimeJobSubmitted, PagesPrinted | ConvertTo-Json
        """;

            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-Command", script);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String json = reader.lines().collect(Collectors.joining("\n"));
            process.waitFor();
            return json;

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Failed to fetch print jobs: " + e.getMessage();
        }
    }


}
