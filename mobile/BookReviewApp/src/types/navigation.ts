// 네비게이션 타입 정의
export type RootStackParamList = {
  Home: undefined;
  Login: undefined;
  Register: undefined;
  BookDetail: { bookId: number };
  ReadingNote: { noteId?: number; chapterId: number };
  Profile: undefined;
  Settings: undefined;
  Search: undefined;
  MyBooks: undefined;
  AddBook: undefined;
  ReadingGoals: undefined;
  Statistics: undefined;
};

export type TabParamList = {
  HomeTab: undefined;
  MyBooksTab: undefined;
  SearchTab: undefined;
  ProfileTab: undefined;
};

export type HomeStackParamList = {
  HomeScreen: undefined;
  BookDetail: { bookId: number };
};

export type MyBooksStackParamList = {
  MyBooksScreen: undefined;
  BookDetail: { bookId: number };
  ReadingNote: { noteId?: number; chapterId: number };
  AddBook: undefined;
};

export type SearchStackParamList = {
  SearchScreen: undefined;
  BookDetail: { bookId: number };
};

export type ProfileStackParamList = {
  ProfileScreen: undefined;
  Settings: undefined;
  ReadingGoals: undefined;
  Statistics: undefined;
};