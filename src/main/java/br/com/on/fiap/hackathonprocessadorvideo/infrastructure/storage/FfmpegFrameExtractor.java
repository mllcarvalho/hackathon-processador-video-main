package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage;

import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception.FrameExtractionException;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception.FrameReadException;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception.NoFramesExtractedException;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.exception.VideoOpenException;
import br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.util.StorageFileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.stereotype.Component;

@Slf4j
@Component("mp4Extractor")
public final class FfmpegFrameExtractor implements FrameExtractor {

    @Override
    public Path extract(Path videoFile) throws IOException {
        ensureNativesLoaded();
        log.info("Starting frame extraction (FFmpeg) from {}", videoFile);

        Path framesDir = createFramesDir();

        try (FFmpegFrameGrabber grabber = newGrabber(videoFile);
                OpenCVFrameConverter.ToMat converter = newConverter()) {

            startGrabberOrThrow(grabber, videoFile, framesDir);

            int count = extractFrames(grabber, converter, framesDir);
            return finalizeOrThrowIfEmpty(count, videoFile, framesDir);

        } catch (FFmpegFrameGrabber.Exception e) {
            cleanupOnError(videoFile, framesDir);
            throw new FrameExtractionException("Error while reading frames via FFmpeg: " + videoFile, e);
        }
    }

    private void ensureNativesLoaded() {
        Loader.load(avutil.class);
    }

    private Path createFramesDir() throws IOException {
        Path dir = Files.createTempDirectory("frames-ffmpeg-");
        try {
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwx------");
            Files.setPosixFilePermissions(dir, perms);
        } catch (UnsupportedOperationException e) {
            log.warn("Cannot set POSIX permissions on temp directory {}: {}", dir, e.getMessage());
        }
        log.debug("Created temp frames dir with restricted permissions: {}", dir);
        return dir;
    }

    private FFmpegFrameGrabber newGrabber(Path videoFile) {
        return new FFmpegFrameGrabber(videoFile.toString());
    }

    private OpenCVFrameConverter.ToMat newConverter() {
        return new OpenCVFrameConverter.ToMat();
    }

    private void startGrabberOrThrow(FFmpegFrameGrabber grabber, Path videoFile, Path framesDir)
            throws VideoOpenException {
        try {
            grabber.start();
            log.debug(
                    "FFmpegFrameGrabber started: format={}, videoCodec={}, width={} height={}",
                    grabber.getFormat(),
                    grabber.getVideoCodec(),
                    grabber.getImageWidth(),
                    grabber.getImageHeight());
        } catch (FFmpegFrameGrabber.Exception startEx) {
            cleanupOnError(videoFile, framesDir);
            throw new VideoOpenException("Failed to open video via FFmpeg: " + videoFile, startEx);
        }
    }

    private int extractFrames(FFmpegFrameGrabber grabber, OpenCVFrameConverter.ToMat converter, Path framesDir)
            throws FrameExtractionException {
        int count = 0;

        while (true) {
            final Frame frame;
            try {
                frame = grabber.grabImage();
            } catch (FFmpegFrameGrabber.Exception e) {
                log.warn("FFmpeg read error after {} frames; aborting extraction (dir: {})", count, framesDir, e);
                throw new FrameReadException(count, e);
            }

            if (frame == null) break;

            Mat mat = converter.convert(frame);
            if (mat != null && !mat.empty()) {
                boolean saved = writeFramePng(mat, framesDir, count);
                mat.release();
                if (saved) count++;
            }
        }
        return count;
    }

    private boolean writeFramePng(Mat mat, Path framesDir, int index) {
        Path out = framesDir.resolve("frame-" + index + ".png");
        boolean ok = opencv_imgcodecs.imwrite(out.toString(), mat);
        if (!ok) {
            log.warn("Failed to write frame {} to {}", index, out);
        } else if (log.isTraceEnabled()) {
            log.trace("Wrote frame {} -> {}", index, out);
        }
        return ok;
    }

    private Path finalizeOrThrowIfEmpty(int count, Path videoFile, Path framesDir) throws NoFramesExtractedException {
        if (count == 0) {
            cleanupOnError(videoFile, framesDir);
            throw new NoFramesExtractedException("No frames extracted from video: " + videoFile);
        }
        log.info("Extracted {} frames into {}", count, framesDir);
        return framesDir;
    }

    private static void cleanupOnError(Path videoFile, Path framesDir) {
        StorageFileUtil.deleteQuietly(videoFile);
        StorageFileUtil.deleteRecursively(framesDir);
    }
}
