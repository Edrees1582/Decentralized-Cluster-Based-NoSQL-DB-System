package com.example.database.models;

import com.example.database.services.FileServices;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Database implements DatabaseInterface {
    private final String name;
    private final Map<String, Collection> collections;
    private Collection currentCollection;
    private final ReentrantReadWriteLock lock;

    public Database(String name) throws IOException {
        this.name = name;
        collections = new HashMap<>();
        FileServices fileServices = new FileServices(name, "", false);
        for (String collectionName : fileServices.getCollectionNames()) {
            collections.put(collectionName, new Collection(name, collectionName));
        }
        currentCollection = null;
        lock = new ReentrantReadWriteLock();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> showCollections() {
        return collections.keySet();
    }

    @Override
    public Collection currentCollection() {
        if (currentCollection == null) throw new NoSuchElementException("No valid collection is selected");
        return currentCollection;
    }

    @Override
    public void useCollection(String collectionName) throws IOException {
        lock.writeLock().lock();
        try {
            if (collectionName == null) currentCollection = null;
            else {
                if (!collections.containsKey(collectionName)) throw new NoSuchElementException("Collection name is not valid");
                currentCollection = collections.get(collectionName);
                new FileServices(name, collectionName, true);
                currentCollection.loadDocuments();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addCollection(String collectionName) throws IOException {
        lock.writeLock().lock();
        try {
            if (collections.containsKey(collectionName)) throw new NoSuchElementException("Collection already exists");
            collections.put(collectionName, new Collection(name, collectionName));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteCurrentCollection() throws IOException {
        lock.writeLock().lock();
        try {
            FileServices fileServices = new FileServices(DatabaseSystem.getInstance().currentDatabase().getName(), currentCollection().getName(), false);
            fileServices.deleteCurrentCollection();
            collections.remove(currentCollection().getName());
            currentCollection = null;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
