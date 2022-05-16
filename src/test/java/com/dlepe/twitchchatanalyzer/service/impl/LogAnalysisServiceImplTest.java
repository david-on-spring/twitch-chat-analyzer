package com.dlepe.twitchchatanalyzer.service.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogAnalysisServiceImplTest {

    @Test
    void testExtractHoursFromDuration() {
        final String durationText = "8h18m57s";

        Pattern pattern = Pattern.compile("^(\\d)+h-(\\d)+m-(\\d)+s$");
        Matcher matcher = pattern.matcher(durationText);
        Assert.assertTrue(matcher.find());
        Assert.assertEquals(8, matcher.group(0));
        Assert.assertEquals(18, matcher.group(1));
        Assert.assertEquals(57, matcher.group(2));
    }
}
