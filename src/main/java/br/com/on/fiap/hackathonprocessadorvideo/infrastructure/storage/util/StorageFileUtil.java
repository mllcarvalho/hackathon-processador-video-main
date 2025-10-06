package br.com.on.fiap.hackathonprocessadorvideo.infrastructure.storage.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class StorageFileUtil {

    private StorageFileUtil() {}

    public static void deleteQuietly(Path p) {
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.warn("Failed to delete path {}", p, e);
        }
    }

    public static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) return;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                @SuppressWarnings("NullableProblems")
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        log.warn("Failed to delete file {}", file, e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                @SuppressWarnings("NullableProblems")
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    try {
                        Files.deleteIfExists(dir);
                    } catch (IOException e) {
                        log.warn("Failed to delete dir {}", dir, e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Error walking tree for {}", root, e);
        }
    }

    public static Path zipDirectory(Path dir) throws IOException {
        Path zip = Files.createTempFile("frames-", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip));
                Stream<Path> paths = Files.walk(dir)) {

            paths.filter(Files::isRegularFile).forEach(file -> {
                try (InputStream in = Files.newInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(file.getFileName().toString()));
                    in.transferTo(zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    log.warn("Error zipping file {} from dir {}", file, dir, e);
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            log.warn("Zip operation failed for dir {}", dir, e.getCause());
            throw e.getCause();
        } catch (IOException e) {
            log.warn("Zip operation failed for dir {}", dir, e);
            throw e;
        }
        return zip;
    }
}
