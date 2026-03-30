package com.melissa.diary.aws.s3;

import com.melissa.diary.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@RequiredArgsConstructor
public class S3AssetUrlResolver {

    private final AmazonConfig amazonConfig;

    public String resolve(String storedValue) {
        if (!StringUtils.hasText(storedValue)) {
            return null;
        }

        String normalized = storedValue.trim();

        if (isHttpUrl(normalized)) {
            String extractedKey = extractKeyFromUrl(normalized);
            return StringUtils.hasText(extractedKey) ? buildPublicUrl(extractedKey) : normalized;
        }

        if (normalized.startsWith("s3://")) {
            String extractedKey = extractKeyFromS3Uri(normalized);
            return StringUtils.hasText(extractedKey) ? buildPublicUrl(extractedKey) : normalized;
        }

        return buildPublicUrl(normalized);
    }

    public String toStorageKey(String storedValue) {
        if (!StringUtils.hasText(storedValue)) {
            return null;
        }

        String normalized = storedValue.trim();

        if (isHttpUrl(normalized)) {
            String extractedKey = extractKeyFromUrl(normalized);
            return StringUtils.hasText(extractedKey) ? extractedKey : normalized;
        }

        if (normalized.startsWith("s3://")) {
            String extractedKey = extractKeyFromS3Uri(normalized);
            return StringUtils.hasText(extractedKey) ? extractedKey : normalized;
        }

        return stripLeadingSlash(normalized);
    }

    private String buildPublicUrl(String key) {
        String normalizedKey = stripLeadingSlash(key);
        String baseUrl = StringUtils.hasText(amazonConfig.getPublicBaseUrl())
                ? amazonConfig.getPublicBaseUrl().trim()
                : "https://%s.s3.%s.amazonaws.com".formatted(amazonConfig.getBucket(), amazonConfig.getRegion());

        if (baseUrl.endsWith("/")) {
            return baseUrl + normalizedKey;
        }
        return baseUrl + "/" + normalizedKey;
    }

    private String extractKeyFromUrl(String absoluteUrl) {
        try {
            URI uri = new URI(absoluteUrl);
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return null;
            }

            String path = stripLeadingSlash(uri.getPath());
            if (!StringUtils.hasText(path)) {
                return null;
            }

            if (isPathStyleS3Host(host)) {
                return removeFirstPathSegment(path);
            }

            if (path.startsWith(amazonConfig.getBucket() + "/")) {
                return path.substring(amazonConfig.getBucket().length() + 1);
            }

            if (host.equalsIgnoreCase(getConfiguredPublicHost()) || host.contains(".amazonaws.com")) {
                return path;
            }

            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private String extractKeyFromS3Uri(String s3Uri) {
        String withoutScheme = s3Uri.substring("s3://".length());
        int firstSlash = withoutScheme.indexOf('/');
        if (firstSlash < 0 || firstSlash == withoutScheme.length() - 1) {
            return null;
        }
        return stripLeadingSlash(withoutScheme.substring(firstSlash + 1));
    }

    private String getConfiguredPublicHost() {
        String publicBaseUrl = amazonConfig.getPublicBaseUrl();
        if (!StringUtils.hasText(publicBaseUrl)) {
            return "%s.s3.%s.amazonaws.com".formatted(amazonConfig.getBucket(), amazonConfig.getRegion());
        }

        try {
            return new URI(publicBaseUrl).getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private boolean isHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private boolean isPathStyleS3Host(String host) {
        return "s3.amazonaws.com".equalsIgnoreCase(host)
                || host.startsWith("s3.")
                || host.startsWith("s3-");
    }

    private String removeFirstPathSegment(String value) {
        int firstSlash = value.indexOf('/');
        if (firstSlash < 0 || firstSlash == value.length() - 1) {
            return value;
        }
        return value.substring(firstSlash + 1);
    }

    private String stripLeadingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
