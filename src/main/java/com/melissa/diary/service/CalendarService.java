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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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

    /**
     * 최신순 피드 조회 (커서 기반 무한 페이징)
     * - 날짜 묶음 N개를 보장 (일기 개수가 아닌 고유 날짜 개수 기준)
     * - 정렬: id DESC (AUTO_INCREMENT로 최신순 보장)
     */
    @Transactional(readOnly = true)
    public CalendarResponseDTO.FeedResponseDTO getFeed(Long userId, Integer limit, Long cursorDiaryId) {
        // 유저 검증
        getUser(userId);

        // limit은 날짜 묶음 개수 (기본 20개, 최대 50개)
        int resolvedLimit = (limit == null ? 20 : limit);
        if (resolvedLimit < 1 || resolvedLimit > 50) {
            throw new ErrorHandler(ErrorStatus.CALENDAR_INVALID_LIMIT);
        }

        // 날짜 묶음을 저장할 자료구조 (순서 유지)
        Map<String, List<CalendarResponseDTO.DiaryDetailDTO>> byDayKey = new LinkedHashMap<>();
        Map<String, int[]> dayParts = new LinkedHashMap<>(); // year, month, day 저장
        
        Long currentCursor = cursorDiaryId;
        boolean hasMore = true;
        
        // 날짜 묶음이 resolvedLimit+1개가 될 때까지 반복 조회 (+1은 hasNext 판단용)
        while (byDayKey.size() <= resolvedLimit && hasMore) {
            // 한 번에 가져올 일기 개수 (배치 사이즈)
            // 날짜당 평균 1.5개 일기를 가정하여, 부족한 날짜 수 * 2
            int remainingDays = (resolvedLimit + 1) - byDayKey.size();
            int batchSize = Math.max(remainingDays * 2, 20);
            
            List<Diary> batch = diaryRepository.findFeedPage(
                    userId,
                    currentCursor,
                    PageRequest.of(0, batchSize)
            );
            
            if (batch.isEmpty()) {
                hasMore = false;
                break;
            }
            
            // 배치 데이터를 날짜별로 그룹화
            for (Diary diary : batch) {
                String key = diary.getYear() + "-" + diary.getMonth() + "-" + diary.getDay();
                
                // 이미 목표 날짜 수(+1)에 도달했으면 중단
                if (byDayKey.size() >= resolvedLimit + 1 && !byDayKey.containsKey(key)) {
                    hasMore = true;
                    break;
                }
                
                List<CalendarResponseDTO.DiaryDetailDTO> diaryList = byDayKey.computeIfAbsent(key, k -> new ArrayList<>());
                
                // 각 날짜별 최대 3개까지만 추가
                if (diaryList.size() < 3) {
                    diaryList.add(DiaryConverter.toDiaryDetailDTO(diary));
                    dayParts.putIfAbsent(key, new int[]{diary.getYear(), diary.getMonth(), diary.getDay()});
                }
                
                currentCursor = diary.getId(); // 다음 조회를 위한 커서 업데이트
            }
            
            // 배치 크기보다 적게 조회되었다면 더 이상 데이터가 없음
            if (batch.size() < batchSize) {
                hasMore = false;
            }
        }

        // hasNext 판단 및 실제 반환할 날짜 목록 결정
        boolean hasNext = byDayKey.size() > resolvedLimit;
        List<String> dayKeys = new ArrayList<>(byDayKey.keySet());
        
        if (hasNext) {
            // 초과된 날짜 제거
            dayKeys = dayKeys.subList(0, resolvedLimit);
        }
        
        // 응답 DTO 생성
        List<CalendarResponseDTO.DailySummaryResponseDTO> days = dayKeys.stream()
                .map(key -> {
                    int[] parts = dayParts.get(key);
                    return CalendarResponseDTO.DailySummaryResponseDTO.builder()
                            .year(parts[0])
                            .month(parts[1])
                            .day(parts[2])
                            .diaries(byDayKey.get(key))
                            .build();
                })
                .collect(Collectors.toList());

        // 다음 커서 계산: 마지막 날짜의 마지막 일기 ID
        CalendarResponseDTO.FeedNextCursorDTO nextCursor = null;
        if (hasNext && !days.isEmpty()) {
            List<CalendarResponseDTO.DiaryDetailDTO> lastDayDiaries = days.get(days.size() - 1).getDiaries();
            if (!lastDayDiaries.isEmpty()) {
                Long lastDiaryId = lastDayDiaries.get(lastDayDiaries.size() - 1).getDiaryId();
                nextCursor = CalendarResponseDTO.FeedNextCursorDTO.builder()
                        .cursorDiaryId(lastDiaryId)
                        .build();
            }
        }

        return CalendarResponseDTO.FeedResponseDTO.builder()
                .days(days)
                .pageInfo(CalendarResponseDTO.FeedPageInfoDTO.builder()
                        .hasNext(hasNext)
                        .nextCursor(nextCursor)
                        .build())
                .build();
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
    public User getUser(@NonNull Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
    }

    private boolean isValidMonth(int year, int month) {
        return month >= 1 && month <= 12;
    }
}