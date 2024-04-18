package com.example.bootstrap.services;

import com.example.bootstrap.models.Document;
import com.example.bootstrap.models.Property;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class FileServices {
    private final String STORAGE_PATH;
    private final IndexService indexService;
    private final String currentDatabaseName;
    private final String currentCollectionName;

    public FileServices(String currentDatabaseName, String currentCollectionName, boolean load) throws IOException {
        STORAGE_PATH = "./storage";
        indexService = IndexService.getInstance();

        Path storageDirectory = Paths.get(STORAGE_PATH);
        if (!Files.exists(storageDirectory)) Files.createDirectories(storageDirectory);

        this.currentDatabaseName = currentDatabaseName;
        this.currentCollectionName = currentCollectionName;

        if (!currentDatabaseName.isEmpty()) {
            Path storageDatabaseDirectory = Paths.get(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName);
            if (!Files.exists(storageDatabaseDirectory)) Files.createDirectories(storageDatabaseDirectory);

            if (load) {
                resetIndexes();
                loadIndex();
                loadPropertyIndex();
            }
        }
    }

    public void addDocumentFile(String id, JSONObject jsonObject) {
        try (FileWriter fileWriter = new FileWriter(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/" + id + ".json")) {
            jsonObject.put("_id", id);
            jsonObject.put("created-at", LocalDateTime.now());
            jsonObject.put("last-modified", "");
            jsonObject.put("version", 0);
            fileWriter.write(jsonObject.toString());

            for (String key : jsonObject.keySet())
                indexService.addPropertyIndex(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/" + key + "_index.json", jsonObject.get(key), STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/" + id + ".json");
            indexService.addIndex(id, STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/" + id + ".json", STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/" + "index.json");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Document getDocumentById(String targetId) {
        try {
            File file = searchFilesByID(targetId);
            if (file == null) return null;

            JSONObject properties = new JSONObject(new String(Files.readAllBytes(file.toPath())));
            return new Document(properties, currentDatabaseName, currentCollectionName, targetId);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Document> getAllDocuments() {
        try {
            List<Document> documents = new ArrayList<>();
            for (String document : indexService.getIndexKeys())
                documents.add(getDocumentById(document));
            return documents;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateDocumentById(String targetId, Property object, long newVersion) {
        try {
            File file = searchFilesByID(targetId);
            if (file == null) return;

            JSONObject jsonObject = new JSONObject(Files.readString(Paths.get(file.getPath())));

            indexService.updatePropertyIndex(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/" + object.key() + "_index.json", jsonObject.get(object.key()).toString(), STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/" + targetId + ".json", targetId, object.value().toString());

            jsonObject.put(object.key(), object.value());

            jsonObject.put("last-modified", LocalDateTime.now());
            jsonObject.put("version", newVersion);

            Files.writeString(Paths.get(file.getPath()), jsonObject.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public void deleteDocument(String targetId) {
        try {
            File file = searchFilesByID(targetId);
            if (file == null) throw new NoSuchElementException("Document \"" + targetId + "\" doesn't exist");

            indexService.deletePropertyIndexById(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/", targetId);
            indexService.deleteIndex(targetId, STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/" + "index.json");
            file.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteCurrentDatabase() throws IOException {
        Files.walkFileTree(Paths.get(STORAGE_PATH + "/" + currentDatabaseName), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void deleteCurrentCollection() throws IOException {
        Files.walkFileTree(Paths.get(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public File searchFilesByID(String targetId) {
        if (indexService.searchIndex(targetId) == null) {
            System.out.println("No files found with ID: " + targetId);
            return null;
        }
        else return new File(String.valueOf(indexService.searchIndex(targetId)));
    }

    public List<String> getDatabaseNames() {
        try {
            return Files.list(Paths.get(STORAGE_PATH))
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<String> getCollectionNames() {
        try {
            return Files.list(Paths.get(STORAGE_PATH + "/" + currentDatabaseName))
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void loadIndex() throws IOException {
        indexService.loadIndex(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/index.json");
    }

    public void loadPropertyIndex() {
        try {
            Files.walkFileTree(Paths.get(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().endsWith("_index.json")) {
                        indexService.loadPropertyIndex(String.valueOf(file).replace("\\", "/"));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetIndexes() {
        indexService.resetCurrentIndexes();
    }

    public void addJsonSchema(String jsonSchema) {
        try (FileWriter fileWriter = new FileWriter(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/schema.json")) {
            fileWriter.write(jsonSchema);
            Path dbSchemaPath = Paths.get(STORAGE_PATH + "/" + currentDatabaseName + "/schemas.json");
            if (!Files.exists(dbSchemaPath)) Files.createFile(dbSchemaPath);

            String json = Files.readString(dbSchemaPath);

            if (json.isEmpty()) json = "[]";

            JSONArray dbSchema = new JSONArray(json);

            for (int i = 0; i < dbSchema.length(); i++) {
                try {
                    JSONObject jsonObject = dbSchema.getJSONObject(i);
                    if (jsonObject.has(currentCollectionName)) {
                        dbSchema.remove(i);
                        break;
                    }
                } catch (JSONException e) {
                    System.err.println("Error processing JSON object at index " + i + ": " + e.getMessage());
                }
            }

            dbSchema.put(new JSONObject().put(currentCollectionName, jsonSchema));

            try (FileWriter fileWriter2 = new FileWriter(STORAGE_PATH + "/" + currentDatabaseName + "/schemas.json")) {
                fileWriter2.write(dbSchema.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
       } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String loadJsonSchema() throws IOException {
        File jsonSchema = new File(STORAGE_PATH + "/" + currentDatabaseName + "/" + currentCollectionName + "/schema.json");
        if (jsonSchema.exists() && SchemaValidator.isValidJsonSchema(Files.readString(jsonSchema.toPath()))) return Files.readString(jsonSchema.toPath());
        return null;
    }
}
