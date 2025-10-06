package br.com.on.fiap.hackathonprocessadorvideo.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoMensagem {

    private String idUsuario;
    private String nomeVideo;
    private String caminhoVideo;
    private LocalDateTime dataCriacao;
    private String email;
}
