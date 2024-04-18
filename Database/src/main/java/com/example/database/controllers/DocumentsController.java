package com.example.database.controllers;

import com.example.database.models.Collection;
import com.example.database.models.Database;
import com.example.database.models.DatabaseSystem;
import com.example.database.models.Document;
import com.example.database.models.Property;
import com.example.database.services.LoadBalancerService;
import com.example.database.services.NodesCommunicationServices;
import com.example.database.services.SchemaValidator;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@Controller
@RequestMapping("/document")
public class DocumentsController {
    @Autowired
    Environment environment;

    @PostMapping("")
    public ResponseEntity<Document> addDocument(@RequestBody String json,
                                                @RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        Database database = DatabaseSystem.getInstance().currentDatabase();
        if (SchemaValidator.isValidJson(database.currentCollection().getSchema(), json)) {
            JSONObject jsonObject = new JSONObject(json);
            if (isBroadcast) {
                Document document = new Document(jsonObject, database.getName(), database.currentCollection().getName(), jsonObject.getString("_id"));
                database.currentCollection().addDocument(document);
                LoadBalancerService.getInstance(Objects.requireNonNull(environment.getProperty("nodes", String[].class))).getNextNode();
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
            else {
                jsonObject.put("affinityNode", LoadBalancerService.getInstance(Objects.requireNonNull(environment.getProperty("nodes", String[].class))).getNextNode());
                Document document = new Document(jsonObject, database.getName(), database.currentCollection().getName(), UUID.randomUUID().toString());
                database.currentCollection().addDocument(document);
                NodesCommunicationServices.broadcastChanges("http://***:" +
                                environment.getProperty("local.server.port") +
                                "/document",
                        HttpMethod.POST,
                        Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                        NodesCommunicationServices.getIP(), document.getProperties().toString());
                return new ResponseEntity<>(document, HttpStatus.OK);
            }
        }
        else
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getDocumentById(@PathVariable String id) throws IOException {
        Document document = DatabaseSystem.getInstance().currentDatabase().currentCollection().getDocumentById(id);
        if (document == null) // "Document with id " + id + ", not found"
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        else
            return new ResponseEntity<>(document.getProperties().toString(), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable String id,
                                                 @RequestParam(value = "isRedirected", defaultValue = "false", required = false) boolean isRedirected,
                                                 @RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        Collection collection = DatabaseSystem.getInstance().currentDatabase().currentCollection();
        Document document = collection.getDocumentById(id);
        if (document == null) // "Document with id " + id + ", not found"
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);

        if (isRedirected) {
            System.out.println("This request was redirected");
            collection.deleteDocument(id);
            if (!isBroadcast)
                NodesCommunicationServices.broadcastChanges("http://***:" +
                                environment.getProperty("local.server.port") +
                                "/document/" + id,
                        HttpMethod.DELETE,
                        Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                        document.getProperty("affinityNode").toString(), "");
            return new ResponseEntity<>(document.getProperties().toString(), HttpStatus.OK);
        }
        else if (isBroadcast) {
            collection.deleteDocument(id);
            return new ResponseEntity<>(document.getProperties().toString(), HttpStatus.OK);
        }
        else {
            System.out.println("This request is not redirected");
            if (document.getProperty("affinityNode").equals(NodesCommunicationServices.getIP())) {
                System.out.println("updating from non-redirected request");
                collection.deleteDocument(id);
                NodesCommunicationServices.broadcastChanges("http://***:" +
                                environment.getProperty("local.server.port") +
                                "/document/" + id,
                        HttpMethod.DELETE,
                        Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                        document.getProperty("affinityNode").toString(), "");
                return new ResponseEntity<>(document.getProperties().toString(), HttpStatus.OK);
            }
            else {
                ResponseEntity<String> response = NodesCommunicationServices
                        .redirectRequest("http://" +
                                        document.getProperty("affinityNode") +
                                        ":" + environment.getProperty("local.server.port") +
                                        "/document/" + id,
                                HttpMethod.DELETE);
                return new ResponseEntity<>(response.getBody(), response.getStatusCode());
            }
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Document>> getDocuments() throws IOException {
        return new ResponseEntity<>(DatabaseSystem.getInstance().currentDatabase().currentCollection().getDocuments(), HttpStatus.OK);
    }

    @GetMapping("/{id}/property")
    public ResponseEntity<String> getDocumentPropertyByKey(@PathVariable String id,
                                                           @RequestParam("propertyKey") String propertyKey) throws IOException {
        Document document = DatabaseSystem.getInstance().currentDatabase().currentCollection().getDocumentById(id);
        if (document == null) // "Document with id " + id + ", not found"
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        else if (document.getProperties().has(propertyKey))
            return new ResponseEntity<>(document.getProperties().get(propertyKey).toString(), HttpStatus.OK);
        else // "Key: \"" + propertyKey + "\" not found in document with id: " + id
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }

    @GetMapping("/property")
    public ResponseEntity<List<String>> getDocumentByProperty(@RequestParam("propertyKey") String propertyKey,
                                                              @RequestParam("propertyValue") String propertyValue) throws IOException {
        List<String> documents = DatabaseSystem.getInstance()
                .currentDatabase()
                .currentCollection()
                .getDocumentByKeyValue(propertyKey, propertyValue)
                .stream()
                .map(document -> document.getProperties().toString())
                .toList();
        if (documents.isEmpty()) // "No documents found with key: \"" + propertyKey + "\", and value: \"" + propertyValue + "\""
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        else
            return new ResponseEntity<>(documents, HttpStatus.OK);
    }

    @PutMapping("/{id}/property")
    public ResponseEntity<String> updateDocumentProperty(@PathVariable String id,
                                                         @RequestParam("propertyKey") String propertyKey,
                                                         @RequestParam("propertyValue") String propertyValue,
                                                         @RequestParam(value = "documentVersion", defaultValue = "-1", required = false) long documentVersion,
                                                         @RequestParam(value = "isRedirected", defaultValue = "false", required = false) boolean isRedirected,
                                                         @RequestParam(value = "isBroadcast", defaultValue = "false", required = false) boolean isBroadcast) throws IOException {
        Document document = DatabaseSystem.getInstance().currentDatabase().currentCollection().getDocumentById(id);
        if (document == null) // "Document with id " + id + ", not found"
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        if (isRedirected) {
            System.out.println("This request was redirected");
            if (document.getVersion() != documentVersion)
                return new ResponseEntity<>("Document version " + documentVersion + " does not match expected version " + document.getVersion(), HttpStatus.CONFLICT);
            else {
                if (document.getProperties().has(propertyKey)) {
                    document.updateProperty(new Property(propertyKey, propertyValue));
                    if (!isBroadcast)
                        NodesCommunicationServices.broadcastChanges("http://***:" +
                                        environment.getProperty("local.server.port") +
                                        "/document/" + id +
                                        "/property?propertyKey=" + propertyKey +
                                        "&propertyValue=" + propertyValue +
                                        "&documentVersion=" + document.getVersion(),
                                HttpMethod.PUT,
                                Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                                document.getProperty("affinityNode").toString(), "");
                    return new ResponseEntity<>(document.getProperties().toString(), HttpStatus.OK);
                }
                else // "Key: \"" + propertyKey + "\" not found in document with id: " + id
                    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        }
        else if (isBroadcast) {
            if (document.getProperties().has(propertyKey)) {
                document.setVersion(documentVersion - 1);
                document.updateProperty(new Property(propertyKey, propertyValue));
                return new ResponseEntity<>(document.getProperties().toString(), HttpStatus.OK);
            }
            else // "Key: \"" + propertyKey + "\" not found in document with id: " + id
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        else {
            System.out.println("This request is not redirected");
            if (document.getProperty("affinityNode").equals(NodesCommunicationServices.getIP())) {
                System.out.println("updating from non-redirected request");
                if (document.getProperties().has(propertyKey)) {
                    document.updateProperty(new Property(propertyKey, propertyValue));
                    NodesCommunicationServices.broadcastChanges("http://***:" +
                                    environment.getProperty("local.server.port") +
                                    "/document/" + id +
                                    "/property?propertyKey=" + propertyKey +
                                    "&propertyValue=" + propertyValue +
                                    "&documentVersion=" + document.getVersion(),
                            HttpMethod.PUT,
                            Objects.requireNonNull(environment.getProperty("nodes", String[].class)),
                            document.getProperty("affinityNode").toString(), "");
                    return new ResponseEntity<>(document.getProperties().toString(), HttpStatus.OK);
                }
                else // "Key: \"" + propertyKey + "\" not found in document with id: " + id
                    return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
            else {
                ResponseEntity<String> response = NodesCommunicationServices.redirectRequest("http://" +
                                document.getProperty("affinityNode") +
                                ":" + environment.getProperty("local.server.port") +
                                "/document/" + id +
                                "/property?propertyKey=" + propertyKey +
                                "&propertyValue=" + propertyValue +
                                "&documentVersion=" + document.getVersion(),
                        HttpMethod.PUT);
                return new ResponseEntity<>(response.getBody(), response.getStatusCode());
            }
        }
    }

    @GetMapping("/owned")
    public ResponseEntity<Integer> getOwnedDocuments() throws IOException { // In current collection only
        List<Document> documents = getDocuments().getBody();
        int ownedDocuments = 0;
        if (documents != null)
            for (Document document : documents)
                if (document.getProperty("affinityNode").equals(NodesCommunicationServices.getIP()))
                    ownedDocuments++;
        return new ResponseEntity<>(ownedDocuments, HttpStatus.OK);
    }
}
