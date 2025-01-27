package com.melissa.diary.ai;

import java.util.Optional;

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

}
