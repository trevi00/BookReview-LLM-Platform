package com.bookreview.service;

import com.bookreview.domain.*;
import com.bookreview.domain.enums.BookCategory;
import com.bookreview.domain.enums.ReadingStatus;
import com.bookreview.dto.statistics.*;
import com.bookreview.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReadingStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(ReadingStatisticsService.class);

    @Autowired
    private UserBookRepository userBookRepository;

    @Autowired
    private ReadingNoteRepository readingNoteRepository;

    @Autowired
    private ReadingGoalRepository readingGoalRepository;

    @Autowired
    private ReadingSessionRepository readingSessionRepository;

    @Autowired
    private AiFeedbackRepository aiFeedbackRepository;

    @Transactional(readOnly = true)
    public ReadingDashboardResponse getReadingDashboard(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thisYearStart = LocalDateTime.of(now.getYear(), 1, 1, 0, 0);
        LocalDateTime thisMonthStart = LocalDateTime.of(now.getYear(), now.getMonth(), 1, 0, 0);
        LocalDateTime thisWeekStart = now.minusDays(7);

        // 올해 통계
        long booksReadThisYear = userBookRepository.countByUserIdAndStatusAndCompletedAtAfter(
            userId, ReadingStatus.COMPLETED, thisYearStart);
        long notesThisYear = readingNoteRepository.countByUserIdAndCreatedAtAfter(userId, thisYearStart);
        
        // 이번 달 통계
        long booksReadThisMonth = userBookRepository.countByUserIdAndStatusAndCompletedAtAfter(
            userId, ReadingStatus.COMPLETED, thisMonthStart);
        long notesThisMonth = readingNoteRepository.countByUserIdAndCreatedAtAfter(userId, thisMonthStart);
        
        // 이번 주 통계
        long booksReadThisWeek = userBookRepository.countByUserIdAndStatusAndCompletedAtAfter(
            userId, ReadingStatus.COMPLETED, thisWeekStart);
        long notesThisWeek = readingNoteRepository.countByUserIdAndCreatedAtAfter(userId, thisWeekStart);

        // 현재 읽고 있는 책
        long currentlyReading = userBookRepository.countByUserIdAndStatus(userId, ReadingStatus.READING);
        
        // 총 독서 시간 (분)
        Long totalReadingMinutes = readingSessionRepository.sumReadingTimeByUserId(userId);
        
        // AI 피드백 통계
        long totalFeedbacks = aiFeedbackRepository.countByUserId(userId);
        
        // 연속 독서일
        int currentStreak = calculateCurrentStreak(userId);
        
        // 현재 년도의 목표 진척률
        ReadingGoal currentYearGoal = readingGoalRepository.findByUserIdAndYear(userId, now.getYear());
        double goalProgress = 0.0;
        if (currentYearGoal != null) {
            goalProgress = (double) booksReadThisYear / currentYearGoal.getTargetBooks() * 100;
        }

        return ReadingDashboardResponse.builder()
            .booksReadThisYear((int) booksReadThisYear)
            .booksReadThisMonth((int) booksReadThisMonth)
            .booksReadThisWeek((int) booksReadThisWeek)
            .currentlyReading((int) currentlyReading)
            .notesThisYear((int) notesThisYear)
            .notesThisMonth((int) notesThisMonth)
            .notesThisWeek((int) notesThisWeek)
            .totalReadingHours(totalReadingMinutes != null ? totalReadingMinutes / 60.0 : 0.0)
            .totalFeedbacks((int) totalFeedbacks)
            .currentStreak(currentStreak)
            .goalProgress(Math.min(goalProgress, 100.0))
            .build();
    }

    @Transactional(readOnly = true)
    public List<MonthlyReadingStats> getMonthlyStatistics(Long userId, int months) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months - 1).withDayOfMonth(1);

        List<MonthlyReadingStats> monthlyStats = new ArrayList<>();
        
        for (int i = 0; i < months; i++) {
            YearMonth yearMonth = YearMonth.from(startDate.plusMonths(i));
            LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            
            long booksRead = userBookRepository.countByUserIdAndStatusAndCompletedAtBetween(
                userId, ReadingStatus.COMPLETED, monthStart, monthEnd);
            long notesWritten = readingNoteRepository.countByUserIdAndCreatedAtBetween(userId, monthStart, monthEnd);
            Long readingMinutes = readingSessionRepository.sumReadingTimeByUserIdAndSessionDateBetween(
                userId, monthStart.toLocalDate(), monthEnd.toLocalDate());
            
            MonthlyReadingStats stats = MonthlyReadingStats.builder()
                .year(yearMonth.getYear())
                .month(yearMonth.getMonthValue())
                .booksRead((int) booksRead)
                .notesWritten((int) notesWritten)
                .readingHours(readingMinutes != null ? readingMinutes / 60.0 : 0.0)
                .build();
            
            monthlyStats.add(stats);
        }
        
        return monthlyStats;
    }

    @Transactional(readOnly = true)
    public List<CategoryReadingStats> getCategoryStatistics(Long userId) {
        List<Object[]> categoryStats = userBookRepository.findCategoryStatsByUserId(userId);
        
        return categoryStats.stream()
            .map(row -> CategoryReadingStats.builder()
                .category((BookCategory) row[0])
                .booksRead(((Number) row[1]).intValue())
                .totalPages(((Number) row[2]).intValue())
                .averageRating(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                .build())
            .collect(Collectors.toList());
    }

    public ReadingGoalResponse createReadingGoal(Long userId, CreateReadingGoalRequest request) {
        // 같은 연도의 기존 목표 확인
        ReadingGoal existingGoal = readingGoalRepository.findByUserIdAndYear(userId, request.getYear());
        if (existingGoal != null) {
            throw new RuntimeException("해당 연도의 독서 목표가 이미 존재합니다: " + request.getYear());
        }

        ReadingGoal goal = ReadingGoal.builder()
            .userId(userId)
            .year(request.getYear())
            .targetBooks(request.getTargetBooks())
            .description(request.getDescription())
            .build();

        goal = readingGoalRepository.save(goal);
        
        logger.info("Reading goal created: {} for user {}", goal.getId(), userId);
        
        return convertToGoalResponse(goal);
    }

    @Transactional(readOnly = true)
    public List<ReadingGoalResponse> getReadingGoals(Long userId, Integer year) {
        List<ReadingGoal> goals;
        
        if (year != null) {
            ReadingGoal goal = readingGoalRepository.findByUserIdAndYear(userId, year);
            goals = goal != null ? Arrays.asList(goal) : Collections.emptyList();
        } else {
            goals = readingGoalRepository.findByUserIdOrderByYearDesc(userId);
        }
        
        return goals.stream()
            .map(goal -> {
                ReadingGoalResponse response = convertToGoalResponse(goal);
                
                // 진척률 계산
                LocalDateTime yearStart = LocalDateTime.of(goal.getYear(), 1, 1, 0, 0);
                LocalDateTime yearEnd = LocalDateTime.of(goal.getYear(), 12, 31, 23, 59);
                long completedBooks = userBookRepository.countByUserIdAndStatusAndCompletedAtBetween(
                    userId, ReadingStatus.COMPLETED, yearStart, yearEnd);
                
                response.setAchievedBooks((int) completedBooks);
                response.setProgressPercentage(
                    goal.getTargetBooks() > 0 ? 
                    (double) completedBooks / goal.getTargetBooks() * 100 : 0.0);
                
                return response;
            })
            .collect(Collectors.toList());
    }

    public ReadingGoalResponse updateReadingGoal(Long userId, Long goalId, UpdateReadingGoalRequest request) {
        ReadingGoal goal = readingGoalRepository.findByIdAndUserId(goalId, userId)
            .orElseThrow(() -> new RuntimeException("독서 목표를 찾을 수 없습니다: " + goalId));

        if (request.getTargetBooks() != null) {
            goal.updateTargetBooks(request.getTargetBooks());
        }
        if (request.getDescription() != null) {
            goal.updateDescription(request.getDescription());
        }

        goal = readingGoalRepository.save(goal);
        
        logger.info("Reading goal updated: {}", goalId);
        
        return convertToGoalResponse(goal);
    }

    public void deleteReadingGoal(Long userId, Long goalId) {
        ReadingGoal goal = readingGoalRepository.findByIdAndUserId(goalId, userId)
            .orElseThrow(() -> new RuntimeException("독서 목표를 찾을 수 없습니다: " + goalId));

        readingGoalRepository.delete(goal);
        
        logger.info("Reading goal deleted: {}", goalId);
    }

    @Transactional(readOnly = true)
    public ReadingProgressResponse getReadingProgress(Long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        LocalDateTime yearStart = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(year, 12, 31, 23, 59);
        
        long completedBooks = userBookRepository.countByUserIdAndStatusAndCompletedAtBetween(
            userId, ReadingStatus.COMPLETED, yearStart, yearEnd);
        long currentlyReading = userBookRepository.countByUserIdAndStatus(userId, ReadingStatus.READING);
        long totalNotes = readingNoteRepository.countByUserIdAndCreatedAtBetween(userId, yearStart, yearEnd);
        
        // 목표 대비 진척률
        ReadingGoal goal = readingGoalRepository.findByUserIdAndYear(userId, year);
        double goalProgress = 0.0;
        int targetBooks = 0;
        
        if (goal != null) {
            targetBooks = goal.getTargetBooks();
            goalProgress = targetBooks > 0 ? (double) completedBooks / targetBooks * 100 : 0.0;
        }
        
        // 월별 진척률
        List<MonthlyProgress> monthlyProgress = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            long monthlyBooks = userBookRepository.countByUserIdAndStatusAndCompletedAtBetween(
                userId, ReadingStatus.COMPLETED, yearStart, monthEnd);
            
            monthlyProgress.add(MonthlyProgress.builder()
                .month(month)
                .cumulativeBooks((int) monthlyBooks)
                .build());
        }

        return ReadingProgressResponse.builder()
            .year(year)
            .targetBooks(targetBooks)
            .completedBooks((int) completedBooks)
            .currentlyReading((int) currentlyReading)
            .totalNotes((int) totalNotes)
            .goalProgressPercentage(Math.min(goalProgress, 100.0))
            .monthlyProgress(monthlyProgress)
            .build();
    }

    @Transactional(readOnly = true)
    public ReadingStreakResponse getReadingStreak(Long userId) {
        int currentStreak = calculateCurrentStreak(userId);
        int longestStreak = calculateLongestStreak(userId);
        
        // 최근 독서일
        LocalDate lastReadingDate = readingSessionRepository.findLastReadingDateByUserId(userId);
        
        return ReadingStreakResponse.builder()
            .currentStreak(currentStreak)
            .longestStreak(longestStreak)
            .lastReadingDate(lastReadingDate)
            .build();
    }

    public ReadingSessionResponse recordReadingSession(Long userId, CreateReadingSessionRequest request) {
        // 사용자 책 권한 확인
        UserBook userBook = userBookRepository.findByIdAndUserId(request.getUserBookId(), userId)
            .orElseThrow(() -> new RuntimeException("사용자 책을 찾을 수 없습니다"));

        ReadingSession session = ReadingSession.builder()
            .userBook(userBook)
            .sessionDate(request.getSessionDate() != null ? request.getSessionDate() : LocalDate.now())
            .readingTimeMinutes(request.getReadingTimeMinutes())
            .startPage(request.getStartPage())
            .endPage(request.getEndPage())
            .notes(request.getNotes())
            .build();

        session = readingSessionRepository.save(session);
        
        logger.info("Reading session recorded: {} for user book {}", session.getId(), request.getUserBookId());
        
        return convertToSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<ReadingSessionResponse> getReadingSessions(Long userId, Long userBookId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        List<ReadingSession> sessions;
        
        if (userBookId != null) {
            // 특정 책의 세션들
            UserBook userBook = userBookRepository.findByIdAndUserId(userBookId, userId)
                .orElseThrow(() -> new RuntimeException("사용자 책을 찾을 수 없습니다"));
            sessions = readingSessionRepository.findByUserBookIdAndSessionDateBetweenOrderBySessionDateDesc(
                userBookId, startDate, endDate);
        } else {
            // 사용자의 모든 세션들
            sessions = readingSessionRepository.findByUserIdAndSessionDateBetweenOrderBySessionDateDesc(
                userId, startDate, endDate);
        }
        
        return sessions.stream()
            .map(this::convertToSessionResponse)
            .collect(Collectors.toList());
    }

    private int calculateCurrentStreak(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate checkDate = today;
        int streak = 0;
        
        // 오늘부터 거슬러 올라가면서 연속일 계산
        while (true) {
            boolean hasReadingActivity = readingSessionRepository.existsByUserIdAndSessionDate(userId, checkDate) ||
                readingNoteRepository.existsByUserIdAndCreatedAtBetween(
                    userId, 
                    checkDate.atStartOfDay(), 
                    checkDate.atTime(23, 59, 59));
            
            if (hasReadingActivity) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                // 오늘 활동이 없으면 어제까지 확인
                if (checkDate.equals(today)) {
                    checkDate = checkDate.minusDays(1);
                    continue;
                }
                break;
            }
            
            // 최대 365일까지만 확인
            if (streak >= 365) break;
        }
        
        return streak;
    }

    private int calculateLongestStreak(Long userId) {
        // 간단한 구현 - 실제로는 더 정교한 알고리즘 필요
        return calculateCurrentStreak(userId);
    }

    private ReadingGoalResponse convertToGoalResponse(ReadingGoal goal) {
        return ReadingGoalResponse.builder()
            .id(goal.getId())
            .year(goal.getYear())
            .targetBooks(goal.getTargetBooks())
            .description(goal.getDescription())
            .achievedBooks(0) // 호출하는 곳에서 설정
            .progressPercentage(0.0) // 호출하는 곳에서 설정
            .createdAt(goal.getCreatedAt())
            .updatedAt(goal.getUpdatedAt())
            .build();
    }

    private ReadingSessionResponse convertToSessionResponse(ReadingSession session) {
        return ReadingSessionResponse.builder()
            .id(session.getId())
            .userBookId(session.getUserBook().getId())
            .bookTitle(session.getUserBook().getBook().getTitle())
            .sessionDate(session.getSessionDate())
            .readingTimeMinutes(session.getReadingTimeMinutes())
            .startPage(session.getStartPage())
            .endPage(session.getEndPage())
            .pagesRead(session.getEndPage() != null && session.getStartPage() != null ? 
                session.getEndPage() - session.getStartPage() + 1 : 0)
            .notes(session.getNotes())
            .createdAt(session.getCreatedAt())
            .build();
    }
}