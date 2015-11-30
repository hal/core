package org.jboss.as.console.client.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Merges entries from {@code UIConstants.properties} into {@code UIConstants_??.properties}.
 *
 * @author Harald Pehl
 */
public class ConstantsMerger {

    public static void main(final String[] args) throws Exception {

        Path rootPath = Paths.get(UIConstants.class.getResource("UIConstants.properties").toURI());
        Map<String, String> root = readEntries(rootPath);
        for (Path languagePath : Files.newDirectoryStream(rootPath.getParent(), "UIConstants_*.properties")) {
            Map<String, String> language = readEntries(languagePath);
            Map<String, String> combined = new HashMap<>(language);
            root.forEach(combined::putIfAbsent);

            Path tmpDir = Paths.get("/tmp/i18n");
            //noinspection ResultOfMethodCallIgnored
            tmpDir.toFile().mkdir();
            Path tmpFile = Paths.get("/tmp/i18n", languagePath.getFileName().toString());
            System.out.printf("Merging %d entries into %s\n", combined.size(), tmpFile);
            Files.write(tmpFile, combined.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(toList()));
        }
    }

    private static Map<String, String> readEntries(final Path path) throws IOException {
        return Files.readAllLines(path)
                .stream()
                .map(s -> s.split("=", 2))
                .collect(toMap(parts -> parts[0], parts -> parts[1]));
    }
}