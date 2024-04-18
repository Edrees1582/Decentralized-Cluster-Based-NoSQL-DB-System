package com.example.bootstrap.controllers;

import com.example.bootstrap.models.Database;
import com.example.bootstrap.models.DatabaseSystem;
import com.example.bootstrap.services.NodesCommunicationServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@Controller
@RequestMapping("/database")
public class DatabaseController {
    @Autowired
    Environment environment;

    @PostMapping("")
    public ResponseEntity<String> addDatabase(@RequestParam("databaseName") String databaseName,
                                              @RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        DatabaseSystem.getInstance().addDatabase(new Database(databaseName));
        if (!isBroadcast)
            NodesCommunicationServices.broadcastChanges("http://***:" +
                            environment.getProperty("local.server.port") +
                            "/database?databaseName=" + databaseName,
                    HttpMethod.POST,
                    Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                    NodesCommunicationServices.getIP(), "");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<String> getCurrentDatabase() throws IOException {
        DatabaseSystem databaseSystem = DatabaseSystem.getInstance();
        return new ResponseEntity<>(databaseSystem.currentDatabase().getName(), HttpStatus.OK);
    }

    @PostMapping("/use")
    public ResponseEntity<String> useDatabase(@RequestParam("databaseName") String databaseName,
                                              @RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        DatabaseSystem databaseSystem = DatabaseSystem.getInstance();
        databaseSystem.useDatabase(databaseName);
        if (!isBroadcast)
            NodesCommunicationServices.broadcastChanges("http://***:" +
                            environment.getProperty("local.server.port") +
                            "/database/use?databaseName=" + databaseName,
                    HttpMethod.POST,
                    Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                    NodesCommunicationServices.getIP(), "");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteCurrentDatabase(@RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        DatabaseSystem.getInstance().deleteCurrentDatabase();
        if (!isBroadcast)
            NodesCommunicationServices.broadcastChanges("http://***:" +
                            environment.getProperty("local.server.port") +
                            "/database",
                    HttpMethod.DELETE,
                    Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                    NodesCommunicationServices.getIP(), "");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<Set<String>> getAllDatabases() throws IOException {
        return new ResponseEntity<>(DatabaseSystem.getInstance().showDatabases(), HttpStatus.OK);
    }
}