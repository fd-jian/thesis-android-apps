package de.dipf.edutec.thriller.experiencesampling.sensorservice;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public class SensorDataFileLogger {

    private static final String TAG = "wear:" + SensorDataFileLogger.class.getSimpleName();
    private final File file;

    private SensorDataFileLogger(File outputStream) {
        this.file = outputStream;
    }

    public static SensorDataFileLogger create(Context applicationContext) {
                Instant date = Instant.now();
        ZonedDateTime now = ZonedDateTime.ofInstant(
                date, ZoneId.systemDefault());

        File externalFilesDir = Optional.ofNullable(applicationContext
                .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
                .orElseThrow(() -> new RuntimeException("Local Download directory not found/inaccessible."));

        // Cleanup old files
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(
                String.format(
                        "regex:.*%s(?!%s).*",
                        DateTimeFormatter.ofPattern("yyyy-MM-").format(now),
                        String.format(Locale.getDefault(), "%02d", now.getDayOfMonth())));

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                Paths.get(externalFilesDir.getPath()), pathMatcher::matches)) {
            dirStream.forEach(path -> {
                Log.d(TAG, String.format("Deleting %s", path.toString()));
                if(!path.toFile().delete()){
                    Log.d(TAG, "Failed to delete old log file.");
                }
            });
        } catch (IOException e) {
            throw new RuntimeException();
        }

        String fileName = String.format("accelero_dump%s",
                DateTimeFormatter.ofPattern("yyyy-MM-dd").format(now));
        File file = new File(externalFilesDir, fileName);

        try {
            if(!file.createNewFile()) {
                Log.d(TAG, String.format(
                        "Log file '%s' already exists. Appending new log output to it.",
                        file.getAbsolutePath()));
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String absolutePath = file.getAbsolutePath();

        Log.d(TAG, String.format("Created FileOutputStream for file at path: %s", absolutePath));

        return new SensorDataFileLogger(file);
    }

    public OutputStream getOutputStream() {
        try {
            return new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
