// API 관련 타입 정의
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  errors?: string[];
}

export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
    };
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  numberOfElements: number;
}

// 인증 관련
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  username: string;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: {
    id: number;
    email: string;
    username: string;
    provider: string;
  };
}

// 책 관련
export interface BookSearchParams {
  title?: string;
  author?: string;
  category?: string;
  year?: number;
}

export interface CreateBookRequest {
  title: string;
  author: string;
  publisher: string;
  isbn?: string;
  publishedYear?: number;
  description?: string;
  totalPages: number;
  category: string;
}

// 독서 기록 관련
export interface CreateUserBookRequest {
  bookId: number;
  status: string;
  startDate?: string;
  currentPage?: number;
}

export interface UpdateUserBookRequest {
  status?: string;
  currentPage?: number;
  endDate?: string;
  rating?: number;
  review?: string;
  isPrivate?: boolean;
}

// 챕터 관련
export interface CreateChapterRequest {
  userBookId: number;
  chapterNumber: number;
  title: string;
  startPage: number;
  endPage: number;
  description?: string;
}

// 독서 노트 관련
export interface CreateReadingNoteRequest {
  chapterId: number;
  content: string;
  noteType: string;
  pageNumber: number;
  isPrivate?: boolean;
}

export interface UpdateReadingNoteRequest {
  content?: string;
  noteType?: string;
  pageNumber?: number;
  isPrivate?: boolean;
}

// AI 피드백 관련
export interface GenerateFeedbackRequest {
  noteId: number;
  feedbackType?: string;
}

export interface FeedbackRatingRequest {
  isUseful: boolean;
  rating?: number;
}

// 독서 목표 관련
export interface CreateReadingGoalRequest {
  year: number;
  targetBooks: number;
  targetPages: number;
}

export interface UpdateReadingGoalRequest {
  targetBooks?: number;
  targetPages?: number;
}

// 독서 세션 관련
export interface StartReadingSessionRequest {
  userBookId: number;
}

export interface EndReadingSessionRequest {
  pagesRead: number;
  notes?: string;
}

// 통계 관련
export interface ReadingStatistics {
  totalBooks: number;
  totalPages: number;
  completedBooks: number;
  averageRating: number;
  readingStreak: number;
  monthlyProgress: {
    month: string;
    booksRead: number;
    pagesRead: number;
  }[];
  categoryDistribution: {
    category: string;
    count: number;
  }[];
}