package com.company.exceptions;

public class NotUniqueBeanNameException extends RuntimeException {
    public NotUniqueBeanNameException(String message) {
        super(message);
    }
}
