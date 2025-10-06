package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception;

import java.io.IOException;

public class FrameExtractionException extends IOException {
    public FrameExtractionException(String message) {
        super(message);
    }

    public FrameExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
