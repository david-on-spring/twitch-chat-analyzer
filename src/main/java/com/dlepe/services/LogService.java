package com.dlepe.services;

import com.dlepe.justlog.api.JustlogApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LogService {
    private final JustlogApi justLogApi;

    public void getLogData() {
        log.info(justLogApi.channels().toString());
    }


}
