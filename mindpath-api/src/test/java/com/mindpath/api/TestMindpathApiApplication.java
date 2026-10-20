package com.mindpath.api;

import org.springframework.boot.SpringApplication;

public class TestMindpathApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(MindpathApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
