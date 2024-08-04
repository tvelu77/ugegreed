package fr.uge.ugegreed.works;

import fr.uge.ugegreed.records.Packet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.*;

public class ResponseCollector {
    private static final Logger logger = Logger.getLogger(ResponseCollector.class.getName());

    private final HashMap<Long, Path> resultsFiles = new HashMap<>();

    private static Path defaultFilePath(long requestId) {
        return Path.of(requestId + " results.txt");
    }

    public void addResultFilename(long requestId, Path filename) {
        resultsFiles.put(requestId, filename);
    }

    public Path getResultFilePath(long requestId) {
        return resultsFiles.getOrDefault(requestId, defaultFilePath(requestId));
    }

    public void saveResponseIntoResultFile(Packet.Work.WorkResponse workResponse){
        var requestId = workResponse.requestId();
        var filePath = resultsFiles.getOrDefault(requestId, defaultFilePath(requestId));
        try {
            Files.writeString(filePath, workResponse.response().toString(), StandardCharsets.UTF_8, CREATE, WRITE, APPEND);
        } catch(IOException e) {
            logger.severe("Fatal exception in Thread : " + Thread.currentThread().getName());
        }
    }
}
