package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception;

import lombok.Getter;

@Getter
public class FrameReadException extends FrameExtractionException {
    private final long frameIndex;

    public FrameReadException(String message, Throwable cause) {
        super(message, cause);
        this.frameIndex = -1;
    }

    public FrameReadException(long frameIndex, Throwable cause) {
        super("Error reading video frame at index " + frameIndex, cause);
        this.frameIndex = frameIndex;
    }
}
