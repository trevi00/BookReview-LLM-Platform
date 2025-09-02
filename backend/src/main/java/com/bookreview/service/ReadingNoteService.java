package com.bookreview.service;

import com.bookreview.domain.AiFeedback;
import com.bookreview.domain.Chapter;
import com.bookreview.domain.ReadingNote;
import com.bookreview.domain.UserBook;
import com.bookreview.domain.enums.FeedbackType;
import com.bookreview.domain.enums.NoteType;
import com.bookreview.dto.note.*;
import com.bookreview.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReadingNoteService {

    private static final Logger logger = LoggerFactory.getLogger(ReadingNoteService.class);

    @Autowired
    private ReadingNoteRepository readingNoteRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UserBookRepository userBookRepository;

    @Autowired
    private AiFeedbackRepository aiFeedbackRepository;

    @Autowired
    private RestTemplate restTemplate;

    public ReadingNoteResponse createReadingNote(Long userId, CreateReadingNoteRequest request) {
        // 챕터 존재 및 권한 확인
        Chapter chapter = chapterRepository.findById(request.getChapterId())
            .orElseThrow(() -> new RuntimeException("챕터를 찾을 수 없습니다: " + request.getChapterId()));

        if (!chapter.getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("챕터에 접근할 권한이 없습니다");
        }

        // 페이지 번호 유효성 검사 (챕터 범위 내)
        if (request.getPageNumber() != null) {
            validatePageNumber(chapter, request.getPageNumber());
        }

        ReadingNote note = ReadingNote.builder()
            .chapter(chapter)
            .content(request.getContent())
            .noteType(request.getNoteType())
            .pageNumber(request.getPageNumber())
            .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
            .build();

        note = readingNoteRepository.save(note);
        
        logger.info("Reading note created: {} for chapter {}", note.getId(), request.getChapterId());
        
        return convertToResponse(note);
    }

    @Transactional(readOnly = true)
    public ReadingNoteResponse getReadingNote(Long userId, Long noteId) {
        ReadingNote note = readingNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("독서 노트를 찾을 수 없습니다: " + noteId));

        // 권한 확인
        if (!note.getChapter().getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("독서 노트에 접근할 권한이 없습니다");
        }

        return convertToResponse(note);
    }

    public ReadingNoteResponse updateReadingNote(Long userId, Long noteId, UpdateReadingNoteRequest request) {
        ReadingNote note = readingNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("독서 노트를 찾을 수 없습니다: " + noteId));

        // 권한 확인
        if (!note.getChapter().getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("독서 노트를 수정할 권한이 없습니다");
        }

        // 페이지 번호 유효성 검사
        if (request.getPageNumber() != null) {
            validatePageNumber(note.getChapter(), request.getPageNumber());
        }

        // 필드별 업데이트
        if (StringUtils.hasText(request.getContent())) {
            note.updateContent(request.getContent());
        }
        if (request.getNoteType() != null) {
            note.updateNoteType(request.getNoteType());
        }
        if (request.getPageNumber() != null) {
            note.updatePageNumber(request.getPageNumber());
        }
        if (request.getIsPrivate() != null) {
            note.updatePrivate(request.getIsPrivate());
        }

        note = readingNoteRepository.save(note);
        
        logger.info("Reading note updated: {}", noteId);
        
        return convertToResponse(note);
    }

    public void deleteReadingNote(Long userId, Long noteId) {
        ReadingNote note = readingNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("독서 노트를 찾을 수 없습니다: " + noteId));

        // 권한 확인
        if (!note.getChapter().getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("독서 노트를 삭제할 권한이 없습니다");
        }

        // 관련 AI 피드백 삭제
        aiFeedbackRepository.deleteByReadingNoteId(noteId);

        readingNoteRepository.delete(note);
        
        logger.info("Reading note deleted: {}", noteId);
    }

    @Transactional(readOnly = true)
    public List<ReadingNoteResponse> getNotesByChapter(Long userId, Long chapterId) {
        // 권한 확인
        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new RuntimeException("챕터를 찾을 수 없습니다: " + chapterId));

        if (!chapter.getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("챕터에 접근할 권한이 없습니다");
        }

        List<ReadingNote> notes = readingNoteRepository.findByChapterIdOrderByPageNumberAscCreatedAtAsc(chapterId);
        
        return notes.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReadingNoteResponse> getNotesByUserBook(Long userId, Long userBookId) {
        // 권한 확인
        UserBook userBook = userBookRepository.findByIdAndUserId(userBookId, userId)
            .orElseThrow(() -> new RuntimeException("사용자 책을 찾을 수 없습니다"));

        List<ReadingNote> notes = readingNoteRepository.findByUserBookId(userBookId);
        
        return notes.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ReadingNoteResponse> searchReadingNotes(Long userId, ReadingNoteSearchRequest searchRequest) {
        Specification<ReadingNote> spec = createSearchSpecification(userId, searchRequest);
        
        // 정렬 설정
        Sort sort = createSort(searchRequest.getSortBy(), searchRequest.getSortDirection());
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
        
        Page<ReadingNote> notePage = readingNoteRepository.findAll(spec, pageable);
        
        return notePage.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public ReadingNoteStatisticsResponse getReadingNoteStatistics(Long userId) {
        // 전체 노트 수
        long totalNotes = readingNoteRepository.countByUserId(userId);
        
        // 타입별 노트 수
        Map<NoteType, Long> notesByType = Arrays.stream(NoteType.values())
            .collect(Collectors.toMap(
                type -> type,
                type -> readingNoteRepository.countByUserIdAndNoteType(userId, type)
            ));
        
        // 최근 7일간 노트 수
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        long notesThisWeek = readingNoteRepository.countByUserIdAndCreatedAtAfter(userId, weekAgo);
        
        // 평균 노트 길이
        Double avgNoteLength = readingNoteRepository.findAverageNoteLengthByUserId(userId);
        
        // AI 피드백 통계
        long totalFeedbacks = aiFeedbackRepository.countByUserId(userId);
        long feedbacksThisWeek = aiFeedbackRepository.countByUserIdAndCreatedAtAfter(userId, weekAgo);
        
        return ReadingNoteStatisticsResponse.builder()
            .totalNotes(totalNotes)
            .notesThisWeek(notesThisWeek)
            .totalFeedbacks(totalFeedbacks)
            .feedbacksThisWeek(feedbacksThisWeek)
            .averageNoteLength(avgNoteLength != null ? avgNoteLength : 0.0)
            .notesByType(notesByType.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> entry.getKey().name(), 
                    Map.Entry::getValue
                )))
            .build();
    }

    public AiFeedbackResponse requestAiFeedback(Long userId, Long noteId, AiFeedbackRequest request) {
        ReadingNote note = readingNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("독서 노트를 찾을 수 없습니다: " + noteId));

        // 권한 확인
        if (!note.getChapter().getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("독서 노트에 접근할 권한이 없습니다");
        }

        try {
            // AI 서비스 호출
            String aiServiceResponse = callAiService(note, request);
            
            // AI 피드백 저장
            AiFeedback feedback = AiFeedback.builder()
                .readingNote(note)
                .feedbackType(request.getFeedbackType())
                .content(aiServiceResponse)
                .aiModel("gpt-4")
                .confidence(0.85)
                .build();

            feedback = aiFeedbackRepository.save(feedback);
            
            logger.info("AI feedback created: {} for note {}", feedback.getId(), noteId);
            
            return convertToFeedbackResponse(feedback);
            
        } catch (Exception e) {
            logger.error("AI 피드백 생성 실패", e);
            throw new RuntimeException("AI 피드백 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<AiFeedbackResponse> getAiFeedbacks(Long userId, Long noteId) {
        ReadingNote note = readingNoteRepository.findById(noteId)
            .orElseThrow(() -> new RuntimeException("독서 노트를 찾을 수 없습니다: " + noteId));

        // 권한 확인
        if (!note.getChapter().getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("독서 노트에 접근할 권한이 없습니다");
        }

        List<AiFeedback> feedbacks = aiFeedbackRepository.findByReadingNoteIdOrderByCreatedAtDesc(noteId);
        
        return feedbacks.stream()
            .map(this::convertToFeedbackResponse)
            .collect(Collectors.toList());
    }

    private void validatePageNumber(Chapter chapter, Integer pageNumber) {
        if (pageNumber < chapter.getStartPage() || pageNumber > chapter.getEndPage()) {
            throw new RuntimeException(
                String.format("페이지 번호는 %d-%d 범위 내여야 합니다", 
                    chapter.getStartPage(), chapter.getEndPage())
            );
        }
    }

    private Specification<ReadingNote> createSearchSpecification(Long userId, ReadingNoteSearchRequest request) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 사용자 필터 (필수)
            predicates.add(builder.equal(root.get("chapter").get("userBook").get("user").get("id"), userId));
            
            // 챕터 필터
            if (request.getChapterId() != null) {
                predicates.add(builder.equal(root.get("chapter").get("id"), request.getChapterId()));
            }
            
            // 사용자 책 필터
            if (request.getUserBookId() != null) {
                predicates.add(builder.equal(root.get("chapter").get("userBook").get("id"), request.getUserBookId()));
            }
            
            // 내용 검색
            if (StringUtils.hasText(request.getContent())) {
                predicates.add(builder.like(
                    builder.lower(root.get("content")), 
                    "%" + request.getContent().toLowerCase() + "%"
                ));
            }
            
            // 노트 타입 필터
            if (request.getNoteType() != null) {
                predicates.add(builder.equal(root.get("noteType"), request.getNoteType()));
            }
            
            // 페이지 범위 필터
            if (request.getMinPage() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("pageNumber"), request.getMinPage()));
            }
            if (request.getMaxPage() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("pageNumber"), request.getMaxPage()));
            }
            
            // 공개/비공개 필터
            if (request.getIsPrivate() != null) {
                predicates.add(builder.equal(root.get("isPrivate"), request.getIsPrivate()));
            }
            
            // 피드백 존재 여부 필터
            if (request.getHasFeedbacks() != null) {
                if (request.getHasFeedbacks()) {
                    predicates.add(builder.isNotEmpty(root.get("aiFeedbacks")));
                } else {
                    predicates.add(builder.isEmpty(root.get("aiFeedbacks")));
                }
            }
            
            // 날짜 필터
            if (request.getCreatedDateFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), request.getCreatedDateFrom()));
            }
            if (request.getCreatedDateTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), request.getCreatedDateTo()));
            }
            
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        
        return switch (sortBy) {
            case "pageNumber" -> Sort.by(direction, "pageNumber", "createdAt");
            case "noteType" -> Sort.by(direction, "noteType", "createdAt");
            case "updatedAt" -> Sort.by(direction, "updatedAt");
            default -> Sort.by(direction, "createdAt");
        };
    }

    private String callAiService(ReadingNote note, AiFeedbackRequest request) {
        try {
            String aiServiceUrl = "http://localhost:8001/api/v1/feedback/generate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-User-ID", note.getChapter().getUserBook().getUser().getId().toString());
            
            // AI 서비스 요청 구성
            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("note_id", note.getId());
            aiRequest.put("feedback_type", request.getFeedbackType().name().toLowerCase());
            
            // 책 정보
            Map<String, Object> bookContext = new HashMap<>();
            bookContext.put("title", note.getChapter().getUserBook().getBook().getTitle());
            bookContext.put("author", note.getChapter().getUserBook().getBook().getAuthor());
            bookContext.put("category", note.getChapter().getUserBook().getBook().getCategory().name().toLowerCase());
            aiRequest.put("book_context", bookContext);
            
            // 챕터 정보
            Map<String, Object> chapterContext = new HashMap<>();
            chapterContext.put("chapter_number", note.getChapter().getChapterNumber());
            chapterContext.put("title", note.getChapter().getTitle());
            aiRequest.put("chapter_context", chapterContext);
            
            // 노트 정보
            Map<String, Object> noteContext = new HashMap<>();
            noteContext.put("content", note.getContent());
            noteContext.put("note_type", note.getNoteType().name().toLowerCase());
            noteContext.put("page_number", note.getPageNumber());
            aiRequest.put("note_context", noteContext);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(aiRequest, headers);
            
            // AI 서비스 호출
            Map<String, Object> response = restTemplate.postForObject(aiServiceUrl, entity, Map.class);
            
            if (response != null && response.containsKey("content")) {
                return response.get("content").toString();
            } else {
                throw new RuntimeException("AI 서비스 응답이 올바르지 않습니다");
            }
            
        } catch (Exception e) {
            logger.error("AI 서비스 호출 실패", e);
            throw new RuntimeException("AI 서비스 연결에 실패했습니다: " + e.getMessage());
        }
    }

    private ReadingNoteResponse convertToResponse(ReadingNote note) {
        // AI 피드백 개수 조회
        long feedbackCount = aiFeedbackRepository.countByReadingNoteId(note.getId());
        
        return ReadingNoteResponse.builder()
            .id(note.getId())
            .chapterId(note.getChapter().getId())
            .content(note.getContent())
            .noteType(note.getNoteType())
            .pageNumber(note.getPageNumber())
            .isPrivate(note.isPrivate())
            .createdAt(note.getCreatedAt())
            .updatedAt(note.getUpdatedAt())
            .feedbackCount(feedbackCount)
            .hasFeedback(feedbackCount > 0)
            // 챕터 정보 포함
            .chapterNumber(note.getChapter().getChapterNumber())
            .chapterTitle(note.getChapter().getTitle())
            // 책 정보 포함
            .bookTitle(note.getChapter().getUserBook().getBook().getTitle())
            .bookAuthor(note.getChapter().getUserBook().getBook().getAuthor())
            .build();
    }

    private AiFeedbackResponse convertToFeedbackResponse(AiFeedback feedback) {
        return AiFeedbackResponse.builder()
            .id(feedback.getId())
            .noteId(feedback.getReadingNote().getId())
            .feedbackType(feedback.getFeedbackType())
            .content(feedback.getContent())
            .aiModel(feedback.getAiModel())
            .confidence(feedback.getConfidence())
            .createdAt(feedback.getCreatedAt())
            .build();
    }
}