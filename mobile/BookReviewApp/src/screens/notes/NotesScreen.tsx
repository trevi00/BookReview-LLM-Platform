import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  TextInput,
  RefreshControl,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { useNavigation } from '@react-navigation/native';
import { NotesScreenNavigationProp } from '../../types/navigation';
import { ReadingNote, NoteType } from '../../types';

interface NoteItem extends ReadingNote {
  chapter?: {
    title: string;
    userBook: {
      book: {
        title: string;
        author: string;
      };
    };
  };
  feedbackCount?: number;
}

const NotesScreen = () => {
  const navigation = useNavigation<NotesScreenNavigationProp>();
  const [notes, setNotes] = useState<NoteItem[]>([]);
  const [filteredNotes, setFilteredNotes] = useState<NoteItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilter, setSelectedFilter] = useState<NoteType | 'ALL'>('ALL');
  const [refreshing, setRefreshing] = useState(false);

  // Mock data
  const mockNotes: NoteItem[] = [
    {
      id: 1,
      chapterId: 1,
      userId: 1,
      content: '클린 아키텍처의 핵심 원칙들을 이해했습니다. 의존성 역전 원칙이 특히 인상깊었어요.',
      noteType: 'IMPRESSION',
      pageNumber: 45,
      isPrivate: false,
      createdAt: new Date('2024-01-15'),
      updatedAt: new Date('2024-01-15'),
      chapter: {
        title: '1장. 소프트웨어 설계',
        userBook: {
          book: {
            title: '클린 아키텍처',
            author: '로버트 C. 마틴',
          },
        },
      },
      feedbackCount: 2,
    },
    {
      id: 2,
      chapterId: 2,
      userId: 1,
      content: '"좋은 아키텍처는 결정을 미루는 것이다" - 이 문장이 매우 인상깊습니다.',
      noteType: 'QUOTE',
      pageNumber: 78,
      isPrivate: false,
      createdAt: new Date('2024-01-16'),
      updatedAt: new Date('2024-01-16'),
      chapter: {
        title: '2장. 아키텍처란 무엇인가',
        userBook: {
          book: {
            title: '클린 아키텍처',
            author: '로버트 C. 마틴',
          },
        },
      },
      feedbackCount: 1,
    },
    {
      id: 3,
      chapterId: 3,
      userId: 1,
      content: '의존성 규칙이 실제 프로젝트에서 어떻게 적용되는지 궁금합니다.',
      noteType: 'QUESTION',
      pageNumber: 120,
      isPrivate: false,
      createdAt: new Date('2024-01-17'),
      updatedAt: new Date('2024-01-17'),
      chapter: {
        title: '3장. 의존성 규칙',
        userBook: {
          book: {
            title: '클린 아키텍처',
            author: '로버트 C. 마틴',
          },
        },
      },
      feedbackCount: 3,
    },
  ];

  useEffect(() => {
    setNotes(mockNotes);
    setFilteredNotes(mockNotes);
  }, []);

  useEffect(() => {
    filterNotes();
  }, [searchQuery, selectedFilter, notes]);

  const filterNotes = () => {
    let filtered = notes;

    // Filter by note type
    if (selectedFilter !== 'ALL') {
      filtered = filtered.filter(note => note.noteType === selectedFilter);
    }

    // Filter by search query
    if (searchQuery) {
      filtered = filtered.filter(note =>
        note.content.toLowerCase().includes(searchQuery.toLowerCase()) ||
        note.chapter?.userBook.book.title.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    setFilteredNotes(filtered);
  };

  const onRefresh = async () => {
    setRefreshing(true);
    // TODO: API에서 데이터 새로고침
    setTimeout(() => {
      setRefreshing(false);
    }, 1000);
  };

  const getNoteTypeText = (type: NoteType) => {
    switch (type) {
      case 'SUMMARY': return '요약';
      case 'QUESTION': return '질문';
      case 'IMPRESSION': return '감상';
      case 'LEARNING': return '학습';
      case 'QUOTE': return '인용';
      default: return type;
    }
  };

  const getNoteTypeColor = (type: NoteType) => {
    switch (type) {
      case 'SUMMARY': return '#007AFF';
      case 'QUESTION': return '#FF9500';
      case 'IMPRESSION': return '#34C759';
      case 'LEARNING': return '#5856D6';
      case 'QUOTE': return '#FF3B30';
      default: return '#8E8E93';
    }
  };

  const formatDate = (date: Date) => {
    return date.toLocaleDateString('ko-KR', {
      month: 'short',
      day: 'numeric',
    });
  };

  const renderNoteItem = ({ item }: { item: NoteItem }) => (
    <TouchableOpacity
      style={styles.noteItem}
      onPress={() => navigation.navigate('NoteDetail', { noteId: item.id })}
    >
      <View style={styles.noteHeader}>
        <View style={styles.noteInfo}>
          <Text style={styles.bookTitle} numberOfLines={1}>
            {item.chapter?.userBook.book.title}
          </Text>
          <Text style={styles.chapterTitle} numberOfLines={1}>
            {item.chapter?.title}
          </Text>
        </View>
        <Text style={styles.noteDate}>{formatDate(item.createdAt)}</Text>
      </View>

      <Text style={styles.noteContent} numberOfLines={3}>
        {item.content}
      </Text>

      <View style={styles.noteFooter}>
        <View style={styles.noteDetails}>
          <View style={[
            styles.typeBadge,
            { backgroundColor: getNoteTypeColor(item.noteType) + '20' }
          ]}>
            <Text style={[styles.typeText, { color: getNoteTypeColor(item.noteType) }]}>
              {getNoteTypeText(item.noteType)}
            </Text>
          </View>
          <Text style={styles.pageNumber}>p. {item.pageNumber}</Text>
        </View>
        
        {item.feedbackCount && item.feedbackCount > 0 && (
          <View style={styles.feedbackInfo}>
            <Icon name="auto-awesome" size={16} color="#FF9500" />
            <Text style={styles.feedbackCount}>{item.feedbackCount}</Text>
          </View>
        )}
      </View>
    </TouchableOpacity>
  );

  const FilterButton = ({ type, label }: { type: NoteType | 'ALL', label: string }) => (
    <TouchableOpacity
      style={[
        styles.filterButton,
        selectedFilter === type && styles.filterButtonActive
      ]}
      onPress={() => setSelectedFilter(type)}
    >
      <Text style={[
        styles.filterButtonText,
        selectedFilter === type && styles.filterButtonTextActive
      ]}>
        {label}
      </Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>독서 노트</Text>
        <TouchableOpacity
          style={styles.addButton}
          onPress={() => navigation.navigate('AddNote')}
        >
          <Icon name="add" size={24} color="#007AFF" />
        </TouchableOpacity>
      </View>

      {/* Search */}
      <View style={styles.searchContainer}>
        <Icon name="search" size={20} color="#8E8E93" style={styles.searchIcon} />
        <TextInput
          style={styles.searchInput}
          placeholder="노트 내용이나 책 제목으로 검색"
          value={searchQuery}
          onChangeText={setSearchQuery}
        />
        {searchQuery.length > 0 && (
          <TouchableOpacity onPress={() => setSearchQuery('')}>
            <Icon name="clear" size={20} color="#8E8E93" />
          </TouchableOpacity>
        )}
      </View>

      {/* Filters */}
      <View style={styles.filtersContainer}>
        <FilterButton type="ALL" label="전체" />
        <FilterButton type="IMPRESSION" label="감상" />
        <FilterButton type="SUMMARY" label="요약" />
        <FilterButton type="QUESTION" label="질문" />
        <FilterButton type="QUOTE" label="인용" />
        <FilterButton type="LEARNING" label="학습" />
      </View>

      {/* Notes List */}
      <FlatList
        data={filteredNotes}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderNoteItem}
        contentContainerStyle={styles.listContainer}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="note" size={64} color="#C7C7CC" />
            <Text style={styles.emptyTitle}>작성된 노트가 없습니다</Text>
            <Text style={styles.emptySubtitle}>
              첫 번째 독서 노트를 작성해보세요
            </Text>
            <TouchableOpacity
              style={styles.emptyButton}
              onPress={() => navigation.navigate('AddNote')}
            >
              <Text style={styles.emptyButtonText}>노트 작성하기</Text>
            </TouchableOpacity>
          </View>
        }
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 60,
    paddingBottom: 16,
    backgroundColor: '#FFFFFF',
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#1C1C1E',
  },
  addButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#007AFF20',
    justifyContent: 'center',
    alignItems: 'center',
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    marginHorizontal: 20,
    marginVertical: 16,
    paddingHorizontal: 12,
    borderRadius: 10,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  searchIcon: {
    marginRight: 8,
  },
  searchInput: {
    flex: 1,
    height: 44,
    fontSize: 16,
    color: '#1C1C1E',
  },
  filtersContainer: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    marginBottom: 16,
  },
  filterButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#FFFFFF',
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#E5E5EA',
  },
  filterButtonActive: {
    backgroundColor: '#007AFF',
    borderColor: '#007AFF',
  },
  filterButtonText: {
    fontSize: 14,
    color: '#1C1C1E',
    fontWeight: '500',
  },
  filterButtonTextActive: {
    color: '#FFFFFF',
  },
  listContainer: {
    paddingHorizontal: 20,
  },
  noteItem: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3,
  },
  noteHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  noteInfo: {
    flex: 1,
    marginRight: 12,
  },
  bookTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  chapterTitle: {
    fontSize: 12,
    color: '#8E8E93',
    marginTop: 2,
  },
  noteDate: {
    fontSize: 12,
    color: '#8E8E93',
  },
  noteContent: {
    fontSize: 16,
    color: '#1C1C1E',
    lineHeight: 22,
    marginBottom: 12,
  },
  noteFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  noteDetails: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  typeBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
    marginRight: 8,
  },
  typeText: {
    fontSize: 12,
    fontWeight: '600',
  },
  pageNumber: {
    fontSize: 12,
    color: '#8E8E93',
  },
  feedbackInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  feedbackCount: {
    fontSize: 12,
    color: '#FF9500',
    fontWeight: '600',
    marginLeft: 4,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 100,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginTop: 16,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 16,
    color: '#8E8E93',
    marginBottom: 24,
  },
  emptyButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  emptyButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default NotesScreen;