package com.example.bankapplication.models;

import com.example.bankapplication.exceptions.InsufficientBalanceException;

public class User {
    private final String _id;
    private final String username;
    private final String affinityNode;
    private double balance;

    public User(String id, String username, String affinityNode, double balance) {
        _id = id;
        this.username = username;
        this.affinityNode = affinityNode;
        this.balance = balance;
    }

    public String getId() {
        return _id;
    }

    public String getUsername() {
        return username;
    }

    public String getAffinityNode() {
        return affinityNode;
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        balance += amount;
    }

    public void withdraw(double amount) throws InsufficientBalanceException {
        if (amount <= balance) balance -= amount;
        else throw new InsufficientBalanceException();
    }

    public static User createUser(String id, String username, String affinityNode, double balance) {
        return new User(id, username, affinityNode, balance);
    }

    @Override
    public String toString() {
        return "User{" +
                "_id='" + _id + '\'' +
                ", username='" + username + '\'' +
                ", affinityNode='" + affinityNode + '\'' +
                ", balance=" + balance +
                '}';
    }
}
