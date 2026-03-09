package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.ExpoPushTokenConverter;
import com.melissa.diary.domain.ExpoPushToken;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.ExpoPushTokenRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.ExpoPushTokenRequestDTO;
import com.melissa.diary.web.dto.ExpoPushTokenResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpoPushTokenService {

    private final ExpoPushTokenRepository expoPushTokenRepository;
    private final UserRepository userRepository;

    /**
     * Expo Push Token 등록/업데이트
     */
    @Transactional
    public ExpoPushTokenResponseDTO.TokenResponse registerToken(
            Long userId,
            ExpoPushTokenRequestDTO.RegisterTokenRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        String tokenValue = request.getExpoPushToken();

        Optional<ExpoPushToken> existingToken = expoPushTokenRepository.findByExpoPushToken(tokenValue);
        if (existingToken.isPresent()) {
            ExpoPushToken token = existingToken.get();
            token.setUser(user);
            token.setPlatform(request.getPlatform());
            token.setDeviceId(request.getDeviceId());
            token.markValid();

            ExpoPushToken savedToken = expoPushTokenRepository.save(token);
            log.info("[ExpoPushToken] updated existing token. userId={}, tokenId={}", userId, savedToken.getId());
            return ExpoPushTokenConverter.toResponse(savedToken);
        }

        ExpoPushToken newToken = ExpoPushTokenConverter.toEntity(request, user);
        try {
            ExpoPushToken savedToken = expoPushTokenRepository.save(newToken);
            log.info("[ExpoPushToken] registered new token. userId={}, tokenId={}", userId, savedToken.getId());
            return ExpoPushTokenConverter.toResponse(savedToken);
        } catch (DataIntegrityViolationException e) {
            log.error("[ExpoPushToken] token registration failed (likely duplicate). userId={}, token={}",
                    userId, tokenValue, e);
            throw new ErrorHandler(ErrorStatus.EXPO_TOKEN_ALREADY_EXISTS);
        }
    }

    /**
     * 사용자별 토큰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ExpoPushTokenResponseDTO.TokenResponse> getUserTokens(Long userId) {
        List<ExpoPushToken> tokens = expoPushTokenRepository.findByUserId(userId);
        log.info("[ExpoPushToken] fetched user tokens. userId={}, count={}", userId, tokens.size());

        return tokens.stream()
                .map(ExpoPushTokenConverter::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 유효한 토큰 전체 조회 (배치 발송용)
     */
    @Transactional(readOnly = true)
    public List<ExpoPushToken> getAllValidTokens() {
        List<ExpoPushToken> tokens = expoPushTokenRepository.findByInvalidFalse();
        log.info("[ExpoPushToken] fetched valid tokens. count={}", tokens.size());
        return tokens;
    }

    /**
     * Expo Push Token 값으로 토큰 삭제
     */
    @Transactional
    public ExpoPushTokenResponseDTO.DeleteResponse deleteToken(String expoPushToken) {
        ExpoPushToken token = expoPushTokenRepository.findByExpoPushToken(expoPushToken)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.EXPO_TOKEN_NOT_FOUND));

        expoPushTokenRepository.delete(token);
        log.info("[ExpoPushToken] deleted token. tokenId={}, token={}", token.getId(), expoPushToken);

        return ExpoPushTokenConverter.toDeleteResponse(expoPushToken);
    }
}
