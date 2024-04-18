package com.example.database.models;

import com.example.database.services.FileServices;
import com.example.database.services.IndexService;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Collection {
    private final String name;
    private final FileServices fileServices;
    private final Map<String, Document> documents;
    private final IndexService indexService;
    private String schema;
    private final ReentrantReadWriteLock lock;

    public Collection(String databaseName, String name) throws IOException {
        this.name = name;
        fileServices = new FileServices(databaseName, name, false);
        documents = new TreeMap<>();
        indexService = IndexService.getInstance();
        schema = fileServices.loadJsonSchema();
        lock = new ReentrantReadWriteLock();
    }

    public void loadDocuments() {
        List<Document> foundDocuments = fileServices.getAllDocuments();
        for (Document document : foundDocuments) documents.put(document.getId(), document);
    }

    public String getName() {
        return name;
    }

    public List<Document> getDocuments() {
        return documents.values().stream().toList();
    }

    public Document getDocumentById(String id) {
        return documents.get(id);
    }

    public List<Document> getDocumentByKeyValue(String key, String value) {
        List<Document> foundDocuments = new ArrayList<>();
        JSONArray propertyIndexes = indexService.searchPropertyIndex(key, value);

        if (propertyIndexes != null)
            for (Object propertyIndex : propertyIndexes) {
                foundDocuments.add(fileServices.getDocumentById(getFileName(propertyIndex.toString())));
            }

        return foundDocuments;
    }

    public void addDocument(Document document) {
        fileServices.addDocumentFile(document.getId(), document.getProperties());
        documents.put(document.getId(), document);
    }

    public void deleteDocument(String id) {
        lock.writeLock().lock();
        try {
            fileServices.deleteDocument(id);
            documents.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getSchema() {
        if (schema == null) throw new NullPointerException("Schema is null");
        return schema;
    }

    public void setSchema(String schema) {
        fileServices.addJsonSchema(schema);
        this.schema = schema;
    }

    public String getFileName(String filePath) {
        File file = new File(filePath);
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex != 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
}
