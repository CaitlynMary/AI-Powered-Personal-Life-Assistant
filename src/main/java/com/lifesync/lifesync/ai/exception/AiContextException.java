package com.lifesync.lifesync.ai.exception;

public class AiContextException extends RuntimeException {
    public AiContextException() {
        super("LifeSync could not retrieve the records needed for this answer. Please retry.");
    }
}
