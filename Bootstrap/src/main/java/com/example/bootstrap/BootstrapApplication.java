package com.example.bootstrap;

import com.example.bootstrap.models.DatabaseSystem;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class BootstrapApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(BootstrapApplication.class, args);
		DatabaseSystem.getInstance().useDatabase("bootstrap");
		DatabaseSystem.getInstance().currentDatabase().useCollection("users");
	}

}
