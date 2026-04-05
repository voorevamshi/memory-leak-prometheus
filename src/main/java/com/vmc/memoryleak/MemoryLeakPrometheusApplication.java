package com.vmc.memoryleak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MemoryLeakPrometheusApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemoryLeakPrometheusApplication.class, args);
	}

}
