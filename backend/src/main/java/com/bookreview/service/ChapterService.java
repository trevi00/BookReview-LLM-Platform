package com.bookreview.service;

import com.bookreview.domain.Chapter;
import com.bookreview.domain.UserBook;
import com.bookreview.dto.chapter.*;
import com.bookreview.repository.ChapterRepository;
import com.bookreview.repository.UserBookRepository;
import com.bookreview.repository.ReadingNoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChapterService {

    private static final Logger logger = LoggerFactory.getLogger(ChapterService.class);

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UserBookRepository userBookRepository;

    @Autowired
    private ReadingNoteRepository readingNoteRepository;

    public ChapterResponse createChapter(Long userId, CreateChapterRequest request) {
        // 사용자 책 존재 및 권한 확인
        UserBook userBook = userBookRepository.findByIdAndUserId(request.getUserBookId(), userId)
            .orElseThrow(() -> new RuntimeException("사용자 책을 찾을 수 없습니다"));

        // 페이지 범위 유효성 검사
        validatePageRange(userBook, request.getStartPage(), request.getEndPage());

        // 챕터 번호 중복 확인
        Optional<Chapter> existingChapter = chapterRepository
            .findByUserBookIdAndChapterNumber(request.getUserBookId(), request.getChapterNumber());
        if (existingChapter.isPresent()) {
            throw new RuntimeException("해당 챕터 번호가 이미 존재합니다: " + request.getChapterNumber());
        }

        // 페이지 범위 겹침 확인
        List<Chapter> overlappingChapters = chapterRepository
            .findOverlappingChapters(request.getUserBookId(), request.getStartPage(), request.getEndPage());
        if (!overlappingChapters.isEmpty()) {
            throw new RuntimeException("페이지 범위가 다른 챕터와 겹칩니다");
        }

        Chapter chapter = Chapter.builder()
            .userBook(userBook)
            .chapterNumber(request.getChapterNumber())
            .title(request.getTitle())
            .startPage(request.getStartPage())
            .endPage(request.getEndPage())
            .description(request.getDescription())
            .build();

        chapter = chapterRepository.save(chapter);
        
        logger.info("Chapter created: {} for user book {}", chapter.getId(), request.getUserBookId());
        
        return convertToResponse(chapter);
    }

    @Transactional(readOnly = true)
    public ChapterResponse getChapter(Long userId, Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new RuntimeException("챕터를 찾을 수 없습니다: " + chapterId));

        // 권한 확인
        if (!chapter.getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("챕터에 접근할 권한이 없습니다");
        }
        
        return convertToResponse(chapter);
    }

    public ChapterResponse updateChapter(Long userId, Long chapterId, UpdateChapterRequest request) {
        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new RuntimeException("챕터를 찾을 수 없습니다: " + chapterId));

        // 권한 확인
        if (!chapter.getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("챕터를 수정할 권한이 없습니다");
        }

        // 챕터 번호 중복 확인 (현재 챕터 제외)
        if (request.getChapterNumber() != null && 
            !request.getChapterNumber().equals(chapter.getChapterNumber())) {
            Optional<Chapter> existingChapter = chapterRepository
                .findByUserBookIdAndChapterNumber(chapter.getUserBook().getId(), request.getChapterNumber());
            if (existingChapter.isPresent()) {
                throw new RuntimeException("해당 챕터 번호가 이미 존재합니다: " + request.getChapterNumber());
            }
        }

        // 페이지 범위 유효성 검사
        Integer startPage = request.getStartPage() != null ? request.getStartPage() : chapter.getStartPage();
        Integer endPage = request.getEndPage() != null ? request.getEndPage() : chapter.getEndPage();
        validatePageRange(chapter.getUserBook(), startPage, endPage);

        // 페이지 범위 겹침 확인 (현재 챕터 제외)
        if (request.getStartPage() != null || request.getEndPage() != null) {
            List<Chapter> overlappingChapters = chapterRepository
                .findOverlappingChaptersExcludingCurrent(chapter.getUserBook().getId(), startPage, endPage, chapterId);
            if (!overlappingChapters.isEmpty()) {
                throw new RuntimeException("페이지 범위가 다른 챕터와 겹칩니다");
            }
        }

        // 필드별 업데이트
        if (request.getChapterNumber() != null) {
            chapter.updateChapterNumber(request.getChapterNumber());
        }
        if (StringUtils.hasText(request.getTitle())) {
            chapter.updateTitle(request.getTitle());
        }
        if (request.getStartPage() != null) {
            chapter.updateStartPage(request.getStartPage());
        }
        if (request.getEndPage() != null) {
            chapter.updateEndPage(request.getEndPage());
        }
        if (request.getDescription() != null) {
            chapter.updateDescription(request.getDescription());
        }

        chapter = chapterRepository.save(chapter);
        
        logger.info("Chapter updated: {}", chapterId);
        
        return convertToResponse(chapter);
    }

    public void deleteChapter(Long userId, Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
            .orElseThrow(() -> new RuntimeException("챕터를 찾을 수 없습니다: " + chapterId));

        // 권한 확인
        if (!chapter.getUserBook().getUser().getId().equals(userId)) {
            throw new RuntimeException("챕터를 삭제할 권한이 없습니다");
        }

        // 관련 노트가 있는지 확인
        long noteCount = readingNoteRepository.countByChapterId(chapterId);
        if (noteCount > 0) {
            throw new RuntimeException("챕터에 노트가 있어 삭제할 수 없습니다. 먼저 노트를 삭제해주세요");
        }

        chapterRepository.delete(chapter);
        
        logger.info("Chapter deleted: {}", chapterId);
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersByUserBook(Long userId, Long userBookId) {
        // 권한 확인
        UserBook userBook = userBookRepository.findByIdAndUserId(userBookId, userId)
            .orElseThrow(() -> new RuntimeException("사용자 책을 찾을 수 없습니다"));

        Sort sort = Sort.by(Sort.Direction.ASC, "chapterNumber");
        List<Chapter> chapters = chapterRepository.findByUserBookId(userBookId, sort);
        
        return chapters.stream()
            .map(this::convertToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> getChaptersWithNotes(Long userId, Long userBookId) {
        // 권한 확인
        UserBook userBook = userBookRepository.findByIdAndUserId(userBookId, userId)
            .orElseThrow(() -> new RuntimeException("사용자 책을 찾을 수 없습니다"));

        List<Chapter> chapters = chapterRepository.findChaptersWithNotes(userBookId);
        
        return chapters.stream()
            .map(this::convertToResponse)
            .toList();
    }

    private void validatePageRange(UserBook userBook, Integer startPage, Integer endPage) {
        if (startPage < 1 || endPage < 1) {
            throw new RuntimeException("페이지 번호는 1 이상이어야 합니다");
        }
        
        if (startPage > userBook.getBook().getTotalPages() || 
            endPage > userBook.getBook().getTotalPages()) {
            throw new RuntimeException("페이지 번호가 책의 총 페이지 수를 초과합니다");
        }
        
        if (startPage > endPage) {
            throw new RuntimeException("시작 페이지는 종료 페이지보다 작거나 같아야 합니다");
        }
    }

    private ChapterResponse convertToResponse(Chapter chapter) {
        // 노트 개수 조회
        long totalNotes = readingNoteRepository.countByChapterId(chapter.getId());
        
        return ChapterResponse.builder()
            .id(chapter.getId())
            .userBookId(chapter.getUserBook().getId())
            .chapterNumber(chapter.getChapterNumber())
            .title(chapter.getTitle())
            .startPage(chapter.getStartPage())
            .endPage(chapter.getEndPage())
            .description(chapter.getDescription())
            .createdAt(chapter.getCreatedAt())
            .updatedAt(chapter.getUpdatedAt())
            .totalNotes(totalNotes)
            .totalPages(chapter.getEndPage() - chapter.getStartPage() + 1)
            .hasNotes(totalNotes > 0)
            .build();
    }
}