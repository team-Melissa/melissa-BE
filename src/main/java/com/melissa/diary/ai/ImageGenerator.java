package com.melissa.diary.ai;

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
    private static final String IMAGE_CONTENT_TYPE = "image/png";

    private final ImageModel imageModel;
    private final UuidRepository uuidRepository;
    private final AmazonS3Manager amazonS3Manager;

    public GeneratedImage generateImage(String prompt) {
        ImageOptions imageOptions = OpenAiImageOptions
                .builder()
                .model("dall-e-3")
                .style("vivid")
                .width(1024)
                .height(1024)
                .build();
        ImagePrompt imagePrompt = new ImagePrompt(prompt, imageOptions);

        ImageResponse imageResponse = imageModel.call(imagePrompt);
        return resolveImageContent(imageResponse);
    }

    private GeneratedImage resolveImageContent(ImageResponse imageResponse) {
        Image image = imageResponse.getResult().getOutput();
        if (hasText(image.getB64Json())) {
            return GeneratedImage.base64(image.getB64Json());
        }
        if (hasText(image.getUrl())) {
            return GeneratedImage.url(image.getUrl());
        }

        throw new IllegalArgumentException("Image response has no base64 or url content.");
    }

    public String genProfileImage(String prompt) {
        GeneratedImage generatedImage = generateImage(prompt);

        String uuid = UUID.randomUUID().toString();
        Uuid savedUuid = uuidRepository.save(Uuid.builder()
                .uuid(uuid).build());

        String keyName = amazonS3Manager.generateAiProfileKeyName(savedUuid);

        return uploadGeneratedImage(keyName, generatedImage);
    }

    public String genDiaryImage(String prompt) {
        GeneratedImage generatedImage = generateImage(prompt);

        String uuid = UUID.randomUUID().toString();
        Uuid savedUuid = uuidRepository.save(Uuid.builder()
                .uuid(uuid).build());

        String keyName = amazonS3Manager.generateDiaryKeyName(savedUuid);

        return uploadGeneratedImage(keyName, generatedImage);
    }

    private String uploadGeneratedImage(String keyName, GeneratedImage generatedImage) {
        if (generatedImage.isBase64()) {
            return amazonS3Manager.uploadFileFromBase64(keyName, generatedImage.content(), IMAGE_CONTENT_TYPE);
        }

        return amazonS3Manager.uploadFileFromUrl(keyName, generatedImage.content(), IMAGE_CONTENT_TYPE);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record GeneratedImage(String content, Type type) {
        public static GeneratedImage base64(String content) {
            return new GeneratedImage(content, Type.BASE64);
        }

        public static GeneratedImage url(String content) {
            return new GeneratedImage(content, Type.URL);
        }

        public boolean isBase64() {
            return type == Type.BASE64;
        }

        private enum Type {
            BASE64,
            URL
        }
    }
}
