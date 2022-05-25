package com.dlepe.twitchchatanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication
@EnableAsync
public class TwitchChatAnalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwitchChatAnalyzerApplication.class, args);
	}

}
