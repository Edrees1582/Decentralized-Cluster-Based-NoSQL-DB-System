package com.example.bootstrap.services;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class NodesCommunicationServices {
    public static void broadcastChanges(String url, HttpMethod httpMethod, String[] nodes, String originNode, String requestBody) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        if (url.contains("?")) url += ("&isBroadcast=" + true);
        else url += ("?isBroadcast=" + true);
        for (String node : nodes)
            if (!node.equals(originNode))
                restTemplate.exchange(url.replace("***", node),
                        httpMethod, new HttpEntity<>(requestBody, httpHeaders), String.class);
    }

    public static ResponseEntity<String> redirectRequest(String url, HttpMethod httpMethod) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        if (url.contains("?")) url += ("&isRedirected=" + true);
        else url += ("?isRedirected=" + true);
        return restTemplate.exchange(url, httpMethod, new HttpEntity<>("", httpHeaders), String.class);
    }

    public static ResponseEntity<Integer> getOwnedDocuments(String node) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.add("Accept", "application/json");
        ResponseEntity<Integer> response = restTemplate.exchange("http://localhost:" + node + "/document/owned",
                HttpMethod.GET, new HttpEntity<>("", httpHeaders), Integer.class);
        return new ResponseEntity<>(response.getBody(), response.getStatusCode());
    }

    public static String getIP() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
        return request.getLocalAddr();
    }
}
