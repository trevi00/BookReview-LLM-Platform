package com.bookreview.dto.statistics;

import lombok.Builder;
import lombok.Getter;

/**
 * 월별 진척률 DTO
 */
@Getter
@Builder
public class MonthlyProgress {
    
    private Integer month;
    private Integer cumulativeBooks;
    private Integer monthlyBooks;
    private Double progressPercentage;
}