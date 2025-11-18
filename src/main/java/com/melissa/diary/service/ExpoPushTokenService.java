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
     * Expo Push Token 등록 (로그인 O)
     * - 이미 존재하는 토큰이면 사용자 정보만 업데이트
     * - 새 토큰이면 신규 등록
     */
    @Transactional
    public ExpoPushTokenResponseDTO.TokenResponse registerToken(
            Long userId, 
            ExpoPushTokenRequestDTO.RegisterTokenRequest request) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        
        String tokenValue = request.getExpoPushToken();
        
        // 이미 존재하는 토큰인지 확인
        Optional<ExpoPushToken> existingToken = expoPushTokenRepository.findByExpoPushToken(tokenValue);
        
        if (existingToken.isPresent()) {
            // 기존 토큰이 있으면 사용자 정보 업데이트
            ExpoPushToken token = existingToken.get();
            token.setUser(user);
            token.setPlatform(request.getPlatform());
            token.setDeviceId(request.getDeviceId());
            token.setInvalid(false); // 다시 활성화
            
            ExpoPushToken savedToken = expoPushTokenRepository.save(token);
            log.info("[ExpoPushToken] 기존 토큰 업데이트 완료. userId={}, tokenId={}", userId, savedToken.getId());
            
            return ExpoPushTokenConverter.toResponse(savedToken);
        } else {
            // 새 토큰 등록
            ExpoPushToken newToken = ExpoPushTokenConverter.toEntity(request, user);
            
            try {
                ExpoPushToken savedToken = expoPushTokenRepository.save(newToken);
                log.info("[ExpoPushToken] 새 토큰 등록 완료. userId={}, tokenId={}", userId, savedToken.getId());
                
                return ExpoPushTokenConverter.toResponse(savedToken);
            } catch (DataIntegrityViolationException e) {
                log.error("[ExpoPushToken] 토큰 등록 실패 (중복 가능성). userId={}, token={}", userId, tokenValue, e);
                throw new ErrorHandler(ErrorStatus.EXPO_TOKEN_ALREADY_EXISTS);
            }
        }
    }
    
    /**
     * 사용자별 토큰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ExpoPushTokenResponseDTO.TokenResponse> getUserTokens(Long userId) {
        List<ExpoPushToken> tokens = expoPushTokenRepository.findByUserId(userId);
        
        log.info("[ExpoPushToken] 토큰 목록 조회 완료. userId={}, count={}", userId, tokens.size());
        
        return tokens.stream()
                .map(ExpoPushTokenConverter::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 유효한 토큰만 조회 (배치 발송용)
     */
    @Transactional(readOnly = true)
    public List<ExpoPushToken> getAllValidTokens() {
        List<ExpoPushToken> tokens = expoPushTokenRepository.findByInvalidFalse();
        
        log.info("[ExpoPushToken] 유효한 토큰 전체 조회 완료. count={}", tokens.size());
        
        return tokens;
    }
    
    /**
     * 특정 토큰 삭제
     */
    @Transactional
    public ExpoPushTokenResponseDTO.DeleteResponse deleteToken(String expoPushToken) {
        ExpoPushToken token = expoPushTokenRepository.findByExpoPushToken(expoPushToken)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.EXPO_TOKEN_NOT_FOUND));
        
        expoPushTokenRepository.delete(token);
        
        log.info("[ExpoPushToken] 토큰 삭제 완료. tokenId={}, token={}", token.getId(), expoPushToken);
        
        return ExpoPushTokenConverter.toDeleteResponse(expoPushToken);
    }
}

