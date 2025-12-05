package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.DiaryConverter;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.DiaryRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.CalendarResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * [v1.3.0] Diary 기반 달력 서비스
 * - 하루에 여러 일기 지원 (최대 3개)
 * - 날짜별 그룹화 및 정렬
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    /**
     * 특정 날짜의 일기 상세 조회 (최대 3개)
     */
    @Transactional(readOnly = true)
    public CalendarResponseDTO.DailySummaryResponseDTO getDailySummary(Long userId, int year, int month, int day) {
        // 유저 검증
        getUser(userId);

        if (!isValidDate(year, month, day)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }

        // 해당 날짜의 활성화된 일기 조회 (최대 3개)
        List<Diary> diaries = diaryRepository.findAllByUserIdAndYearAndMonthAndDayAndIsActiveOrderByCreatedAtDesc(
                userId, year, month, day, true);

        // 일기가 없어도 빈 배열로 반환 (에러 발생하지 않음)
        List<CalendarResponseDTO.DiaryDetailDTO> diaryDetails = diaries.stream()
                .limit(3) // 최대 3개로 제한
                .map(DiaryConverter::toDiaryDetailDTO)
                .collect(Collectors.toList());

        return CalendarResponseDTO.DailySummaryResponseDTO.builder()
                .year(year)
                .month(month)
                .day(day)
                .diaries(diaryDetails)
                .build();
    }

    /**
     * 월간 미리보기 조회 (날짜별 그룹화)
     */
    @Transactional(readOnly = true)
    public List<CalendarResponseDTO.DailyPreviewResponseDTO> getMonthlySummary(Long userId, int year, int month) {
        // 유저 검증
        getUser(userId);

        if (!isValidMonth(year, month)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }

        // 해당 월의 모든 활성화된 일기 조회
        List<Diary> diaries = diaryRepository.findAllByUserIdAndYearAndMonthAndIsActiveOrderByDayAscCreatedAtDesc(
                userId, year, month, true);

        // 날짜별로 그룹화 (day를 키로 사용)
        Map<Integer, List<Diary>> diariesByDay = diaries.stream()
                .collect(Collectors.groupingBy(
                        Diary::getDay,
                        LinkedHashMap::new, // 순서 유지
                        Collectors.toList()
                ));

        // 각 날짜별로 DailyPreviewResponseDTO 생성
        return diariesByDay.entrySet().stream()
                .map(entry -> {
                    int day = entry.getKey();
                    List<Diary> dayDiaries = entry.getValue();
                    
                    // 최대 3개로 제한하고 DiaryPreviewDTO로 변환
                    List<CalendarResponseDTO.DiaryPreviewDTO> previews = dayDiaries.stream()
                            .limit(3)
                            .map(DiaryConverter::toDiaryPreviewDTO)
                            .collect(Collectors.toList());

                    return CalendarResponseDTO.DailyPreviewResponseDTO.builder()
                            .year(year)
                            .month(month)
                            .day(day)
                            .diaries(previews)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 월간 전체 조회 (날짜별 상세 정보 포함)
     */
    @Transactional(readOnly = true)
    public List<CalendarResponseDTO.DailySummaryResponseDTO> getMonthlyView(Long userId, int year, int month) {
        // 유저 검증
        getUser(userId);

        if (!isValidMonth(year, month)) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_DATE);
        }

        // 해당 월의 모든 활성화된 일기 조회
        List<Diary> diaries = diaryRepository.findAllByUserIdAndYearAndMonthAndIsActiveOrderByDayAscCreatedAtDesc(
                userId, year, month, true);

        // 날짜별로 그룹화
        Map<Integer, List<Diary>> diariesByDay = diaries.stream()
                .collect(Collectors.groupingBy(
                        Diary::getDay,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 각 날짜별로 DailySummaryResponseDTO 생성
        return diariesByDay.entrySet().stream()
                .map(entry -> {
                    int day = entry.getKey();
                    List<Diary> dayDiaries = entry.getValue();
                    
                    // 최대 3개로 제한하고 DiaryDetailDTO로 변환
                    List<CalendarResponseDTO.DiaryDetailDTO> details = dayDiaries.stream()
                            .limit(3)
                            .map(DiaryConverter::toDiaryDetailDTO)
                            .collect(Collectors.toList());

                    return CalendarResponseDTO.DailySummaryResponseDTO.builder()
                            .year(year)
                            .month(month)
                            .day(day)
                            .diaries(details)
                            .build();
                })
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
        return userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
    }

    private boolean isValidMonth(int year, int month) {
        return month >= 1 && month <= 12;
    }
}