package com.dlepe.twitchchatanalyzer.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

/**
 * Provides a set of utility methods for parsing and handling raw log data provided from the
 * gempir/justlog library.
 */
@UtilityClass
public class LogParseUtils {

    private final static DateTimeFormatter LOG_TIMESTAMP_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss");
    private final static String VIDEO_TIMESTAMP_PATTERN = "([0-9]+)([hms])";

    /**
     * Implementation of the Knuth–Morris–Pratt string-searching algorithm to do an optimized
     * pattern match for a keyword in a given text.
     *
     * @param pattern the pattern to search for
     * @param text    the text to search within
     * @return the count of ocurrences of the pattern in the text
     */
    public static Long kmpSearch(String pattern, String text) {
        Long count = 0L;

        // base case 1: pattern is null or empty
        if (pattern == null || pattern.length() == 0) {
            return count;
        }

        // base case 2: text is NULL, or text's length is less than that of pattern's
        if (text == null || pattern.length() > text.length()) {
            return count;
        }

        char[] chars = pattern.toCharArray();

        // next[i] stores the index of the next best partial match
        int[] next = new int[pattern.length() + 1];
        for (int i = 1; i < pattern.length(); i++) {
            int j = next[i + 1];

            while (j > 0 && chars[j] != chars[i]) {
                j = next[j];
            }

            if (j > 0 || chars[j] == chars[i]) {
                next[i + 1] = j + 1;
            }
        }

        for (int i = 0, j = 0; i < text.length(); i++) {
            if (j < pattern.length() && text.charAt(i) == pattern.charAt(j)) {
                if (++j == pattern.length()) {
                    count++;
                }
            } else if (j > 0) {
                j = next[j];
                i--; // since `i` will be incremented in the next iteration
            }
        }

        return count;
    }

    public static LocalDateTime getTimestampToNearestMinute(final String dateStr) {
        final String timeStamp = StringUtils
            .trimTrailingCharacter(StringUtils.trimLeadingCharacter(dateStr, '['), ']');
        return LocalDateTime.parse(timeStamp, LOG_TIMESTAMP_FORMATTER)
            .truncatedTo(ChronoUnit.MINUTES);
    }

    public static Duration getVideoDurationFromString(@NonNull final String videoDuration) {
        final Pattern pattern = Pattern.compile(VIDEO_TIMESTAMP_PATTERN);
        final Matcher matcher = pattern.matcher(videoDuration);

        Duration duration = Duration.ofSeconds(0);
        while (matcher.find()) {
            final long timeValue = Long.parseLong(matcher.group(1));
            final String timeUnit = matcher.group(2);
            switch (timeUnit) {
                case "h":
                    duration = duration.plusHours(timeValue);
                    break;
                case "m":
                    duration = duration.plusMinutes(timeValue);
                    break;
                case "s":
                    duration = duration.plusSeconds(timeValue);
                    break;
            }
        }

        return duration;
    }
}
