package com.company.exceptions;

public class BadConfigException extends RuntimeException {
    public BadConfigException(String message) { super(message); }
    public BadConfigException(String message, Throwable e) { super(message, e); }
}
