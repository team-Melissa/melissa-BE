package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.ThreadConverter;
import com.melissa.diary.domain.Thread;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.CalenderResponseDTO;
import com.melissa.diary.domain.Thread;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalenderService {

    private final ThreadRepository threadRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CalenderResponseDTO.dailySummaryResponseDTO getDailySummary(Long userId, int year, int month, int day) {
        // 실제 등록된 유저인지 보호
        User user = getUser(userId);

        if (!isValidDate(year, month, day)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }

        Thread thread = threadRepository.findByUserIdAndYearAndMonthAndDay(userId, year, month, day)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND));

        if (!thread.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_FORBIDDEN);
        }

        return ThreadConverter.toDailySummaryResponseDTO(thread);
    }

    @Transactional(readOnly = true)
    public List<CalenderResponseDTO.dailyResponseDTO> getMonthlySummary(Long userId, int year, int month) {

        // 실제 등록된 유저인지 보호
        User user = getUser(userId);

        if (!isValidMonth(year, month)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }

        List<Thread> threads = threadRepository.findByUserIdAndYearAndMonth(userId, year, month);

        if (threads.isEmpty()) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND);
        }

        return threads.stream()
                .map(ThreadConverter::toDailyResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalenderResponseDTO.dailySummaryResponseDTO> getMonthlyView(Long userId, int year, int month) {

        // 실제 등록된 유저인지 보호
        User user = getUser(userId);

        if (!isValidMonth(year, month)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }

        List<Thread> threads = threadRepository.findByUserIdAndYearAndMonth(userId, year, month);

        if (threads.isEmpty()) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_NOT_FOUND);
        }

        return threads.stream()
                .map(ThreadConverter::toDailySummaryResponseDTO)
                .collect(Collectors.toList());
    }


    private boolean isValidDate(int year, int month, int day) {
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;

        // 2월 처리
        if (month == 2) {
            boolean isLeapYear = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
            return day <= (isLeapYear ? 29 : 28);
        }

        // 4, 6, 9, 11월은 30일까지
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return day <= 30;
        }

        return true;
    }
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        return userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
    }

    private boolean isValidMonth(int year, int month) {
        return month >= 1 && month <= 12;
    }
}