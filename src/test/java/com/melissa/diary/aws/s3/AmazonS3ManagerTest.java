package com.melissa.diary.aws.s3;

import com.sun.net.httpserver.HttpServer;
import com.melissa.diary.config.AmazonConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmazonS3ManagerTest {

    @Test
    void uploadFilePropagatesNonRetryableS3Failure() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        AmazonConfig amazonConfig = mock(AmazonConfig.class);
        MultipartFile multipartFile = mock(MultipartFile.class);
        AmazonS3Manager manager = new AmazonS3Manager(s3Client, amazonConfig);

        AwsServiceException exception = AwsServiceException.builder()
                .message("forbidden")
                .statusCode(403)
                .build();

        when(amazonConfig.getBucket()).thenReturn("bucket");
        when(multipartFile.getSize()).thenReturn(4L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(exception);

        assertThatThrownBy(() -> manager.uploadFile("key.png", multipartFile))
                .isSameAs(exception);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFileFromUrlDownloadsAndUploadsImage() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        AmazonConfig amazonConfig = mock(AmazonConfig.class);
        AmazonS3Manager manager = new AmazonS3Manager(s3Client, amazonConfig);
        byte[] imageBytes = new byte[]{1, 2, 3, 4};

        when(amazonConfig.getBucket()).thenReturn("bucket");

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/image.png", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, imageBytes.length);
            exchange.getResponseBody().write(imageBytes);
            exchange.close();
        });
        server.start();

        try {
            String imageUrl = "http://localhost:" + server.getAddress().getPort() + "/image.png";

            String result = manager.uploadFileFromUrl("diary/key.png", imageUrl, "image/png");

            assertThat(result).isEqualTo("diary/key.png");

            ArgumentCaptor<PutObjectRequest> captor = forClass(PutObjectRequest.class);
            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
            assertThat(captor.getValue().contentLength()).isEqualTo(imageBytes.length);
            assertThat(captor.getValue().contentType()).isEqualTo("image/png");
        } finally {
            server.stop(0);
        }
    }
}
