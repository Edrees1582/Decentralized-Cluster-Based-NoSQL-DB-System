package com.example.database.controllers;

import com.example.database.models.DatabaseSystem;
import com.example.database.services.NodesCommunicationServices;
import com.example.database.services.SchemaValidator;
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
@RequestMapping("/collection")
public class CollectionController {
    @Autowired
    Environment environment;
    
    @PostMapping("")
    public ResponseEntity<String> addCollection(@RequestParam("collectionName") String collectionName,
                                                @RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        DatabaseSystem.getInstance().currentDatabase().addCollection(collectionName);
        if (!isBroadcast)
            NodesCommunicationServices.broadcastChanges("http://***:" +
                            environment.getProperty("local.server.port") +
                            "/collection?collectionName=" + collectionName,
                    HttpMethod.POST,
                    Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                    NodesCommunicationServices.getIP(), "");

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<String> getCurrentCollection() throws IOException {
        return new ResponseEntity<>(DatabaseSystem.getInstance().currentDatabase().currentCollection().getName(), HttpStatus.OK);
    }

    @PostMapping("/use")
    public ResponseEntity<String> useCollection(@RequestParam("collectionName") String collectionName,
                                                @RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        DatabaseSystem.getInstance().currentDatabase().useCollection(collectionName);
        if (!isBroadcast)
            NodesCommunicationServices.broadcastChanges("http://***:" +
                            environment.getProperty("local.server.port") +
                            "/collection/use?collectionName=" + collectionName,
                    HttpMethod.POST,
                    Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                    NodesCommunicationServices.getIP(), "");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteCurrentCollection(@RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        DatabaseSystem.getInstance().currentDatabase().deleteCurrentCollection();
        if (!isBroadcast)
            NodesCommunicationServices.broadcastChanges("http://***:" +
                            environment.getProperty("local.server.port") +
                            "/collection",
                    HttpMethod.DELETE,
                    Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                    NodesCommunicationServices.getIP(), "");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<Set<String>> getAllCollections() throws IOException {
        return new ResponseEntity<>(DatabaseSystem.getInstance().currentDatabase().showCollections(), HttpStatus.OK);
    }

    @PostMapping("/schema")
    public ResponseEntity<String> setCollectionSchema(@RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast,
                                                      @RequestBody String collectionSchema) throws IOException {
        if (SchemaValidator.isValidJsonSchema(collectionSchema)) {
            DatabaseSystem.getInstance().currentDatabase().currentCollection().setSchema(collectionSchema);
            if (!isBroadcast)
                NodesCommunicationServices.broadcastChanges("http://***:" +
                                environment.getProperty("local.server.port") +
                                "/collection/schema",
                        HttpMethod.POST,
                        Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                        NodesCommunicationServices.getIP(), collectionSchema);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        else return new ResponseEntity<>("Invalid JSON Schema", HttpStatus.BAD_REQUEST);
    }
}