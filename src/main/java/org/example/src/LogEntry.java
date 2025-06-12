package org.example.src;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {
    private final LocalDateTime timestamp;
    private final String user;
    private final String operation;
    private final double amount;
    private final String targetUser;

    public LogEntry(LocalDateTime timestamp, String user, String operation, double amount, String targetUser) {
        this.timestamp = timestamp;
        this.user = user;
        this.operation = operation;
        this.amount = amount;
        this.targetUser = targetUser;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }

    public String getOperation() {
        return operation;
    }

    public double getAmount() {
        return amount;
    }

    public String getTargetUser() {
        return targetUser;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(timestamp.format(formatter)).append("] ");
        sb.append(user).append(" ").append(operation);
        if (!operation.equals("final balance")) {
            sb.append(" ").append(String.format("%.2f", amount));
        } else {
            sb.append(" ").append(String.format("%.2f", amount));
        }
        if (targetUser != null && (operation.equals("transferred") || operation.equals("received"))) {
            sb.append(operation.equals("transferred") ? " to " : " from ").append(targetUser);
        }
        return sb.toString();
    }
}