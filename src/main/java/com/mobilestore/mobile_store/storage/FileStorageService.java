package com.mobilestore.mobile_store.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Stores the given file under a logical sub-directory (e.g. "products") and
     * returns a publicly reachable URL for it. Implementations may back this with
     * local disk, S3, or any other object store without callers needing to change.
     */
    String store(MultipartFile file, String subDirectory);

    /**
     * Best-effort delete of a previously-stored file by its public URL. URLs that
     * weren't produced by this service (external links, legacy inline data) are
     * silently ignored rather than treated as an error.
     */
    void delete(String url);
}
