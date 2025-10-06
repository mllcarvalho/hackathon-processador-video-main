package br.com.on.fiap.hackathonprocessadorvideo.domain.service;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.VideoMensagem;
import java.io.IOException;

public interface VideoProcessingPort {
    String processarVideo(VideoMensagem mensagem) throws IOException;
}
