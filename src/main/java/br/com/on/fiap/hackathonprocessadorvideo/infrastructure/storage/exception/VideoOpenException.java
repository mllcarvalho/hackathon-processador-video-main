package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception;

public class VideoOpenException extends FrameExtractionException {
    public VideoOpenException(String message) {
        super(message);
    }

    public VideoOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}
