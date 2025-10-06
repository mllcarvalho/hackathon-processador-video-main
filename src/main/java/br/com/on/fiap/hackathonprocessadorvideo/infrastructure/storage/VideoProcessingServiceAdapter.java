// VideoProcessingServiceAdapter.java
package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.VideoMensagem;
import br.com.on.fiap.hackathonprocessadorvideo.domain.service.VideoProcessingPort;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.util.StorageFileUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
public class VideoProcessingServiceAdapter implements VideoProcessingPort {

    @Value("${nomeBucket}")
    private String bucket;

    private final S3Client s3Client;
    private final FrameExtractor extractor;

    public VideoProcessingServiceAdapter(S3Client s3Client, @Qualifier("mp4Extractor") FrameExtractor extractor) {
        this.s3Client = s3Client;
        this.extractor = extractor;
    }

    @Override
    public String processarVideo(VideoMensagem mensagem) throws IOException {
        String key = mensagem.getCaminhoVideo();
        String keyOutput = key.replace("entrada", "saida").replace(".mp4", ".zip");

        Path tempVideo = null;
        Path framesDir = null;
        Path zipFile = null;

        try {
            tempVideo = downloadVideo(bucket, key);
            framesDir = extractor.extract(tempVideo);
            zipFile = StorageFileUtil.zipDirectory(framesDir);
            uploadZip(bucket, keyOutput, zipFile);
            return keyOutput;
        } finally {
            cleanup(tempVideo, framesDir, zipFile);
        }
    }

    private Path downloadVideo(String bucket, String key) throws IOException {
        Path tempVideo = Files.createTempFile("video-", ".mp4");
        try (InputStream in = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build())) {
            Files.copy(in, tempVideo, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Vídeo baixado para temporário: {}", tempVideo);
        return tempVideo;
    }

    void uploadZip(String bucket, String keyOutput, Path zipFile) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(keyOutput).build(), RequestBody.fromFile(zipFile));
        log.info("Zip enviado para S3: {}/{}", bucket, keyOutput);
    }

    void cleanup(Path videoFile, Path framesDir, Path zipFile) {
        try {
            StorageFileUtil.deleteRecursively(framesDir);
            StorageFileUtil.deleteQuietly(videoFile);
            StorageFileUtil.deleteQuietly(zipFile);
            log.info("Arquivos temporários limpos");
        } catch (Exception e) {
            log.warn(
                    "Erro ao limpar arquivos temporários (video={}, framesDir={}, zip={})",
                    videoFile,
                    framesDir,
                    zipFile,
                    e);
        }
    }
}
