package com.melissa.diary.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private Iap iap = new Iap();
    private Google google = new Google();
    private Apple apple = new Apple();

    @Getter
    @Setter
    public static class Iap {
        private boolean enabled = false;
        private long verifyTimeoutMs = 5000;
        private StoreProduct storeProduct = new StoreProduct();
    }

    @Getter
    @Setter
    public static class StoreProduct {
        private String removeAds = "remove_ads";
    }

    @Getter
    @Setter
    public static class Google {
        private boolean enabled = false;
        private String packageName = "com.melissa.melissaFE";
        private String productRemoveAds = "premium";
        private String serviceAccountJsonBase64 = "";
        private String tokenHashSalt = "";
    }

    @Getter
    @Setter
    public static class Apple {
        private boolean enabled = false;
        private String bundleId = "com.melissa.melissaFE";
        private String productRemoveAds = "com.melissa.melissaFE.premium";
        private String issuerId = "";
        private String keyId = "";
        private String privateKeyP8Base64 = "";
        private String environment = "SANDBOX";
    }
}
