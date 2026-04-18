package com.vmc.memoryleak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class MemoryLeakPrometheusApplication {

	private static final Logger logger = LoggerFactory.getLogger(MemoryLeakPrometheusApplication.class);

	public static void main(String[] args) {
		logger.info("MemoryLeakPrometheusApplication started");
		SpringApplication.run(MemoryLeakPrometheusApplication.class, args);
	}

}
