// 기본 엔티티 타입들
export interface User {
  id: number;
  email: string;
  username: string;
  provider: 'LOCAL' | 'GOOGLE';
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface Book {
  id: number;
  title: string;
  author: string;
  publisher: string;
  isbn: string;
  publishedYear: number;
  description: string;
  totalPages: number;
  category: BookCategory;
  createdAt: Date;
  updatedAt: Date;
}

export type BookCategory = 
  | 'FICTION' 
  | 'NON_FICTION' 
  | 'TECHNOLOGY' 
  | 'SCIENCE' 
  | 'HISTORY' 
  | 'BIOGRAPHY' 
  | 'SELF_HELP' 
  | 'BUSINESS' 
  | 'EDUCATION' 
  | 'HEALTH' 
  | 'TRAVEL' 
  | 'COOKING' 
  | 'ART' 
  | 'RELIGION' 
  | 'PHILOSOPHY' 
  | 'PSYCHOLOGY' 
  | 'CHILDREN' 
  | 'POETRY' 
  | 'DRAMA' 
  | 'OTHER';

export interface UserBook {
  id: number;
  userId: number;
  bookId: number;
  status: ReadingStatus;
  startDate?: Date;
  endDate?: Date;
  currentPage: number;
  rating?: number;
  review?: string;
  isPrivate: boolean;
  createdAt: Date;
  updatedAt: Date;
  book?: Book;
}

export type ReadingStatus = 'NOT_STARTED' | 'READING' | 'COMPLETED' | 'PAUSED' | 'DROPPED';

export interface Chapter {
  id: number;
  userBookId: number;
  chapterNumber: number;
  title: string;
  startPage: number;
  endPage: number;
  description?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface ReadingNote {
  id: number;
  chapterId: number;
  userId: number;
  content: string;
  noteType: NoteType;
  pageNumber: number;
  isPrivate: boolean;
  createdAt: Date;
  updatedAt: Date;
  chapter?: Chapter;
  feedbacks?: Feedback[];
}

export type NoteType = 'SUMMARY' | 'QUESTION' | 'IMPRESSION' | 'LEARNING' | 'QUOTE';

export interface Feedback {
  id: number;
  readingNoteId: number;
  content: string;
  feedbackType: FeedbackType;
  aiModel: string;
  isUseful?: boolean;
  userRating?: number;
  createdAt: Date;
  updatedAt: Date;
}

export type FeedbackType = 'QUESTION' | 'SUGGESTION' | 'ENCOURAGEMENT' | 'ANALYSIS' | 'COMMENT';

export interface ReadingGoal {
  id: number;
  userId: number;
  year: number;
  targetBooks: number;
  targetPages: number;
  currentBooks: number;
  currentPages: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface ReadingSession {
  id: number;
  userBookId: number;
  startTime: Date;
  endTime?: Date;
  pagesRead: number;
  notes?: string;
  createdAt: Date;
  updatedAt: Date;
}