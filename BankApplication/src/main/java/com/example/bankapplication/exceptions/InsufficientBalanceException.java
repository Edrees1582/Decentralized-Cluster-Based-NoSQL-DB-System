package com.example.bankapplication.exceptions;

public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException() {
        super("Insufficient balance to perform the withdrawal.");
    }
}
