package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.on.fiap.hackathonprocessadorvideo.domain.model.VideoMensagem;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception.FrameExtractionException;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception.FrameReadException;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception.NoFramesExtractedException;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception.VideoOpenException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("VideoProcessingServiceAdapter - Cenários de exceção do extractor")
class VideoProcessingServiceAdapterExtractorExceptionTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private FrameExtractor extractorMock;

    @InjectMocks
    private VideoProcessingServiceAdapter service;

    @BeforeEach
    void setup() {
        service = new VideoProcessingServiceAdapter(s3Client, extractorMock);
        ReflectionTestUtils.setField(service, "bucket", "my-bucket");
    }

    private VideoMensagem videoMsg() {
        return new VideoMensagem("user-123", "video.mp4", "/entrada/video.mp4", LocalDateTime.now(), "x@y.com");
    }

    @Test
    @DisplayName("FrameReadException deve ser propagada")
    void givenFrameReadException_whenProcessarVideo_thenPropagates() throws Exception {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream(new byte[] {0}))));

        when(extractorMock.extract(any(Path.class))).thenThrow(new FrameReadException(3, new IOException("read fail")));

        FrameReadException ex = assertThrows(FrameReadException.class, () -> service.processarVideo(videoMsg()));
        assertThat(ex.getMessage()).contains("3");
        verifyNoMoreInteractions(s3Client);
    }

    @Test
    @DisplayName("VideoOpenException deve ser propagada (construtor com cause)")
    void givenVideoOpenExceptionWithCause_whenProcessarVideo_thenPropagates() throws Exception {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream(new byte[] {0}))));

        when(extractorMock.extract(any(Path.class)))
                .thenThrow(new VideoOpenException("fail open", new IOException("cause")));

        VideoOpenException ex = assertThrows(VideoOpenException.class, () -> service.processarVideo(videoMsg()));
        assertThat(ex.getMessage()).contains("fail open");
        verifyNoMoreInteractions(s3Client);
    }

    @Test
    @DisplayName("VideoOpenException deve ser propagada (construtor simples)")
    void givenVideoOpenExceptionWithMessage_whenProcessarVideo_thenPropagates() throws Exception {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream(new byte[] {0}))));

        when(extractorMock.extract(any(Path.class))).thenThrow(new VideoOpenException("fail open"));

        VideoOpenException ex = assertThrows(VideoOpenException.class, () -> service.processarVideo(videoMsg()));
        assertThat(ex.getMessage()).contains("fail open");
        verifyNoMoreInteractions(s3Client);
    }

    @Test
    @DisplayName("FrameReadException deve ser propagada (frameIndex)")
    void givenFrameReadExceptionWithIndex_whenProcessarVideo_thenPropagates() throws Exception {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream(new byte[] {0}))));

        when(extractorMock.extract(any(Path.class))).thenThrow(new FrameReadException(5, new IOException("read fail")));

        FrameReadException ex = assertThrows(FrameReadException.class, () -> service.processarVideo(videoMsg()));
        assertThat(ex.getMessage()).contains("5");
        assertNotEquals(0, ex.getFrameIndex());
        verifyNoMoreInteractions(s3Client);
    }

    @Test
    @DisplayName("FrameReadException deve ser propagada (message)")
    void givenFrameReadExceptionWithMessage_whenProcessarVideo_thenPropagates() throws Exception {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream(new byte[] {0}))));

        when(extractorMock.extract(any(Path.class)))
                .thenThrow(new FrameReadException("custom message", new IOException("cause")));

        FrameReadException ex = assertThrows(FrameReadException.class, () -> service.processarVideo(videoMsg()));
        assertThat(ex.getMessage()).contains("custom message");
        verifyNoMoreInteractions(s3Client);
    }

    @Test
    @DisplayName("FrameExtractionException deve ser propagada")
    void givenFrameExtractionException_whenProcessarVideo_thenPropagates() throws Exception {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                        GetObjectResponse.builder().build(),
                        AbortableInputStream.create(new ByteArrayInputStream(new byte[] {0}))));
        when(extractorMock.extract(any(Path.class))).thenThrow(new FrameExtractionException("extract fail"));

        FrameExtractionException ex =
                assertThrows(FrameExtractionException.class, () -> service.processarVideo(videoMsg()));
        assertThat(ex.getMessage()).contains("extract fail");
        verifyNoMoreInteractions(s3Client);
    }

    @Test
    @DisplayName("NoFramesExtractedException do extractor é propagada")
    void givenExtractorThrowsNoFrames_whenExtract_thenPropagates() throws Exception {
        Path videoFile = Path.of("video.mp4");

        doThrow(new NoFramesExtractedException("no frames")).when(extractorMock).extract(videoFile);

        NoFramesExtractedException ex =
                assertThrows(NoFramesExtractedException.class, () -> extractorMock.extract(videoFile));

        assertThat(ex.getMessage()).contains("no frames");
    }

    @Test
    void testFinalizeOrThrowIfEmpty_noException() throws Exception {
        FfmpegFrameExtractor extractor = new FfmpegFrameExtractor();
        Path videoFile = Path.of("video.mp4");
        Path framesDir = Files.createTempDirectory("frames");

        Method method = FfmpegFrameExtractor.class.getDeclaredMethod(
                "finalizeOrThrowIfEmpty", int.class, Path.class, Path.class);
        method.setAccessible(true);

        Path result = (Path) method.invoke(extractor, 5, videoFile, framesDir);
        assertEquals(framesDir, result);

        InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, () -> method.invoke(extractor, 0, videoFile, framesDir));
        assertInstanceOf(NoFramesExtractedException.class, ex.getCause());
    }

    @Test
    void givenGrabberStartThrowsException_whenStartGrabberOrThrow_thenVideoOpenException() throws Exception {
        FfmpegFrameExtractor extractor = new FfmpegFrameExtractor();
        Path videoFile = Files.createTempFile("video", ".mp4");
        Path framesDir = Files.createTempDirectory("frames");

        FFmpegFrameGrabber grabberMock = mock(FFmpegFrameGrabber.class);
        doThrow(new FFmpegFrameGrabber.Exception("grabber fail"))
                .when(grabberMock)
                .start();

        Method method = FfmpegFrameExtractor.class.getDeclaredMethod(
                "startGrabberOrThrow", FFmpegFrameGrabber.class, Path.class, Path.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(
                InvocationTargetException.class, () -> method.invoke(extractor, grabberMock, videoFile, framesDir));

        assertInstanceOf(VideoOpenException.class, ex.getCause());
        assertThat(ex.getCause().getMessage()).contains("Failed to open video via FFmpeg");
    }

    @Test
    void givenGrabberThrowsOnGrabImage_whenExtractFrames_thenFrameReadException() throws Exception {
        FfmpegFrameExtractor extractor = new FfmpegFrameExtractor();
        Path framesDir = Files.createTempDirectory("frames");

        FFmpegFrameGrabber grabberMock = mock(FFmpegFrameGrabber.class);
        when(grabberMock.grabImage()).thenThrow(new FFmpegFrameGrabber.Exception("grab fail"));

        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        Method method = FfmpegFrameExtractor.class.getDeclaredMethod(
                "extractFrames", FFmpegFrameGrabber.class, OpenCVFrameConverter.ToMat.class, Path.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(
                InvocationTargetException.class, () -> method.invoke(extractor, grabberMock, converter, framesDir));

        assertInstanceOf(FrameReadException.class, ex.getCause());
        assertInstanceOf(FFmpegFrameGrabber.Exception.class, ex.getCause().getCause());
        assertThat(ex.getCause().getMessage()).contains("0");
    }

    @Test
    void testCreateFramesDir_unsupportedOperation() throws Exception {
        FfmpegFrameExtractor extractor = new FfmpegFrameExtractor();
        Path tempDir = Files.createTempDirectory("frames-ffmpeg-");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            mockedFiles.when(() -> Files.createTempDirectory(anyString())).thenReturn(tempDir);

            mockedFiles
                    .when(() -> Files.setPosixFilePermissions(any(Path.class), any(Set.class)))
                    .thenThrow(new UnsupportedOperationException("POSIX not supported"));

            Method method = FfmpegFrameExtractor.class.getDeclaredMethod("createFramesDir");
            method.setAccessible(true);

            Path result = (Path) method.invoke(extractor);

            assertNotNull(result);
            assertEquals(tempDir, result);
        }
    }
}
