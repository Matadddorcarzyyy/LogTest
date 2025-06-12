package org.example.src;
import org.example.src.LogEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
    private final Pattern logPattern = Pattern.compile("\\[(.+?)\\] (\\w+) (balance inquiry|transferred|withdrew) (\\d+\\.\\d{2})(?: to (\\w+))?");

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public LogEntry parseLine(String line) throws IllegalArgumentException {
        Matcher matcher = logPattern.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Неверный формат строки лога: " + line);
        }
        LocalDateTime time = LocalDateTime.parse(matcher.group(1), formatter);
        String user = matcher.group(2);
        String operation = matcher.group(3);
        double amount = Double.parseDouble(matcher.group(4));
        String targetUser = matcher.group(5);
        return new LogEntry(time, user, operation, amount, targetUser);
    }

    public LogEntry createReceivedEntry(LogEntry transferEntry) {
        if (!transferEntry.getOperation().equals("transferred")) {
            throw new IllegalArgumentException("Можно создать запись 'received' только для операции 'transferred'");
        }
        return new LogEntry(
                transferEntry.getTimestamp(),
                transferEntry.getTargetUser(),
                "received",
                transferEntry.getAmount(),
                transferEntry.getUser()
        );
    }
}
