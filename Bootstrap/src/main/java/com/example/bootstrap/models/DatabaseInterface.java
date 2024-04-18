package com.example.bootstrap.models;

import java.io.IOException;
import java.util.Set;

public interface DatabaseInterface {
    String getName();
    Set<String> showCollections();
    Collection currentCollection();
    void useCollection(String collectionName) throws IOException;
    void addCollection(String collectionName) throws IOException;
    void deleteCurrentCollection() throws IOException;
}
