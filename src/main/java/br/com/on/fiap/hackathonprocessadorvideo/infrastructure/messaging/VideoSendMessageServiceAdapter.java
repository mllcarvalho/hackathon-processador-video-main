package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.messaging;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.NotificacaoVideo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Component
@Slf4j
public class VideoSendMessageServiceAdapter {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    public VideoSendMessageServiceAdapter(SnsClient snsClient, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
    }

    public void enviar(NotificacaoVideo mensagem, String topicArn) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(mensagem);
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(jsonMessage)
                    .build();
            snsClient.publish(request);
            log.info("Mensagem enviada para SNS Topic '{}': {}", topicArn, jsonMessage);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para SNS Topic: {}", e.getMessage(), e);
        }
    }
}
