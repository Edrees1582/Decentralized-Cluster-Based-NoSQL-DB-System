package com.example.bootstrap.services;

import java.io.IOException;
import java.util.List;

public class LoadBalancerService {
    private static LoadBalancerService instance;
    private final List<String> nodes;
    private int currentIndex;

    private LoadBalancerService(String[] nodes) {
        this.nodes = List.of(nodes);
        currentIndex = 0;
    }

    public static LoadBalancerService getInstance(String[] nodes) throws IOException {
        if (instance == null) {
            instance = new LoadBalancerService(nodes);
        }
        return instance;
    }

    public synchronized String getNextNode() {
        String nextNode = nodes.get(currentIndex);
        currentIndex = (currentIndex + 1) % nodes.size();
        return nextNode;
    }
}
