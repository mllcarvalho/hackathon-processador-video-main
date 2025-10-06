package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.NotificacaoVideo;
import br.com.on.fiap.hackathonprocessadorvideo.domain.model.VideoMensagem;
import br.com.on.fiap.hackathonprocessadorvideo.domain.service.VideoProcessingPort;
import br.com.on.fiap.hackathonprocessadorvideo.fixture.VideoMensagemFixture;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SqsListenerAdapterTest {

    @Mock
    private VideoProcessingPort videoProcessingPort;

    @Mock
    private VideoSendMessageServiceAdapter videoSendMessageServiceAdapter;

    @Mock
    private Acknowledgement ack;

    @InjectMocks
    private SqsListenerAdapter listener;

    private final String topicArn = "arn:aws:sns:us-east-1:000000000000:processed-videos";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "topicArn", topicArn);
    }

    @Test
    @DisplayName("Sucesso: processa vídeo, publica SNS e dá ACK")
    void givenValidMessage_whenListen_thenProcessPublishAndAck() throws IOException {
        VideoMensagem entrada = VideoMensagemFixture.builder()
                .idUsuario("22")
                .nomeVideo("video_ok.mp4")
                .email("user@example.com")
                .caminhoVideo("entrada/22/video_ok.mp4")
                .build();

        String caminhoSaidaGerado = "saida/22/video_ok.zip";
        when(videoProcessingPort.processarVideo(any(VideoMensagem.class))).thenReturn(caminhoSaidaGerado);

        ArgumentCaptor<NotificacaoVideo> notifCaptor = ArgumentCaptor.forClass(NotificacaoVideo.class);

        listener.listen(entrada, "msg-1", ack);

        verify(videoProcessingPort).processarVideo(entrada);
        verify(videoSendMessageServiceAdapter).enviar(notifCaptor.capture(), eq(topicArn));
        verify(ack).acknowledge();
        verifyNoMoreInteractions(videoProcessingPort, videoSendMessageServiceAdapter, ack);

        NotificacaoVideo notif = notifCaptor.getValue();
        assertThat(notif.idUsuario()).isEqualTo("22");
        assertThat(notif.nomeVideo()).isEqualTo("video_ok.mp4");
        assertThat(notif.caminhoSaida()).isEqualTo(caminhoSaidaGerado);
        assertThat(notif.situacao()).isEqualTo("ARQUIVO_PROCESSADO");
        assertThat(notif.descricao()).isEqualTo("Video processado com successo.");
        assertThat(notif.email()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Erro transitório (IOException): não publica e NÃO dá ACK (reentrega)")
    void givenTransientError_whenListen_thenNoAckAndNoPublish() throws IOException {
        VideoMensagem entrada = VideoMensagemFixture.builder()
                .idUsuario("22")
                .nomeVideo("video_falha.mp4")
                .email("user@example.com")
                .caminhoVideo("entrada/22/video_falha.mp4")
                .build();

        when(videoProcessingPort.processarVideo(any(VideoMensagem.class))).thenThrow(new IOException("S3 timeout"));

        assertDoesNotThrow(() -> listener.listen(entrada, "msg-2", ack));

        verify(videoProcessingPort).processarVideo(entrada);
        verifyNoInteractions(videoSendMessageServiceAdapter);
        verify(ack, never()).acknowledge();
        verifyNoMoreInteractions(videoProcessingPort, ack);
    }

    @Test
    @DisplayName("Erro permanente (IllegalArgumentException): não publica e dá ACK (descarta)")
    void givenPermanentError_whenListen_thenAckAndNoPublish() throws IOException {
        VideoMensagem entrada = VideoMensagemFixture.builder()
                .idUsuario("22")
                .nomeVideo("video_invalido.mp4")
                .email("user@example.com")
                .caminhoVideo("entrada/22/video_invalido.mp4")
                .build();

        when(videoProcessingPort.processarVideo(any(VideoMensagem.class)))
                .thenThrow(new IllegalArgumentException("formato inválido"));

        assertDoesNotThrow(() -> listener.listen(entrada, "msg-3", ack));

        verify(videoProcessingPort).processarVideo(entrada);
        verifyNoInteractions(videoSendMessageServiceAdapter);
        verify(ack).acknowledge();
        verifyNoMoreInteractions(videoProcessingPort, ack);
    }

    @Test
    @DisplayName("Payload inválido: ACK imediato e não processa")
    void givenInvalidPayload_whenListen_thenAckAndSkip() {
        assertDoesNotThrow(() -> listener.listen(null, "msg-4", ack));
        verify(ack).acknowledge();
        verifyNoInteractions(videoProcessingPort, videoSendMessageServiceAdapter);
        clearInvocations(ack);

        VideoMensagem semId = VideoMensagemFixture.builder()
                .idUsuario("  ")
                .nomeVideo("x.mp4")
                .email("u@e.com")
                .caminhoVideo("entrada/x.mp4")
                .build();
        assertDoesNotThrow(() -> listener.listen(semId, "msg-5", ack));
        verify(ack).acknowledge();
        verifyNoInteractions(videoProcessingPort, videoSendMessageServiceAdapter);
        clearInvocations(ack);

        VideoMensagem semNome = VideoMensagemFixture.builder()
                .idUsuario("22")
                .nomeVideo(" ")
                .email("u@e.com")
                .caminhoVideo("entrada/x.mp4")
                .build();
        assertDoesNotThrow(() -> listener.listen(semNome, "msg-6", ack));
        verify(ack).acknowledge();
        verifyNoInteractions(videoProcessingPort, videoSendMessageServiceAdapter);
        clearInvocations(ack);

        VideoMensagem semCaminho = VideoMensagemFixture.builder()
                .idUsuario("22")
                .nomeVideo("x.mp4")
                .email("u@e.com")
                .caminhoVideo(" ")
                .build();
        assertDoesNotThrow(() -> listener.listen(semCaminho, "msg-7", ack));
        verify(ack).acknowledge();
        verifyNoInteractions(videoProcessingPort, videoSendMessageServiceAdapter);
    }
}
