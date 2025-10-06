package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception;

public class NoFramesExtractedException extends RuntimeException {
    public NoFramesExtractedException(String message) {
        super(message);
    }
}
