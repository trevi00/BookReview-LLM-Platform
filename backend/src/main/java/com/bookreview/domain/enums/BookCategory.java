package com.bookreview.domain.enums;

/**
 * 도서 카테고리 열거형
 */
public enum BookCategory {
    FICTION("소설"),
    NON_FICTION("에세이/비소설"),
    SCIENCE("과학"),
    TECHNOLOGY("기술/IT"),
    HISTORY("역사"),
    BIOGRAPHY("자서전/전기"),
    BUSINESS("경영/비즈니스"),
    SELF_HELP("자기계발"),
    PHILOSOPHY("철학"),
    PSYCHOLOGY("심리학"),
    ARTS("예술"),
    TRAVEL("여행"),
    COOKING("요리"),
    HEALTH("건강"),
    SPORTS("스포츠"),
    RELIGION("종교"),
    EDUCATION("교육"),
    LANGUAGE("언어학습"),
    CHILDREN("어린이"),
    YOUNG_ADULT("청소년"),
    MYSTERY("미스터리"),
    ROMANCE("로맨스"),
    FANTASY("판타지"),
    SCI_FI("SF"),
    HORROR("공포"),
    THRILLER("스릴러"),
    POETRY("시"),
    DRAMA("희곡"),
    COMICS("만화"),
    OTHER("기타");

    private final String description;

    BookCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}