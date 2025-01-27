package com.melissa.diary.aws.s3;

        import com.amazonaws.services.s3.AmazonS3;
        import com.amazonaws.services.s3.model.ObjectMetadata;
        import com.amazonaws.services.s3.model.PutObjectRequest;
        import com.melissa.diary.config.AmazonConfig;
        import com.melissa.diary.domain.Uuid;
        import com.melissa.diary.repository.UuidRepository;
        import lombok.RequiredArgsConstructor;
        import lombok.extern.slf4j.Slf4j;
        import org.springframework.stereotype.Component;
        import org.springframework.web.multipart.MultipartFile;

        import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Manager{

    private final AmazonS3 amazonS3;

    private final AmazonConfig amazonConfig;

    public String uploadFile(String keyName, MultipartFile file){
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        try {
            amazonS3.putObject(new PutObjectRequest(amazonConfig.getBucket(), keyName, file.getInputStream(), metadata));
        }catch (IOException e){
            log.error("error at AmazonS3Manager uploadFile : {}", (Object) e.getStackTrace());
        }

        return amazonS3.getUrl(amazonConfig.getBucket(), keyName).toString();
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
}