package br.com.on.fiap.hackathonprocessadorvideo.fixture;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.NotificacaoVideo;

public final class NotificacaoVideoFixture {

    private NotificacaoVideoFixture() {}

    public static NotificacaoVideo sample() {
        return new NotificacaoVideo(
                "user-123",
                "video_teste.mp4",
                "/tmp/out/video_teste.mp4",
                "ARQUIVO_PROCESSADO",
                "Video processado com successo.",
                "destino@example.com");
    }
}
