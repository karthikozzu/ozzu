package ai.ozzu.api.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Media Service to upload media
 */
public interface MediaService {

    String uploadImage(
            byte[] fileBytes,
            String fileName,
            String contentType,
            String mediaType,
            UUID entityId
    );
}
