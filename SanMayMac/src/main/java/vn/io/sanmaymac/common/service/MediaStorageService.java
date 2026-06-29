package vn.io.sanmaymac.common.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class MediaStorageService {
    private static final String STORAGE_PROVIDER_S3 = "s3";

    private final String storageProvider;
    private final Path uploadRoot;
    private final String s3Bucket;
    private final String s3Prefix;
    private final String s3Region;
    private final String s3PublicBaseUrl;
    private final S3Client s3Client;

    public MediaStorageService(
            @Value("${app.storage.provider:local}") String storageProvider,
            @Value("${app.storage.upload-dir:uploads}") String uploadDir,
            @Value("${app.storage.s3.bucket:}") String s3Bucket,
            @Value("${app.storage.s3.prefix:uploads}") String s3Prefix,
            @Value("${app.storage.s3.region:ap-southeast-1}") String s3Region,
            @Value("${app.storage.s3.public-base-url:}") String s3PublicBaseUrl,
            @Value("${app.storage.s3.access-key:}") String s3AccessKey,
            @Value("${app.storage.s3.secret-key:}") String s3SecretKey) {
        this.storageProvider = storageProvider == null ? "local" : storageProvider.trim().toLowerCase();
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.s3Bucket = s3Bucket == null ? "" : s3Bucket.trim();
        this.s3Prefix = s3Prefix == null || s3Prefix.isBlank() ? "uploads" : s3Prefix.trim();
        this.s3Region = s3Region == null || s3Region.isBlank() ? "ap-southeast-1" : s3Region.trim();
        this.s3PublicBaseUrl = s3PublicBaseUrl == null ? "" : s3PublicBaseUrl.trim();
        this.s3Client = initS3Client(s3AccessKey, s3SecretKey);
    }

    public String store(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String safePrefix = prefix == null || prefix.isBlank() ? "file" : prefix;
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = safePrefix + "-" + UUID.randomUUID() + "-" + safeName;

        if (STORAGE_PROVIDER_S3.equals(storageProvider)) {
            return storeOnS3(file, fileName);
        }

        return storeOnLocal(file, fileName);
    }

    private String storeOnLocal(MultipartFile file, String fileName) {
        try {
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(fileName);
            Files.copy(file.getInputStream(), target);
            return "/uploads/" + fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store uploaded file", ex);
        }
    }

    private String storeOnS3(MultipartFile file, String fileName) {
        if (s3Client == null) {
            throw new IllegalStateException("S3 storage is enabled but S3 client is not configured");
        }
        if (s3Bucket.isBlank()) {
            throw new IllegalStateException("S3 storage is enabled but app.storage.s3.bucket is empty");
        }

        String objectKey = (s3Prefix + "/" + fileName).replaceAll("^/+", "");
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return buildS3PublicUrl(objectKey);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not upload file to AWS S3", ex);
        }
    }

    private String buildS3PublicUrl(String objectKey) {
        String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8).replace("+", "%20");
        if (!s3PublicBaseUrl.isBlank()) {
            String base = s3PublicBaseUrl.endsWith("/") ? s3PublicBaseUrl.substring(0, s3PublicBaseUrl.length() - 1) : s3PublicBaseUrl;
            return base + "/" + encodedKey;
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s", s3Bucket, s3Region, encodedKey);
    }

    private S3Client initS3Client(String s3AccessKey, String s3SecretKey) {
        if (!STORAGE_PROVIDER_S3.equals(storageProvider)) {
            return null;
        }

        var builder = S3Client.builder().region(Region.of(s3Region));
        if (s3AccessKey != null && !s3AccessKey.isBlank() && s3SecretKey != null && !s3SecretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3AccessKey.trim(), s3SecretKey.trim())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}