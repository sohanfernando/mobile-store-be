package com.mobilestore.mobile_store.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.mobilestore.mobile_store.exception.FileStorageException;
import com.mobilestore.mobile_store.exception.InvalidFileException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private final Path storageRoot;
    private final String publicBaseUrl;

    public LocalFileStorageService(
            @Value("${app.storage.location:./uploads}") String storageLocation,
            @Value("${app.storage.public-base-url:/uploads}") String publicBaseUrl) {
        this.storageRoot = Path.of(storageLocation).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize file storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file was provided");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new InvalidFileException("File exceeds the maximum allowed size of 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidFileException("Unsupported file type. Allowed types: JPEG, PNG, WEBP, GIF");
        }

        String extension = extensionFor(contentType);
        String fileName = UUID.randomUUID() + extension;

        try {
            Path targetDir = storageRoot.resolve(subDirectory).normalize();
            if (!targetDir.startsWith(storageRoot)) {
                throw new InvalidFileException("Invalid storage sub-directory");
            }
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(fileName);
            file.transferTo(targetFile);
            return publicBaseUrl + "/" + subDirectory + "/" + fileName;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store uploaded file", e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith(publicBaseUrl + "/")) {
            // Not a file this service produced (external URL or legacy inline data) - nothing to clean up.
            return;
        }
        String relativePath = url.substring(publicBaseUrl.length() + 1);
        try {
            Path target = storageRoot.resolve(relativePath).normalize();
            if (!target.startsWith(storageRoot)) {
                log.warn("Refusing to delete file outside storage root for url {}", url);
                return;
            }
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete stored file for url {}", url, e);
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }
}
