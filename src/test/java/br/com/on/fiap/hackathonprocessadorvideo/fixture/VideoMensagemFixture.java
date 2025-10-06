package br.com.on.fiap.hackathonprocessadorvideo.fixture;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.VideoMensagem;
import java.time.LocalDateTime;

public final class VideoMensagemFixture {

    private VideoMensagemFixture() {}

    public static Builder builder() {
        return new Builder()
                .idUsuario("user-123")
                .nomeVideo("video_teste.mp4")
                .caminhoVideo("/tmp/in/video_teste.mp4")
                .dataCriacao(LocalDateTime.now())
                .email("destino@example.com");
    }

    public static VideoMensagem sample() {
        return builder().build();
    }

    public static final class Builder {
        private String idUsuario;
        private String nomeVideo;
        private String caminhoVideo;
        private LocalDateTime dataCriacao;
        private String email;

        public Builder idUsuario(String v) {
            this.idUsuario = v;
            return this;
        }

        public Builder nomeVideo(String v) {
            this.nomeVideo = v;
            return this;
        }

        public Builder caminhoVideo(String v) {
            this.caminhoVideo = v;
            return this;
        }

        public Builder dataCriacao(LocalDateTime v) {
            this.dataCriacao = v;
            return this;
        }

        public Builder email(String v) {
            this.email = v;
            return this;
        }

        public VideoMensagem build() {
            return new VideoMensagem(idUsuario, nomeVideo, caminhoVideo, dataCriacao, email);
        }
    }
}
