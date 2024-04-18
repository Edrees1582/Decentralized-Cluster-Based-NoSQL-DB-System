package com.example.database.models;

import com.example.database.services.FileServices;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Document {
    private final String id;
    private final FileServices fileServices;
    private final JSONObject properties;
    private final ReentrantReadWriteLock lock;

    public Document(JSONObject properties, String databaseName, String collectionName, String... id) throws IOException {
        if (id.length != 0) this.id = id[0];
        else this.id = UUID.randomUUID().toString();
        fileServices = new FileServices(databaseName, collectionName, false);
        this.properties = properties;
        lock = new ReentrantReadWriteLock();
    }

    public String getId() {
        return id;
    }

    public JSONObject getProperties() {
        return properties;
    }

    public void updateProperty(Property property) {
        lock.writeLock().lock();
        try {
            properties.put(property.key(), property.value());
            properties.put("last-modified", LocalDateTime.now());
            properties.put("version", properties.getLong("version") + 1);
            fileServices.updateDocumentById(id, property, getVersion());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public long getVersion() {
        return properties.getLong("version");
    }

    public void setVersion(long newVersion) {
        properties.put("version", newVersion);
    }

    @Override
    public String toString() {
        return "Document: " + properties;
    }
}
