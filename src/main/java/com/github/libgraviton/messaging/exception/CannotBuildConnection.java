package com.github.libgraviton.messaging.exception;

public class CannotBuildConnection extends Exception {

    public CannotBuildConnection(Class connectionType, String reason) {
        super(String.format("Cannot build connection of type '%s'. Reason: '%s'", connectionType, reason));
    }

    public CannotBuildConnection(Class connectionType, Throwable cause) {
        this(connectionType, "An exception occurred.");
        initCause(cause);
    }

}
