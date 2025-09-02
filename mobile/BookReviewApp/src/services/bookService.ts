import { apiClient, PaginatedResponse } from './api';
import { 
  Book, 
  UserBook, 
  Chapter, 
  ReadingNote, 
  AiFeedback,
  ReadingSession,
  BookCategory,
  ReadingStatus,
  NoteType
} from '../types';
import { ApiResponse } from '../types/api';

interface BookSearchParams {
  query?: string;
  category?: BookCategory;
  author?: string;
  isbn?: string;
}

interface CreateBookRequest {
  title: string;
  author: string;
  publisher?: string;
  isbn?: string;
  publishedYear?: number;
  totalPages?: number;
  category: BookCategory;
  description?: string;
  coverImageUrl?: string;
}

interface CreateUserBookRequest {
  bookId: number;
  status: ReadingStatus;
  currentPage?: number;
  rating?: number;
  isPrivate?: boolean;
}

interface UpdateUserBookRequest {
  status?: ReadingStatus;
  currentPage?: number;
  rating?: number;
  isPrivate?: boolean;
}

interface CreateChapterRequest {
  userBookId: number;
  title: string;
  chapterNumber: number;
  startPage: number;
  endPage: number;
}

interface CreateReadingNoteRequest {
  chapterId: number;
  content: string;
  noteType: NoteType;
  pageNumber?: number;
  isPrivate?: boolean;
}

interface UpdateReadingNoteRequest {
  content?: string;
  noteType?: NoteType;
  pageNumber?: number;
  isPrivate?: boolean;
}

interface GenerateFeedbackRequest {
  noteId: number;
  content: string;
  context?: string;
}

interface StartReadingSessionRequest {
  userBookId: number;
  startPage: number;
}

interface EndReadingSessionRequest {
  endPage: number;
  duration: number;
}

interface PaginationParams {
  page?: number;
  size?: number;
}

class BookService {
  // 책 검색
  async searchBooks(params: BookSearchParams & PaginationParams): Promise<ApiResponse<PaginatedResponse<Book>>> {
    return apiClient.get('/books/search', params);
  }

  // 책 상세 정보 조회
  async getBook(bookId: number): Promise<ApiResponse<Book>> {
    return apiClient.get(`/books/${bookId}`);
  }

  // 새 책 등록
  async createBook(bookData: CreateBookRequest): Promise<ApiResponse<Book>> {
    return apiClient.post('/books', bookData);
  }

  // 사용자 책 목록 조회
  async getUserBooks(params?: PaginationParams): Promise<ApiResponse<PaginatedResponse<UserBook>>> {
    return apiClient.get('/user-books', params);
  }

  // 사용자 책 상세 조회
  async getUserBook(userBookId: number): Promise<ApiResponse<UserBook>> {
    return apiClient.get(`/user-books/${userBookId}`);
  }

  // 사용자 책 등록
  async addUserBook(bookData: CreateUserBookRequest): Promise<ApiResponse<UserBook>> {
    return apiClient.post('/user-books', bookData);
  }

  // 사용자 책 정보 수정
  async updateUserBook(userBookId: number, updateData: UpdateUserBookRequest): Promise<ApiResponse<UserBook>> {
    return apiClient.patch(`/user-books/${userBookId}`, updateData);
  }

  // 사용자 책 삭제
  async deleteUserBook(userBookId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`/user-books/${userBookId}`);
  }

  // 챕터 목록 조회
  async getChapters(userBookId: number): Promise<ApiResponse<Chapter[]>> {
    return apiClient.get(`/user-books/${userBookId}/chapters`);
  }

  // 챕터 생성
  async createChapter(chapterData: CreateChapterRequest): Promise<ApiResponse<Chapter>> {
    return apiClient.post('/chapters', chapterData);
  }

  // 챕터 수정
  async updateChapter(chapterId: number, updateData: Partial<CreateChapterRequest>): Promise<ApiResponse<Chapter>> {
    return apiClient.patch(`/chapters/${chapterId}`, updateData);
  }

  // 챕터 삭제
  async deleteChapter(chapterId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`/chapters/${chapterId}`);
  }

  // 독서 노트 목록 조회
  async getReadingNotes(params?: PaginationParams & { chapterId?: number; userId?: number }): Promise<ApiResponse<PaginatedResponse<ReadingNote>>> {
    return apiClient.get('/reading-notes', params);
  }

  // 독서 노트 상세 조회
  async getReadingNote(noteId: number): Promise<ApiResponse<ReadingNote>> {
    return apiClient.get(`/reading-notes/${noteId}`);
  }

  // 독서 노트 생성
  async createReadingNote(noteData: CreateReadingNoteRequest): Promise<ApiResponse<ReadingNote>> {
    return apiClient.post('/reading-notes', noteData);
  }

  // 독서 노트 수정
  async updateReadingNote(noteId: number, updateData: UpdateReadingNoteRequest): Promise<ApiResponse<ReadingNote>> {
    return apiClient.patch(`/reading-notes/${noteId}`, updateData);
  }

  // 독서 노트 삭제
  async deleteReadingNote(noteId: number): Promise<ApiResponse<void>> {
    return apiClient.delete(`/reading-notes/${noteId}`);
  }

  // AI 피드백 생성
  async generateFeedback(feedbackData: GenerateFeedbackRequest): Promise<ApiResponse<AiFeedback>> {
    return apiClient.aiPost('/feedback/generate', feedbackData);
  }

  // 노트의 AI 피드백 조회
  async getNoteFeedbacks(noteId: number): Promise<ApiResponse<AiFeedback[]>> {
    return apiClient.get(`/reading-notes/${noteId}/feedbacks`);
  }

  // 독서 세션 시작
  async startReadingSession(sessionData: StartReadingSessionRequest): Promise<ApiResponse<ReadingSession>> {
    return apiClient.post('/reading-sessions', sessionData);
  }

  // 독서 세션 종료
  async endReadingSession(sessionId: number, endData: EndReadingSessionRequest): Promise<ApiResponse<ReadingSession>> {
    return apiClient.patch(`/reading-sessions/${sessionId}/end`, endData);
  }

  // 사용자별 독서 세션 목록 조회
  async getReadingSessions(params?: PaginationParams & { userBookId?: number }): Promise<ApiResponse<PaginatedResponse<ReadingSession>>> {
    return apiClient.get('/reading-sessions', params);
  }

  // 독서 통계 조회
  async getReadingStatistics(): Promise<ApiResponse<any>> {
    return apiClient.get('/statistics/reading');
  }

  // 노트 통계 조회
  async getNoteStatistics(): Promise<ApiResponse<any>> {
    return apiClient.get('/statistics/notes');
  }

  // ISBN으로 책 검색
  async searchByISBN(isbn: string): Promise<ApiResponse<Book>> {
    return apiClient.get('/books/search/isbn', { isbn });
  }

  // 인기 책 목록 조회
  async getPopularBooks(limit: number = 10): Promise<ApiResponse<Book[]>> {
    return apiClient.get('/books/popular', { limit });
  }

  // 최근 추가된 책 목록 조회
  async getRecentBooks(limit: number = 10): Promise<ApiResponse<Book[]>> {
    return apiClient.get('/books/recent', { limit });
  }

  // 추천 책 목록 조회
  async getRecommendedBooks(limit: number = 10): Promise<ApiResponse<Book[]>> {
    return apiClient.get('/books/recommended', { limit });
  }
}

export const bookService = new BookService();

export type {
  BookSearchParams,
  CreateBookRequest,
  CreateUserBookRequest,
  UpdateUserBookRequest,
  CreateChapterRequest,
  CreateReadingNoteRequest,
  UpdateReadingNoteRequest,
  GenerateFeedbackRequest,
  StartReadingSessionRequest,
  EndReadingSessionRequest,
  PaginationParams,
};