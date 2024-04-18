package com.example.database.controllers;

import com.example.database.services.NodesCommunicationServices;
import com.example.database.services.PasswordEncryption;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("user")
public class UsersController {
    @Autowired
    Environment environment;

    @PostMapping("/auth")
    public ResponseEntity<String> authenticateUser(@RequestBody String user) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        JSONObject jsonObject = new JSONObject(user);

        String getUser = ((List<String>) Objects.requireNonNull(restTemplate.exchange("http://" +
                        NodesCommunicationServices.getIP() +
                        ":" +
                        environment.getProperty("local.server.port") +
                        "/document/property?propertyKey=username" + "&propertyValue=" + jsonObject.getString("username"),
                HttpMethod.GET, new HttpEntity<>("", httpHeaders), List.class).getBody())).get(0);

        JSONObject userJSON = new JSONObject(getUser);

        if (userJSON.getString("password").equals(PasswordEncryption.EncryptPassword(jsonObject.getString("password"))))
            return new ResponseEntity<>("Authenticated", HttpStatus.OK);
        else return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }
}
