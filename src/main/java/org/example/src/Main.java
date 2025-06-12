package org.example.src;
import org.example.src.LogEntry;
import org.example.src.LogProcessor;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String inputDir = args.length > 0 ? args[0] : "resources";

        try {
            LogProcessor processor = new LogProcessor(inputDir);
            processor.readAndProcessLogs();
            processor.sortLogsByTimestamp();
            processor.generateRandomAction();
            processor.calculateAndAppendFinalBalances();
            processor.writeUserLogs();
            System.out.println("Processing completed. Check " + inputDir + "/transactions_by_users for results.");
            System.out.println("Processing completed. Check " + inputDir + "/transactions_by_users for results.");
        } catch (IOException e) {
            System.err.println("Error processing logs: " + e.getMessage());
        }
    }
}
