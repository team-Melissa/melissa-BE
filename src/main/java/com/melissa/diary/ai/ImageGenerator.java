package com.melissa.diary.ai;

import java.util.Optional;
import java.util.UUID;

import com.melissa.diary.aws.s3.AmazonS3Manager;
import com.melissa.diary.domain.Uuid;
import com.melissa.diary.repository.UuidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ImageGenerator {
    private final ImageModel imageModel;
    private final UuidRepository uuidRepository;
    private final AmazonS3Manager amazonS3Manager;

    public String generateB64(String prompt) {
        ImageOptions imageOptions = OpenAiImageOptions
                .builder()
                .model("dall-e-3")
                .withHeight(1024)
                .withWidth(1024)
                .responseFormat("b64_json")
                .build();
        ImagePrompt imagePrompt = new ImagePrompt(prompt, imageOptions);

        ImageResponse imageResponse = imageModel.call(imagePrompt);
        return resolveImageContent(imageResponse);
    }

    private String resolveImageContent(ImageResponse imageResponse) {
        Image image = imageResponse.getResult().getOutput();
        return Optional
                .ofNullable(image.getB64Json())
                .orElseThrow(() -> new IllegalArgumentException("이미지의 Base64 값이 없습니다."));
    }

    public String genProfileImage(String prompt) {
        String base64Img = generateB64(prompt);

        // keyName 생성 (ex. "profile/1679999999999.png")
        String uuid = UUID.randomUUID().toString();
        Uuid savedUuid = uuidRepository.save(Uuid.builder()
                .uuid(uuid).build());

        String keyName = amazonS3Manager.generateAiProfileKeyName(savedUuid);

        // S3 업로드
        return amazonS3Manager.uploadFileFromBase64(keyName, base64Img, "image/png");
    }

}
