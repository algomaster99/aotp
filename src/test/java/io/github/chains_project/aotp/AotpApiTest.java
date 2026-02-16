package io.github.chains_project.aotp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import io.github.chains_project.aotp.oops.klass.ClassEntry;

class AotpApiTest {

    private static final Path RESOURCE_DIR = Paths.get("src/test/resources");
    private static final Path AOT_RESOURCE = RESOURCE_DIR.resolve("test.aot");
    private static final Path CSV_RESOURCE = RESOURCE_DIR.resolve("classes.csv");

    /**
     * Reads classes.csv (format: "className,size" with '/' e.g. java/lang/String),
     * looks up each class in test.map via AotpApi, and asserts the size matches.
     * Classes not present in the AOT file are skipped.
     */
    @Test
    void classSizesMatchCsv() throws IOException, URISyntaxException {
        // arrange
        List<String> lines = java.nio.file.Files.readAllLines(CSV_RESOURCE);
        String aotPathStr = AOT_RESOURCE.toString();
        Map<String, Integer> expectedSizes = lines.stream()
                .filter(line -> !line.isBlank())
                .collect(Collectors.toMap(
                        line -> line.substring(0, line.lastIndexOf(',')).trim(),
                        line -> Integer.parseInt(line.substring(line.lastIndexOf(',') + 1).trim())));

        List<String> classNames = expectedSizes.keySet().stream().toList();
        Map<ClassEntry, Integer> sizes = AotpApi.getClassSizes(aotPathStr, classNames);

        assertEquals(expectedSizes.size(), sizes.size(), "All classes should be found in " + AOT_RESOURCE);
        for (Map.Entry<ClassEntry, Integer> entry : sizes.entrySet()) {
            if (entry.getKey().isInterface()) {
                // TODO: embedded interfaces are not handled yet
                // https://github.com/openjdk/jdk/blob/jdk-27%2B7/src/hotspot/share/oops/instanceKlass.hpp#L286
                continue;
            }
            String className = entry.getKey().getName();
            if (!expectedSizes.containsKey(className)) {
                continue;
            }
            assertEquals(expectedSizes.get(className), entry.getValue(),
                    "Class size mismatch for: " + className);
        }
    }
}
