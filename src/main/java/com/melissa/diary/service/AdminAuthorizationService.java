package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.AccountRole;
import com.melissa.diary.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public void assertAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        if (user.getAccountRole() != AccountRole.ADMIN) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_ADMIN_REQUIRED);
        }
    }
}
