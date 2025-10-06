package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.NotificacaoVideo;
import br.com.on.fiap.hackathonprocessadorvideo.fixture.NotificacaoVideoFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@ExtendWith(MockitoExtension.class)
class VideoSendMessageServiceAdapterTest {

    @Mock
    private SnsClient snsClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VideoSendMessageServiceAdapter adapter;

    private final String topicArn = "arn:aws:sns:us-east-1:000000000000:processed-videos";

    @Test
    @DisplayName("Deve serializar NotificacaoVideo em JSON e publicar no SNS")
    void givenValidNotificacaoVideo_whenEnviar_thenPublishToSns() throws Exception {
        NotificacaoVideo notif = NotificacaoVideoFixture.sample();
        String expectedJson = "{\"idUsuario\":\"user-123\"}";

        when(objectMapper.writeValueAsString(notif)).thenReturn(expectedJson);

        ArgumentCaptor<PublishRequest> reqCaptor = ArgumentCaptor.forClass(PublishRequest.class);

        adapter.enviar(notif, topicArn);

        verify(objectMapper, times(1)).writeValueAsString(notif);
        verify(snsClient, times(1)).publish(reqCaptor.capture());
        verifyNoMoreInteractions(snsClient);

        PublishRequest captured = reqCaptor.getValue();
        assertThat(captured.topicArn()).isEqualTo(topicArn);
        assertThat(captured.message()).isEqualTo(expectedJson);
    }

    @Test
    @DisplayName("Deve logar erro quando falhar ao serializar ou publicar")
    void givenSerializationFails_whenEnviar_thenCatchAndLogError() throws Exception {
        NotificacaoVideo notif = NotificacaoVideoFixture.sample();
        when(objectMapper.writeValueAsString(notif)).thenThrow(new RuntimeException("boom"));

        adapter.enviar(notif, topicArn);

        verify(objectMapper, times(1)).writeValueAsString(notif);
        verifyNoInteractions(snsClient);
    }
}
