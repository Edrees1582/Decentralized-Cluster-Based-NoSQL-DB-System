package com.example.bootstrap.services;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

public class SchemaValidator {
    public static boolean isValidJsonSchema(String jsonSchema) {
        try {
            SchemaLoader.load(new JSONObject(jsonSchema));
            return true;
        } catch (org.everit.json.schema.ValidationException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean isValidJson(String jsonSchema, String json) {
        JSONObject schemaJson = new JSONObject(jsonSchema);
        Schema schema = SchemaLoader.load(schemaJson);

        try {
            schema.validate(new JSONObject(json));
            return true;
        } catch (org.everit.json.schema.ValidationException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
