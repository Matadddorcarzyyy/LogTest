package org.example.src;

import org.example.src.LogParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class LogProcessor {
    private final Map<String, List<org.example.src.LogEntry>> userLogs = new HashMap<>();
    private final LogParser parser = new LogParser();
    private final Path inputDir;
    private final Path outputDir;
    private final Random random = new Random();
    private final String runTimestamp;

    public LogProcessor(String inputDirPath) throws IOException {
        this.inputDir = Paths.get(inputDirPath);
        this.runTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        this.outputDir = inputDir.resolve("transactions_by_users_" + runTimestamp);
        Files.createDirectories(inputDir); // Create input directory if it doesn't exist
        Files.createDirectories(outputDir); // Create output directory
    }

    public void readAndProcessLogs() {
        try (Stream<Path> files = Files.list(inputDir).filter(p -> p.toString().endsWith(".log"))) {
            files.forEach(file -> {
                try {
                    List<String> lines = Files.readAllLines(file);
                    for (String line : lines) {
                        try {
                            org.example.src.LogEntry entry = parser.parseLine(line);
                            userLogs.computeIfAbsent(entry.getUser(), k -> new ArrayList<>()).add(entry);
                            if (entry.getOperation().equals("transferred")) {
                                org.example.src.LogEntry receivedEntry = parser.createReceivedEntry(entry);
                                userLogs.computeIfAbsent(receivedEntry.getUser(), k -> new ArrayList<>()).add(receivedEntry);
                            }
                        } catch (IllegalArgumentException e) {
                            System.err.println("Skipping invalid line in " + file + ": " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file " + file + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("Error listing files in " + inputDir + ": " + e.getMessage());
        }
    }

    public void sortLogsByTimestamp() {
        userLogs.values().forEach(entries -> entries.sort(Comparator.comparing(org.example.src.LogEntry::getTimestamp)));
    }

    public void calculateAndAppendFinalBalances() {
        for (Map.Entry<String, List<org.example.src.LogEntry>> entry : userLogs.entrySet()) {
            String user = entry.getKey();
            List<org.example.src.LogEntry> logs = entry.getValue();
            double balance = 0.0;
            for (org.example.src.LogEntry log : logs) {
                String operation = log.getOperation();
                if (operation.equals("transferred") || operation.equals("withdrew")) {
                    balance -= log.getAmount();
                } else if (operation.equals("received")) {
                    balance += log.getAmount();
                }
            }
            logs.add(new org.example.src.LogEntry(LocalDateTime.now(), user, "final balance", balance, null));
        }
    }

    public void writeUserLogs() {
        for (Map.Entry<String, List<org.example.src.LogEntry>> entry : userLogs.entrySet()) {
            String user = entry.getKey();
            List<org.example.src.LogEntry> logs = entry.getValue();
            Path userDir = outputDir.resolve(user);
            try {
                Files.createDirectories(userDir);
            } catch (IOException e) {
                System.err.println("Error creating directory for user " + user + ": " + e.getMessage());
                continue;
            }
            Path outputFile = userDir.resolve(user + "_run_" + runTimestamp + ".log");
            List<String> lines = new ArrayList<>();
            for (org.example.src.LogEntry log : logs) {
                lines.add(log.toString());
            }
            try {
                Files.write(outputFile, lines, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                System.err.println("Error writing file " + outputFile + ": " + e.getMessage());
            }
        }
    }

    public void generateRandomAction() {
        String[] users = userLogs.keySet().toArray(new String[0]);
        if (users.length == 0) {
            // Создаем тестовых пользователей, если их нет
            int userCount = random.nextInt(10) + 5; // Рандомное число пользователей от 5 до 15
            users = new String[userCount];
            for (int i = 0; i < userCount; i++) {
                String user = "testUser" + (i + 1);
                users[i] = user;
                userLogs.put(user, new ArrayList<>());
                userLogs.get(user).add(new org.example.src.LogEntry(LocalDateTime.now(), user, "balance inquiry", 0.0, null));
            }
            System.out.println("Created " + userCount + " test users as no users were found.");
        }
        simulateUserActions(users);
    }

    private void simulateUserActions(String[] users) {
        int threadCount = random.nextInt(5) + 3; // Рандомное число потоков от 3 до 8
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int actionCount = random.nextInt(20) + 10; // Рандомное число действий от 10 до 30
        System.out.println("Simulating " + actionCount + " actions with " + threadCount + " threads.");
        Path logFile = outputDir.resolve("simulation_log_" + runTimestamp + ".log");
        List<String> logLines = new ArrayList<>();
        for (int i = 0; i < actionCount; i++) {
            final int actionIndex = i;
            executor.submit(() -> {
                String user = users[random.nextInt(users.length)];
                String[] actions = {"balance inquiry", "withdrew", "transferred"};
                String action = actions[random.nextInt(actions.length)];
                double amount = action.equals("balance inquiry") ? 0.0 : random.nextDouble() * 100;
                String targetUser = action.equals("transferred") ? users[random.nextInt(users.length)] : null;
                if (action.equals("transferred") && targetUser != null && targetUser.equals(user)) {
                    targetUser = users[(random.nextInt(users.length - 1) + 1) % users.length];
                }
                org.example.src.LogEntry newEntry = new org.example.src.LogEntry(LocalDateTime.now(), user, action, amount, targetUser);
                synchronized (userLogs) {
                    userLogs.computeIfAbsent(user, k -> new ArrayList<>()).add(newEntry);
                    if (action.equals("transferred") && targetUser != null) {
                        org.example.src.LogEntry receivedEntry = parser.createReceivedEntry(newEntry);
                        userLogs.computeIfAbsent(receivedEntry.getUser(), k -> new ArrayList<>()).add(receivedEntry);
                    }
                    logLines.add("Action " + actionIndex + ": " + newEntry.toString());
                }
                System.out.println("Action " + actionIndex + ": " + newEntry);
                try {
                    Thread.sleep(random.nextInt(500)); // Задержка для имитации реального времени
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted: " + e.getMessage());
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
            Files.write(logFile, logLines, StandardOpenOption.CREATE_NEW);
            System.out.println("Simulation log saved to " + logFile);
        } catch (InterruptedException e) {
            System.err.println("Executor termination interrupted: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error writing simulation log: " + e.getMessage());
        }
        System.out.println("Simulation completed.");
    }
}