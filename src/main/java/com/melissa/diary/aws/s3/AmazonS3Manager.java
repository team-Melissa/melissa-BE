package com.melissa.diary.aws.s3;

import com.melissa.diary.config.AmazonConfig;
import com.melissa.diary.domain.Uuid;
import com.melissa.diary.retry.RetryExecutor;
import com.melissa.diary.retry.RetryPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Base64;
import java.util.Optional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Manager {

    private final S3Client s3Client;

    private final AmazonConfig amazonConfig;

    public String uploadFile(String keyName, MultipartFile file) {
        return RetryExecutor.execute("s3.uploadFile", RetryPolicy.S3_UPLOAD, () -> {
            long contentLength = file.getSize();
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(amazonConfig.getBucket())
                    .key(keyName)
                    .contentLength(contentLength);

            String contentType = file.getContentType();
            if (contentType != null && !contentType.isBlank()) {
                requestBuilder.contentType(contentType);
            }

            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(
                        requestBuilder.build(),
                        RequestBody.fromInputStream(inputStream, contentLength)
                );
            }
            return keyName;
        });
    }

    public String uploadFileFromBase64(String keyName, String base64Data, String contentType) {
        base64Data = base64Data.replaceAll("\\s+", "");
        byte[] fileContent = Base64.getDecoder().decode(base64Data);
        return uploadFileFromBytes(keyName, fileContent, contentType);
    }

    public String uploadFileFromUrl(String keyName, String fileUrl, String fallbackContentType) {
        DownloadedFile downloadedFile = RetryExecutor.execute("image.download", RetryPolicy.S3_UPLOAD,
                () -> downloadFile(fileUrl, fallbackContentType));

        return uploadFileFromBytes(keyName, downloadedFile.content(), downloadedFile.contentType());
    }

    public String uploadFileFromBytes(String keyName, byte[] fileContent, String contentType) {
        MultipartFile multipartFile = new Base64ToMultipartFile(fileContent, keyName, contentType);
        return uploadFile(keyName, multipartFile);
    }

    private DownloadedFile downloadFile(String fileUrl, String fallbackContentType) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(fileUrl).toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);

        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("image download failed. statusCode=" + statusCode);
        }

        String contentType = Optional.ofNullable(connection.getContentType())
                .filter(value -> !value.isBlank())
                .orElse(fallbackContentType);

        try (InputStream inputStream = connection.getInputStream()) {
            return new DownloadedFile(inputStream.readAllBytes(), contentType);
        } finally {
            connection.disconnect();
        }
    }

    public String generateDaySummaryKeyName(Uuid uuid) {
        return amazonConfig.getDaySummary() + '/' + uuid.getUuid();
    }

    public String generateMonthSummaryKeyName(Uuid uuid) {
        return amazonConfig.getMonthSummary() + '/' + uuid.getUuid();
    }

    public String generateAiProfileKeyName(Uuid uuid) {
        return amazonConfig.getAiProfile() + '/' + uuid.getUuid();
    }

    public String generateDiaryKeyName(Uuid uuid) {
        return amazonConfig.getDiary() + '/' + uuid.getUuid();
    }

    private record DownloadedFile(byte[] content, String contentType) {
    }
}
