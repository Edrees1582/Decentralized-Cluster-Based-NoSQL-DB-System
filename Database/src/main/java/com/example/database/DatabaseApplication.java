package com.example.database;

import com.example.database.models.DatabaseSystem;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class DatabaseApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(DatabaseApplication.class, args);
		DatabaseSystem.getInstance().useDatabase("bank");
		DatabaseSystem.getInstance().currentDatabase().useCollection("users");
	}

}
