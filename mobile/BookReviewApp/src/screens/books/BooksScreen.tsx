import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  TextInput,
  Image,
  Alert,
  RefreshControl,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { useNavigation } from '@react-navigation/native';
import { BooksScreenNavigationProp } from '../../types/navigation';
import { UserBook, ReadingStatus } from '../../types';

interface BookItem extends UserBook {
  book: {
    id: number;
    title: string;
    author: string;
    coverImageUrl?: string;
    totalPages: number;
  };
}

const BooksScreen = () => {
  const navigation = useNavigation<BooksScreenNavigationProp>();
  const [books, setBooks] = useState<BookItem[]>([]);
  const [filteredBooks, setFilteredBooks] = useState<BookItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedFilter, setSelectedFilter] = useState<ReadingStatus | 'ALL'>('ALL');
  const [refreshing, setRefreshing] = useState(false);

  // Mock data
  const mockBooks: BookItem[] = [
    {
      id: 1,
      userId: 1,
      bookId: 1,
      status: 'READING',
      currentPage: 150,
      rating: 4,
      isPrivate: false,
      createdAt: new Date(),
      updatedAt: new Date(),
      book: {
        id: 1,
        title: '클린 아키텍처',
        author: '로버트 C. 마틴',
        coverImageUrl: 'https://via.placeholder.com/150x200',
        totalPages: 350,
      },
    },
    {
      id: 2,
      userId: 1,
      bookId: 2,
      status: 'COMPLETED',
      currentPage: 280,
      rating: 5,
      isPrivate: false,
      createdAt: new Date(),
      updatedAt: new Date(),
      book: {
        id: 2,
        title: '이펙티브 자바',
        author: '조슈아 블로크',
        coverImageUrl: 'https://via.placeholder.com/150x200',
        totalPages: 280,
      },
    },
    {
      id: 3,
      userId: 1,
      bookId: 3,
      status: 'NOT_STARTED',
      currentPage: 0,
      isPrivate: false,
      createdAt: new Date(),
      updatedAt: new Date(),
      book: {
        id: 3,
        title: '코틀린 인 액션',
        author: '드미트리 제메로프',
        coverImageUrl: 'https://via.placeholder.com/150x200',
        totalPages: 450,
      },
    },
  ];

  useEffect(() => {
    setBooks(mockBooks);
    setFilteredBooks(mockBooks);
  }, []);

  useEffect(() => {
    filterBooks();
  }, [searchQuery, selectedFilter, books]);

  const filterBooks = () => {
    let filtered = books;

    // Filter by status
    if (selectedFilter !== 'ALL') {
      filtered = filtered.filter(book => book.status === selectedFilter);
    }

    // Filter by search query
    if (searchQuery) {
      filtered = filtered.filter(book =>
        book.book.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        book.book.author.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    setFilteredBooks(filtered);
  };

  const onRefresh = async () => {
    setRefreshing(true);
    // TODO: API에서 데이터 새로고침
    setTimeout(() => {
      setRefreshing(false);
    }, 1000);
  };

  const getStatusText = (status: ReadingStatus) => {
    switch (status) {
      case 'NOT_STARTED': return '읽기 전';
      case 'READING': return '읽는 중';
      case 'COMPLETED': return '완료';
      case 'PAUSED': return '중단';
      case 'DROPPED': return '포기';
      default: return status;
    }
  };

  const getStatusColor = (status: ReadingStatus) => {
    switch (status) {
      case 'NOT_STARTED': return '#8E8E93';
      case 'READING': return '#007AFF';
      case 'COMPLETED': return '#34C759';
      case 'PAUSED': return '#FF9500';
      case 'DROPPED': return '#FF3B30';
      default: return '#8E8E93';
    }
  };

  const calculateProgress = (currentPage: number, totalPages: number) => {
    return totalPages > 0 ? (currentPage / totalPages) * 100 : 0;
  };

  const renderBookItem = ({ item }: { item: BookItem }) => (
    <TouchableOpacity
      style={styles.bookItem}
      onPress={() => navigation.navigate('BookDetail', { bookId: item.id })}
    >
      <Image
        source={{ uri: item.book.coverImageUrl || 'https://via.placeholder.com/150x200' }}
        style={styles.bookCover}
        resizeMode="cover"
      />
      <View style={styles.bookInfo}>
        <Text style={styles.bookTitle} numberOfLines={2}>
          {item.book.title}
        </Text>
        <Text style={styles.bookAuthor} numberOfLines={1}>
          {item.book.author}
        </Text>
        
        <View style={styles.statusContainer}>
          <View style={[styles.statusBadge, { backgroundColor: getStatusColor(item.status) + '20' }]}>
            <Text style={[styles.statusText, { color: getStatusColor(item.status) }]}>
              {getStatusText(item.status)}
            </Text>
          </View>
        </View>

        {item.status === 'READING' && (
          <View style={styles.progressContainer}>
            <View style={styles.progressBar}>
              <View
                style={[
                  styles.progressFill,
                  { width: `${calculateProgress(item.currentPage, item.book.totalPages)}%` }
                ]}
              />
            </View>
            <Text style={styles.progressText}>
              {item.currentPage} / {item.book.totalPages} 페이지
            </Text>
          </View>
        )}

        {item.rating && (
          <View style={styles.ratingContainer}>
            {[1, 2, 3, 4, 5].map(star => (
              <Icon
                key={star}
                name={star <= item.rating! ? 'star' : 'star-border'}
                size={16}
                color="#FFD60A"
              />
            ))}
          </View>
        )}
      </View>
      <Icon name="chevron-right" size={24} color="#C7C7CC" />
    </TouchableOpacity>
  );

  const FilterButton = ({ status, label }: { status: ReadingStatus | 'ALL', label: string }) => (
    <TouchableOpacity
      style={[
        styles.filterButton,
        selectedFilter === status && styles.filterButtonActive
      ]}
      onPress={() => setSelectedFilter(status)}
    >
      <Text style={[
        styles.filterButtonText,
        selectedFilter === status && styles.filterButtonTextActive
      ]}>
        {label}
      </Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>내 서재</Text>
        <TouchableOpacity
          style={styles.addButton}
          onPress={() => navigation.navigate('AddBook')}
        >
          <Icon name="add" size={24} color="#007AFF" />
        </TouchableOpacity>
      </View>

      {/* Search */}
      <View style={styles.searchContainer}>
        <Icon name="search" size={20} color="#8E8E93" style={styles.searchIcon} />
        <TextInput
          style={styles.searchInput}
          placeholder="책 제목이나 저자로 검색"
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
        <FilterButton status="ALL" label="전체" />
        <FilterButton status="READING" label="읽는 중" />
        <FilterButton status="COMPLETED" label="완료" />
        <FilterButton status="NOT_STARTED" label="읽기 전" />
        <FilterButton status="PAUSED" label="중단" />
      </View>

      {/* Books List */}
      <FlatList
        data={filteredBooks}
        keyExtractor={(item) => item.id.toString()}
        renderItem={renderBookItem}
        contentContainerStyle={styles.listContainer}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Icon name="book" size={64} color="#C7C7CC" />
            <Text style={styles.emptyTitle}>등록된 책이 없습니다</Text>
            <Text style={styles.emptySubtitle}>
              첫 번째 책을 추가해보세요
            </Text>
            <TouchableOpacity
              style={styles.emptyButton}
              onPress={() => navigation.navigate('AddBook')}
            >
              <Text style={styles.emptyButtonText}>책 추가하기</Text>
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
  bookItem: {
    flexDirection: 'row',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3,
  },
  bookCover: {
    width: 60,
    height: 80,
    borderRadius: 8,
    backgroundColor: '#F2F2F7',
  },
  bookInfo: {
    flex: 1,
    marginLeft: 16,
  },
  bookTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 4,
  },
  bookAuthor: {
    fontSize: 14,
    color: '#8E8E93',
    marginBottom: 8,
  },
  statusContainer: {
    marginBottom: 8,
  },
  statusBadge: {
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusText: {
    fontSize: 12,
    fontWeight: '600',
  },
  progressContainer: {
    marginBottom: 8,
  },
  progressBar: {
    height: 4,
    backgroundColor: '#E5E5EA',
    borderRadius: 2,
    marginBottom: 4,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#007AFF',
    borderRadius: 2,
  },
  progressText: {
    fontSize: 12,
    color: '#8E8E93',
  },
  ratingContainer: {
    flexDirection: 'row',
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

export default BooksScreen;