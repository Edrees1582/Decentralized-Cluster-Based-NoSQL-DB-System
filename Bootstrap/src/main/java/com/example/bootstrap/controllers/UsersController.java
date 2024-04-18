package com.example.bootstrap.controllers;

import com.example.bootstrap.services.LoadBalancerService;
import com.example.bootstrap.services.PasswordEncryption;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("user")
public class UsersController {
    @Autowired
    Environment environment;

    @PostMapping("")
    public ResponseEntity<String> addUser(@RequestBody String user) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        JSONObject jsonObject = requestBodyToJSON(user);
        jsonObject.put("password", PasswordEncryption.EncryptPassword(jsonObject.getString("password")));
        jsonObject.put("affinityNode", LoadBalancerService.getInstance(Objects.requireNonNull(environment.getProperty("nodes", String[].class))).getNextNode());
        restTemplate.exchange("http://172.18.0.2:8080/document",
                HttpMethod.POST, new HttpEntity<>(jsonObject.toString(), httpHeaders), String.class);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/auth")
    public ResponseEntity<String> authenticateUser(@RequestBody String user) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        JSONObject jsonObject = requestBodyToJSON(user);

        String getUser = ((List<String>) Objects.requireNonNull(restTemplate.exchange("http://172.18.0.2:8080/document/property?propertyKey=username" + "&propertyValue=" + jsonObject.getString("username"),
                HttpMethod.GET, new HttpEntity<>("", httpHeaders), List.class).getBody())).get(0);

        JSONObject userJSON = new JSONObject(getUser);

        System.out.println("getUser: " + getUser);
        System.out.println("userJSON: " + userJSON);

        if (userJSON.getString("password").equals(PasswordEncryption.EncryptPassword(jsonObject.getString("password"))))
            return new ResponseEntity<>(getUser, HttpStatus.OK);
        else return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    private JSONObject requestBodyToJSON(String requestBody) {
        Map<String, String> keyValueMap = UriComponentsBuilder.fromUriString("?" + requestBody)
                .build()
                .getQueryParams()
                .toSingleValueMap();
        return new JSONObject(keyValueMap);
    }
}
