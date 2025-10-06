package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.messaging;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.NotificacaoVideo;
import br.com.on.fiap.hackathonprocessadorvideo.domain.model.VideoMensagem;
import br.com.on.fiap.hackathonprocessadorvideo.domain.service.VideoProcessingPort;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqsListenerAdapter {

    @Value("${sns.topic-arn}")
    private String topicArn;

    private final VideoProcessingPort videoProcessingPort;
    private final VideoSendMessageServiceAdapter videoSendMessageServiceAdapter;

    public SqsListenerAdapter(
            VideoProcessingPort videoProcessingPort, VideoSendMessageServiceAdapter videoSendMessageServiceAdapter) {
        this.videoProcessingPort = videoProcessingPort;
        this.videoSendMessageServiceAdapter = videoSendMessageServiceAdapter;
    }

    @SqsListener(value = "${queue.path}", maxMessagesPerPoll = "1", acknowledgementMode = "MANUAL")
    public void listen(
            @Payload(required = false) VideoMensagem mensagem, @Header("id") String messageId, Acknowledgement ack) {

        log.info("MENSAGEM RECEBIDA [{}] - {}", messageId, mensagem);

        if (mensagem == null
                || isBlank(mensagem.getCaminhoVideo())
                || isBlank(mensagem.getNomeVideo())
                || isBlank(mensagem.getIdUsuario())) {
            log.warn("Discarding invalid message [{}]: {}", messageId, mensagem);
            ack.acknowledge();
            return;
        }

        try {
            String caminhoSaida = videoProcessingPort.processarVideo(mensagem);

            NotificacaoVideo respSaida = new NotificacaoVideo(
                    mensagem.getIdUsuario(),
                    mensagem.getNomeVideo(),
                    caminhoSaida,
                    "ARQUIVO_PROCESSADO",
                    "Video processado com successo.",
                    mensagem.getEmail());

            videoSendMessageServiceAdapter.enviar(respSaida, topicArn);

            ack.acknowledge();
            log.info("Mensagem [{}] processada e confirmada (ACK).", messageId);

        } catch (Exception e) {
            if (isTransient(e)) {
                log.error(
                        "Erro TRANSITÓRIO na mensagem [{}]. Sem ACK; será reentregue. Causa={}",
                        messageId,
                        rootMessage(e),
                        e);
                return;
            }

            log.error(
                    "Erro PERMANENTE na mensagem [{}]. Dando ACK para descartar. Causa={}",
                    messageId,
                    rootMessage(e),
                    e);
            ack.acknowledge();
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Heurística simples para classificar erro como transitório:
     * - IOException, Timeout
     * - Exceções do SDK AWS marcadas como "retryable"/"SdkClientException"
     * - Mensagens que contenham "timeout" ou "throttl"
     */
    private boolean isTransient(Throwable t) {
        Throwable c = t;
        while (c != null) {
            if (c instanceof IOException || c instanceof TimeoutException) {
                return true;
            }
            String cn = c.getClass().getName();
            if (cn.contains("SdkClientException") || cn.contains("Retryable")) {
                return true;
            }
            String msg = c.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("timeout") || m.contains("throttl") || m.contains("temporarily")) {
                    return true;
                }
            }
            c = c.getCause();
        }
        return false;
    }

    private String rootMessage(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null) r = r.getCause();
        return r.toString();
    }
}
