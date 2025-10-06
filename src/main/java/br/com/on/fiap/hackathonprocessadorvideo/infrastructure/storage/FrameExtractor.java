package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Path;

public interface FrameExtractor {
    Path extract(Path videoFile) throws IOException;
}
