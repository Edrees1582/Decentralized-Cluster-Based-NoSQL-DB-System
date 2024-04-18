package com.example.bankapplication.services;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class Communication {
    public static ResponseEntity<String> callAPI(String url, HttpMethod httpMethod, String body) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        return restTemplate.exchange(url, httpMethod, new HttpEntity<>(body, httpHeaders), String.class);
    }

    public static ResponseEntity<String> callAPI(String url, HttpMethod httpMethod, String[] keys, Object[] values) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?").append(keys[0]).append("=").append(values[0].toString());
        for (int i = 1; i < keys.length; i++)
            urlBuilder.append("&").append(keys[i]).append("=").append(values[i].toString());
        url = urlBuilder.toString();
        return restTemplate.exchange(url, httpMethod, new HttpEntity<>("", httpHeaders), String.class);
    }
}
