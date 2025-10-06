package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.VideoMensagem;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.util.StorageFileUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.zip.ZipInputStream;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoProcessingServiceAdapter - fluxo completo com resources + limpeza via StorageFileUtil")
class VideoProcessingServiceAdapterTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private VideoProcessingServiceAdapter service;

    @BeforeEach
    void setup() {
        service = new VideoProcessingServiceAdapter(s3Client, new FfmpegFrameExtractor());
    }

    private byte[] loadResourceVideo() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("video_teste.mp4")) {
            if (in == null) throw new IOException("Resource 'video_teste.mp4' não encontrado no classpath.");
            return in.readAllBytes();
        }
    }

    private boolean ffmpegAvailable(byte[] bytes) {
        Path tmp = null;
        try {
            Loader.load(avutil.class);
            tmp = Files.createTempFile("smoke-", ".mp4");
            Files.write(tmp, bytes);
            try (FFmpegFrameGrabber g = new FFmpegFrameGrabber(tmp.toString())) {
                g.start();
                g.stop();
            }
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            StorageFileUtil.deleteQuietly(tmp);
        }
    }

    @Test
    @DisplayName(
            "Happy path: baixa do S3, extrai frames (FFmpeg), zipa, envia — valida conteúdo do ZIP e limpa com StorageFileUtil")
    void givenResourceVideo_whenProcessarVideo_thenOkAndZipHasPngs() throws Exception {
        byte[] video = loadResourceVideo();
        Assumptions.assumeTrue(ffmpegAvailable(video), "Pulando: FFmpeg não disponível");

        ResponseInputStream<GetObjectResponse> ris = new ResponseInputStream<>(
                GetObjectResponse.builder().contentType("video/mp4").build(),
                AbortableInputStream.create(new ByteArrayInputStream(video)));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(ris);

        ReflectionTestUtils.setField(service, "bucket", "my-bucket");
        var msg = new VideoMensagem(
                "user-123", "video_teste.mp4", "/entrada/video_teste.mp4", LocalDateTime.now(), "user@example.com");

        final byte[][] uploadedZipRef = new byte[1][];
        doAnswer(inv -> {
                    RequestBody body = inv.getArgument(1);
                    try (InputStream in = body.contentStreamProvider().newStream()) {
                        uploadedZipRef[0] = in.readAllBytes();
                    }
                    return null;
                })
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        String outKey = service.processarVideo(msg);
        assertThat(outKey).isEqualTo("/saida/video_teste.zip");

        ArgumentCaptor<PutObjectRequest> reqCap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(reqCap.capture(), any(RequestBody.class));
        assertThat(reqCap.getValue().bucket()).isEqualTo("my-bucket");
        assertThat(reqCap.getValue().key()).isEqualTo("/saida/video_teste.zip");

        assertThat(uploadedZipRef[0]).isNotNull();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(uploadedZipRef[0]))) {
            int entries = 0, pngs = 0;
            for (var e = zis.getNextEntry(); e != null; e = zis.getNextEntry()) {
                entries++;
                if (e.getName().endsWith(".png")) pngs++;
            }
            assertThat(entries).isGreaterThan(0);
            assertThat(pngs).isEqualTo(entries);
        }
    }

    @Test
    @DisplayName("Falha de S3 (Runtime) durante download deve ser propagada")
    void givenS3Failure_whenProcessarVideo_thenPropagatesRuntime() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("S3 down"));

        ReflectionTestUtils.setField(service, "bucket", "my-bucket");
        var msg = new VideoMensagem(
                "user-123", "video_teste.mp4", "/entrada/video_teste.mp4", LocalDateTime.now(), "user@example.com");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.processarVideo(msg));
        assertThat(ex).hasMessage("S3 down");
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("IOException durante o copy do download deve ser propagada (método declara throws IOException)")
    void givenDownloadIOException_whenProcessarVideo_thenThrowsIOException() {
        InputStream failingIn = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("io-boom");
            }
        };
        ResponseInputStream<GetObjectResponse> ris =
                new ResponseInputStream<>(GetObjectResponse.builder().build(), AbortableInputStream.create(failingIn));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(ris);

        ReflectionTestUtils.setField(service, "bucket", "my-bucket");
        var msg =
                new VideoMensagem("u1", "video_teste.mp4", "/entrada/video_teste.mp4", LocalDateTime.now(), "x@y.com");

        IOException ex = assertThrows(IOException.class, () -> service.processarVideo(msg));
        assertThat(ex).hasMessage("io-boom");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
