package com.mobilestore.mobile_store.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.mobilestore.mobile_store.exception.InvalidFileException;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalFileStorageService(tempDir.toString(), "/uploads");
    }

    @Test
    void storesFileAndReturnsPublicUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "fake-bytes".getBytes());

        String url = service.store(file, "products");

        assertThat(url).startsWith("/uploads/products/").endsWith(".png");
    }

    @Test
    void writesFileToDiskUnderRequestedSubDirectory() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-bytes".getBytes());

        String url = service.store(file, "products");
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        assertThat(Files.exists(tempDir.resolve("products").resolve(fileName))).isTrue();
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThrows(InvalidFileException.class, () -> service.store(empty, "products"));
    }

    @Test
    void rejectsOversizedFile() {
        MockMultipartFile huge = new MockMultipartFile("file", "huge.png", "image/png", new byte[6 * 1024 * 1024]);

        assertThrows(InvalidFileException.class, () -> service.store(huge, "products"));
    }

    @Test
    void rejectsDisallowedContentType() {
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", "fake-bytes".getBytes());

        assertThrows(InvalidFileException.class, () -> service.store(pdf, "products"));
    }

    @Test
    void rejectsPathTraversalInSubDirectory() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "fake-bytes".getBytes());

        assertThrows(InvalidFileException.class, () -> service.store(file, "../../etc"));
    }

    @Test
    void deleteRemovesAPreviouslyStoredFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "fake-bytes".getBytes());
        String url = service.store(file, "products");
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(tempDir.resolve("products").resolve(fileName))).isTrue();

        service.delete(url);

        assertThat(Files.exists(tempDir.resolve("products").resolve(fileName))).isFalse();
    }

    @Test
    void deleteIgnoresUrlsItDidNotProduce() {
        assertDoesNotThrow(() -> service.delete("https://example.com/some-external-image.png"));
        assertDoesNotThrow(() -> service.delete("data:image/png;base64,abc123"));
        assertDoesNotThrow(() -> service.delete(null));
    }

    @Test
    void deleteIgnoresPathTraversalAttempts() {
        assertDoesNotThrow(() -> service.delete("/uploads/../../outside-secret.txt"));
    }
}
