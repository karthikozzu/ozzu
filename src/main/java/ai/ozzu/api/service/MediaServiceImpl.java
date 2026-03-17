package ai.ozzu.api.service;

import ai.ozzu.api.config.MediaProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.UUID;

@Service
public class MediaServiceImpl implements MediaService {

    @Autowired
    private S3Client s3Client;
    @Autowired
    private MediaProperties props;

    @Value("${ozzu.media.bucket}")
    private String bucket;

    public MediaServiceImpl(S3Client s3Client, MediaProperties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    public MediaServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadImage(
            byte[] fileBytes,
            String fileName,
            String contentType,
            String mediaType,
            UUID entityId
    ) {

        try {

            String bucket = props.getBucket();
            validate(fileBytes, contentType);

            String extension = getExtension(fileName);

            String key = mediaType.toLowerCase()
                    + "/"
                    + entityId
                    + "/"
                    + UUID.randomUUID()
                    + "."
                    + extension;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(fileBytes)
            );

            return "https://" + bucket + ".s3.amazonaws.com/" + key;
        } catch (Exception e) {
            throw new RuntimeException("Image upload failed", e);
        }
    }

    private void validate(byte[] fileBytes, String contentType) {

        if (fileBytes == null || fileBytes.length == 0)
            throw new IllegalArgumentException("File cannot be empty");

        if (fileBytes.length > 5_000_000)
            throw new IllegalArgumentException("File too large (max 5MB)");

        if (contentType == null ||
                !List.of("image/jpeg","image/png","image/webp").contains(contentType))
            throw new IllegalArgumentException("Unsupported image type");
    }

    private String getExtension(String fileName) {

        if (fileName == null) return "jpg";

        int index = fileName.lastIndexOf(".");
        if (index == -1) return "jpg";

        return fileName.substring(index + 1);
    }
}