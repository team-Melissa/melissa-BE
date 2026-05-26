package com.melissa.diary.aws.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.melissa.diary.config.AmazonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmazonS3ManagerTest {

    @Test
    void uploadFilePropagatesNonRetryableS3Failure() throws Exception {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        AmazonConfig amazonConfig = mock(AmazonConfig.class);
        MultipartFile multipartFile = mock(MultipartFile.class);
        AmazonS3Manager manager = new AmazonS3Manager(amazonS3, amazonConfig);

        AmazonServiceException exception = new AmazonServiceException("forbidden");
        exception.setStatusCode(403);

        when(amazonConfig.getBucket()).thenReturn("bucket");
        when(multipartFile.getSize()).thenReturn(4L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        when(amazonS3.putObject(any())).thenThrow(exception);

        assertThatThrownBy(() -> manager.uploadFile("key.png", multipartFile))
                .isSameAs(exception);

        verify(amazonS3).putObject(any());
    }
}
