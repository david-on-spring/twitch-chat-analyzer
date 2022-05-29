package com.dlepe.twitchchatanalyzer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {

    @SneakyThrows
    public static String getTestChatLogs(final String filename) {
        final String pathString = ClassLoader.getSystemClassLoader()
            .getResource("chatlogs/" + filename).getPath();
        return Files.readString(Paths.get(pathString), StandardCharsets.UTF_8);
    }

}
