package com.dlepe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;



@SpringBootApplication
@ComponentScan(basePackages = "com.gempir.justlog")
public class TwitchChatAnalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwitchChatAnalyzerApplication.class, args);
	}

}
