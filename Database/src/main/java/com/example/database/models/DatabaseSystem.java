package com.example.database.models;

import com.example.database.services.FileServices;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatabaseSystem {
    private final Map<String, Database> databases;
    private Database currentDatabase;
    private static DatabaseSystem instance;
    private final FileServices fileServices;
    private final ReentrantReadWriteLock lock;

    private DatabaseSystem() throws IOException {
        databases = new TreeMap<>();
        fileServices = new FileServices("", "", false);
        for (String databaseName : fileServices.getDatabaseNames()) {
            databases.put(databaseName, new Database(databaseName));
        }
        currentDatabase = null;
        lock = new ReentrantReadWriteLock();
    }

    public static DatabaseSystem getInstance() throws IOException {
        if (instance == null) {
            instance = new DatabaseSystem();
        }
        return instance;
    }

    public Set<String> showDatabases() {
        return databases.keySet();
    }

    public Database currentDatabase() {
        if (currentDatabase == null) throw new NoSuchElementException("No valid database is selected");
        return currentDatabase;
    }

    public void useDatabase(String name) throws IOException {
        lock.writeLock().lock();
        try {
            if (!databases.containsKey(name)) throw new NoSuchElementException("Database name is not valid");
            fileServices.resetIndexes();
            if (currentDatabase != null) currentDatabase.useCollection(null);
            currentDatabase = databases.get(name);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addDatabase(Database database) {
        lock.writeLock().lock();
        try {
            if (databases.containsKey(database.getName())) throw new NoSuchElementException("Database already exists");
            databases.put(database.getName(), database);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteCurrentDatabase() throws IOException {
        lock.writeLock().lock();
        try {
            FileServices fileServices = new FileServices(currentDatabase.getName(), "", false);
            fileServices.deleteCurrentDatabase();
            databases.remove(currentDatabase.getName());
            currentDatabase = null;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
