package fr.uge.ugegreed;

import fr.uge.ugegreed.records.Task;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class Console {
    public record StartCommand(String jarUrl, String className, long startRange, long endRange, Path fileName) {
        private static StartCommand fromString(String command) {
            var args = command.split("\\s+");
            if(args.length < 5) {
                logger.info("missing arguments");
                return null;
            }

            String jarUrl = null;
            String className = null;
            long startRange = 0L;
            long endRange = 0L;
            Path fileName = null;

            for(var arg : args) {
                var cmd = arg.split("=");
                if(cmd.length < 2) {
                    logger.info("Usage : argument_name=argument_value");
                    return null;
                }

                var argName = cmd[0];
                var argValue = cmd[1];
                switch (argName) {
                    case "url-jar" -> jarUrl = argValue;
                    case "fqn" -> className = argValue;
                    case "start-range" -> startRange = Long.parseLong(argValue);
                    case "end-range" -> endRange = Long.parseLong(argValue);
                    case "filename" -> fileName = Path.of(argValue);
                }
            }

            if(jarUrl == null) {
                logger.info("missing jar url");
                return null;
            }

            if(className == null) {
                logger.info("missing fqdn");
                return null;
            }

            if(fileName == null) {
                logger.info("missing file name");
                return null;
            }

            if(endRange < 0 || startRange > endRange) {
                logger.info("bad start and/or end range");
                return null;
            }

            return new StartCommand(jarUrl, className, startRange, endRange, fileName);
        }
    }

    private static final Logger logger = Logger.getLogger(Console.class.getName());

    private final Consumer<Task> onStartCommand;
    private final Runnable onDisconnectCommand;
    private final Runnable route;

    private final Object lock = new Object();

    public Console(Consumer<Task> onStartCommand, Runnable onDisconnectCommand, Runnable route) {
        this.onStartCommand = Objects.requireNonNull(onStartCommand);
        this.onDisconnectCommand = Objects.requireNonNull(onDisconnectCommand);
        this.route = route;
    }

    private void handleCommand(String command) {
        switch (command) {
            case "disconnect" -> {
                synchronized (lock) {
                    onDisconnectCommand.run();
                }
            }
            case "route" -> route.run();
            default -> {
                if(!command.startsWith("start")) {
                    System.err.println("Unknown command, please retry !");
                } else {
                    var args = command.replaceFirst("start ", "");
                    var startCommand = StartCommand.fromString(args);
                    if(startCommand == null) {
                        return;
                    }
                    var task = Task.fromCommand(startCommand);
                    synchronized (lock) {
                        onStartCommand.accept(task);
                    }
                }
            }
        }
    }

    public void run() {
        try (var scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine() && !Thread.interrupted()) {
                var msg = scanner.nextLine();
                handleCommand(msg);
            }

            logger.info("Console thread stopping");
        }
    }
}
