package com.bookreview.dto.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {
    
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
    
    // 정렬 정보
    private Sort sort;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Sort {
        private boolean sorted;
        private boolean unsorted;
        private List<SortOrder> orders;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SortOrder {
            private String property;
            private Direction direction;
            
            public enum Direction {
                ASC, DESC
            }
        }
    }
    
    // Spring Data의 Page를 PagedResponse로 변환하는 유틸리티 메서드
    public static <T> PagedResponse<T> of(org.springframework.data.domain.Page<T> page) {
        List<Sort.SortOrder> sortOrders = page.getSort().stream()
            .map(order -> Sort.SortOrder.builder()
                .property(order.getProperty())
                .direction(order.getDirection().isAscending() ? 
                    Sort.SortOrder.Direction.ASC : Sort.SortOrder.Direction.DESC)
                .build())
            .toList();
            
        Sort sort = Sort.builder()
            .sorted(page.getSort().isSorted())
            .unsorted(page.getSort().isUnsorted())
            .orders(sortOrders)
            .build();
            
        return PagedResponse.<T>builder()
            .content(page.getContent())
            .pageNumber(page.getNumber())
            .pageSize(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .empty(page.isEmpty())
            .sort(sort)
            .build();
    }
}