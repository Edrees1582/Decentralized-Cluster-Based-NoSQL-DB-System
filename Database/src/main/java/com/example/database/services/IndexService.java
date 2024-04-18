package com.example.database.services;

import com.example.database.models.BPlusTree;
import com.example.database.models.DatabaseSystem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class IndexService {
    private BPlusTree index;
    private Map<String, BPlusTree> propertyIndex;
    private final int M;
    private static IndexService instance;

    private IndexService() {
        M = 4;
        index = new BPlusTree(M);
        propertyIndex = new HashMap<>();
    }

    public static IndexService getInstance() {
        if (instance == null) {
            instance = new IndexService();
        }
        return instance;
    }

    public void loadIndex(String filePath) throws IOException {
        index = loadTreeFromFile(filePath);
    }

    public List<String> getIndexKeys() {
        return index.getKeys();
    }

    public void addIndex(String key, Object value, String filePath) {
        index.insert(key, value);
        saveTreeToFile(filePath, "index");
    }

    public void deleteIndex(String targetId, String filePath) {
        index.delete(targetId);
        saveTreeToFile(filePath, "index");
    }

    public Object searchIndex(String targetId) {
        return index.search(targetId);
    }

    public void loadPropertyIndex(String filePath) throws IOException {
        propertyIndex = loadTreeFromFile(filePath, M);
    }

    public void addPropertyIndex(String filePath, Object key, String value) throws IOException {
        if (isValidKey(getFileName(filePath)) && isValidValue(key)) {
            if (!propertyIndex.containsKey(getFileName(filePath))) {
                propertyIndex.put(getFileName(filePath), new BPlusTree(M));
                propertyIndex.get(getFileName(filePath)).insert(key.toString(), value);
            }
            else propertyIndex.get(getFileName(filePath)).insert(key.toString(), value);
            save(filePath, key.toString(), value);
        }
    }

    public void updatePropertyIndex(String filePath, String oldKey, String value, String documentId, String newKey) throws IOException {
        if (isValidKey(getFileName(filePath)) && isValidValue(oldKey) && isValidValue(newKey)) {
            if (!propertyIndex.containsKey(getFileName(filePath))) propertyIndex.put(getFileName(filePath), new BPlusTree(M));
            else {
                delete(filePath, oldKey, documentId);
                save(filePath, newKey, value);
                loadPropertyIndex(filePath);
            }
        }
    }

    public void deletePropertyIndexById(String filePath, String documentId) throws IOException {
        JSONObject properties = DatabaseSystem.getInstance().currentDatabase().currentCollection().getDocumentById(documentId).getProperties();
        properties.keySet().forEach(key -> {
            if (isValidKey(key)) {
                try {
                    delete(filePath + key + "_index.json", documentId);
                    propertyIndex.get(key).delete(properties.get(key).toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public JSONArray searchPropertyIndex(String targetKey, String targetValue) {
        if (propertyIndex.get(targetKey) == null) return null;
        return new JSONArray(propertyIndex.get(targetKey).search(targetValue, targetValue));
    }

    public void resetCurrentIndexes() {
        index = new BPlusTree(M);
        propertyIndex = new HashMap<>();
    }

    public BPlusTree loadTreeFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) saveTreeToFile(filePath, "index");

        String content = Files.readString(Paths.get(filePath));
        JSONObject jsonObject = new JSONObject(content);

        BPlusTree tree = index;
        for (String key : jsonObject.keySet()) tree.insert(key, jsonObject.get(key));

        return tree;
    }

    public Map<String, BPlusTree> loadTreeFromFile(String filePath, int m) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) saveTreeToFile(filePath, "property");

        String content = Files.readString(Paths.get(filePath));
        JSONObject jsonObject = new JSONObject(content);
        BPlusTree bplusTree = new BPlusTree(m);
        for (String key : jsonObject.keySet()) bplusTree.insert(key, jsonObject.get(key));

        Map<String, BPlusTree> tree = propertyIndex;
        tree.put(getFileName(filePath), bplusTree);

        return tree;
    }

    public void saveTreeToFile(String filePath, String indexType) {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            JSONObject jsonObject = new JSONObject();
            if (indexType.equals("index"))
                for (String key : index.getKeys()) jsonObject.put(key, index.search(key));
            else if (indexType.equals("property"))
                for (String key : propertyIndex.get(getFileName(filePath)).getKeys()) jsonObject.put(key, propertyIndex.get(getFileName(filePath)).search(key));
            fileWriter.write(jsonObject.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(String filePath, String key, String value) throws IOException {
        JSONObject jsonObject = load(filePath);
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            if (jsonObject.has(key)) {
                JSONArray jsonArray = (JSONArray) jsonObject.get(key);
                jsonArray.put(value);
                jsonObject.put(key, jsonArray);
            }
            else {
                List<String> list = new ArrayList<>();
                list.add(value);
                jsonObject.put(key, list);
            }
            fileWriter.write(jsonObject.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(String filePath, String key, String documentId) throws IOException {
        JSONObject jsonObject = load(filePath);
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            if (jsonObject.has(key)) {
                JSONArray jsonArray = (JSONArray) jsonObject.get(key);
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (Objects.equals(extractId(jsonArray.get(i).toString()), documentId)) {
                        jsonArray.remove(i);
                        if (jsonArray.isEmpty()) jsonObject.remove(key);
                        fileWriter.write(jsonObject.toString());
                        return;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(String filePath, String documentId) throws IOException {
        JSONObject jsonObject = load(filePath);
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            for (String key : jsonObject.keySet()) {
                JSONArray jsonArray = (JSONArray) jsonObject.get(key);
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (Objects.equals(extractId(jsonArray.get(i).toString()), documentId)) {
                        jsonArray.remove(i);
                        if (jsonArray.isEmpty()) jsonObject.remove(key);
                        fileWriter.write(jsonObject.toString());
                        return;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JSONObject load(String filePath) throws IOException {
        File file = new File(filePath);
        file.createNewFile();

        String content = Files.readString(Paths.get(filePath));

        if (content.isEmpty()) content += "{}";

        return new JSONObject(content);
    }

    public boolean isValidValue(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean;
    }

    public boolean isValidKey(String key) {
       return !(key.equals("_id") || key.equals("affinityNode") || key.equals("created-at") || key.equals("last-modified") || key.equals("version") || key.equals("assignedNode") || key.equals("password"));
    }

    public String getFileName(String filePath) {
        File file = new File(filePath);
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String fileName2 = fileName;
        if (dotIndex != -1 && dotIndex != 0) {
            fileName2 = fileName.substring(0, dotIndex);
        }
        int lastIndex = fileName2.lastIndexOf("_index");
        if (lastIndex != -1) {
            return fileName2.substring(0, lastIndex);
        }
        return fileName2.substring(0, dotIndex);
    }

    public static String extractId(String filePath) {
        int lastIndex = filePath.lastIndexOf('/');
        if (lastIndex != -1 && lastIndex < filePath.length() - 1) {
            String fileName = filePath.substring(lastIndex + 1);
            return fileName.substring(0, fileName.lastIndexOf('.'));
        }
        return null;
    }
}
