package com.melissa.diary.converter;

import com.melissa.diary.security.EncryptionManager;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
public class EncryptionAttributeConverter implements AttributeConverter<String, String> {

    // Bean으로 등록된 EncryptionManager 주입
    @Autowired
    private final EncryptionManager encryptionManager;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // null이면 그냥 null, 아니면 암호화하여 DB에 저장
        return (attribute == null) ? null : encryptionManager.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // null이면 그냥 null, 아니면 복호화하여 엔티티 필드에 세팅
        return (dbData == null) ? null : encryptionManager.decrypt(dbData);
    }
}