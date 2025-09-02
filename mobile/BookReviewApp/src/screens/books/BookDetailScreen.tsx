import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Image,
  Alert,
  Dimensions,
  Modal,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { BookDetailScreenNavigationProp } from '../../types/navigation';
import { UserBook, ReadingStatus, Chapter } from '../../types';

const { width } = Dimensions.get('window');

interface BookDetailRouteParams {
  bookId: number;
}

interface BookDetailData extends UserBook {
  book: {
    id: number;
    title: string;
    author: string;
    publisher: string;
    isbn: string;
    publishedYear: number;
    totalPages: number;
    coverImageUrl?: string;
    description: string;
  };
  chapters: Chapter[];
  notesCount: number;
}

const BookDetailScreen = () => {
  const navigation = useNavigation<BookDetailScreenNavigationProp>();
  const route = useRoute<RouteProp<{ params: BookDetailRouteParams }, 'params'>>();
  const { bookId } = route.params;
  
  const [bookData, setBookData] = useState<BookDetailData | null>(null);
  const [loading, setLoading] = useState(true);
  const [statusModalVisible, setStatusModalVisible] = useState(false);

  // Mock data
  const mockBookData: BookDetailData = {
    id: 1,
    userId: 1,
    bookId: 1,
    status: 'READING',
    currentPage: 150,
    rating: 4,
    isPrivate: false,
    createdAt: new Date('2024-01-15'),
    updatedAt: new Date('2024-01-20'),
    book: {
      id: 1,
      title: '클린 아키텍처',
      author: '로버트 C. 마틴',
      publisher: '인사이트',
      isbn: '978-89-6626-234-9',
      publishedYear: 2019,
      totalPages: 350,
      coverImageUrl: 'https://via.placeholder.com/300x400',
      description: '소프트웨어 구조와 설계의 원칙에 대한 포괄적인 가이드입니다. 이 책은 좋은 소프트웨어의 특징과 나쁜 소프트웨어의 문제점을 명확히 구분하여 설명하며, 시간이 지나도 변하지 않는 소프트웨어 아키텍처의 핵심 원칙들을 다룹니다.',
    },
    chapters: [
      {
        id: 1,
        userBookId: 1,
        title: '1장. 소프트웨어 설계',
        chapterNumber: 1,
        startPage: 1,
        endPage: 50,
        createdAt: new Date(),
        updatedAt: new Date(),
      },
      {
        id: 2,
        userBookId: 1,
        title: '2장. 아키텍처란 무엇인가',
        chapterNumber: 2,
        startPage: 51,
        endPage: 100,
        createdAt: new Date(),
        updatedAt: new Date(),
      },
      {
        id: 3,
        userBookId: 1,
        title: '3장. 의존성 규칙',
        chapterNumber: 3,
        startPage: 101,
        endPage: 150,
        createdAt: new Date(),
        updatedAt: new Date(),
      },
    ],
    notesCount: 8,
  };

  useEffect(() => {
    loadBookData();
  }, [bookId]);

  const loadBookData = async () => {
    setLoading(true);
    try {
      // TODO: API 호출
      // const response = await bookService.getBookDetail(bookId);
      // setBookData(response.data);
      
      // Mock data for now
      setTimeout(() => {
        setBookData(mockBookData);
        setLoading(false);
      }, 500);
    } catch (error) {
      setLoading(false);
      Alert.alert('오류', '책 정보를 불러오는데 실패했습니다.');
    }
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

  const calculateProgress = () => {
    if (!bookData) return 0;
    return bookData.book.totalPages > 0 
      ? (bookData.currentPage / bookData.book.totalPages) * 100 
      : 0;
  };

  const handleStatusChange = (newStatus: ReadingStatus) => {
    if (!bookData) return;
    
    setBookData(prev => prev ? { ...prev, status: newStatus } : null);
    setStatusModalVisible(false);
    
    // TODO: API 호출로 상태 업데이트
    Alert.alert('성공', '읽기 상태가 변경되었습니다.');
  };

  const handleEditBook = () => {
    // TODO: 책 편집 화면으로 이동
    Alert.alert('준비중', '책 편집 기능을 준비중입니다.');
  };

  const handleDeleteBook = () => {
    Alert.alert(
      '책 삭제',
      '정말로 이 책을 삭제하시겠습니까? 관련된 모든 노트도 함께 삭제됩니다.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: () => {
            // TODO: API 호출로 책 삭제
            Alert.alert('성공', '책이 삭제되었습니다.', [
              { text: '확인', onPress: () => navigation.goBack() },
            ]);
          },
        },
      ]
    );
  };

  const StatusModal = () => {
    const statuses: { status: ReadingStatus; label: string }[] = [
      { status: 'NOT_STARTED', label: '읽기 전' },
      { status: 'READING', label: '읽는 중' },
      { status: 'COMPLETED', label: '완료' },
      { status: 'PAUSED', label: '중단' },
      { status: 'DROPPED', label: '포기' },
    ];

    return (
      <Modal
        visible={statusModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setStatusModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>읽기 상태 변경</Text>
            
            {statuses.map((item) => (
              <TouchableOpacity
                key={item.status}
                style={[
                  styles.statusOption,
                  bookData?.status === item.status && styles.statusOptionActive
                ]}
                onPress={() => handleStatusChange(item.status)}
              >
                <View style={[
                  styles.statusDot,
                  { backgroundColor: getStatusColor(item.status) }
                ]} />
                <Text style={[
                  styles.statusOptionText,
                  bookData?.status === item.status && styles.statusOptionTextActive
                ]}>
                  {item.label}
                </Text>
                {bookData?.status === item.status && (
                  <Icon name="check" size={20} color="#007AFF" />
                )}
              </TouchableOpacity>
            ))}
            
            <TouchableOpacity
              style={styles.modalCancelButton}
              onPress={() => setStatusModalVisible(false)}
            >
              <Text style={styles.modalCancelText}>취소</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    );
  };

  if (loading || !bookData) {
    return (
      <View style={styles.loadingContainer}>
        <Text style={styles.loadingText}>로딩 중...</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
        >
          <Icon name="arrow-back" size={24} color="#1C1C1E" />
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.moreButton}
          onPress={handleEditBook}
        >
          <Icon name="edit" size={24} color="#1C1C1E" />
        </TouchableOpacity>
      </View>

      {/* Book Info */}
      <View style={styles.bookInfoSection}>
        <Image
          source={{ uri: bookData.book.coverImageUrl || 'https://via.placeholder.com/300x400' }}
          style={styles.bookCover}
          resizeMode="cover"
        />
        
        <View style={styles.bookDetails}>
          <Text style={styles.bookTitle}>{bookData.book.title}</Text>
          <Text style={styles.bookAuthor}>{bookData.book.author}</Text>
          <Text style={styles.bookPublisher}>
            {bookData.book.publisher} · {bookData.book.publishedYear}
          </Text>
          
          <View style={styles.bookMeta}>
            <View style={styles.metaItem}>
              <Icon name="book" size={16} color="#8E8E93" />
              <Text style={styles.metaText}>{bookData.book.totalPages} 페이지</Text>
            </View>
            <View style={styles.metaItem}>
              <Icon name="note" size={16} color="#8E8E93" />
              <Text style={styles.metaText}>노트 {bookData.notesCount}개</Text>
            </View>
          </View>

          {/* Status and Progress */}
          <TouchableOpacity
            style={styles.statusContainer}
            onPress={() => setStatusModalVisible(true)}
          >
            <View style={[
              styles.statusBadge,
              { backgroundColor: getStatusColor(bookData.status) + '20' }
            ]}>
              <Text style={[
                styles.statusText,
                { color: getStatusColor(bookData.status) }
              ]}>
                {getStatusText(bookData.status)}
              </Text>
            </View>
            <Icon name="expand-more" size={20} color="#8E8E93" />
          </TouchableOpacity>

          {bookData.status === 'READING' && (
            <View style={styles.progressSection}>
              <View style={styles.progressHeader}>
                <Text style={styles.progressTitle}>읽기 진행률</Text>
                <Text style={styles.progressPercentage}>
                  {Math.round(calculateProgress())}%
                </Text>
              </View>
              <View style={styles.progressBar}>
                <View
                  style={[
                    styles.progressFill,
                    { width: `${calculateProgress()}%` }
                  ]}
                />
              </View>
              <Text style={styles.progressText}>
                {bookData.currentPage} / {bookData.book.totalPages} 페이지
              </Text>
            </View>
          )}

          {/* Rating */}
          {bookData.rating && (
            <View style={styles.ratingSection}>
              <Text style={styles.ratingTitle}>내 평점</Text>
              <View style={styles.ratingContainer}>
                {[1, 2, 3, 4, 5].map(star => (
                  <Icon
                    key={star}
                    name={star <= bookData.rating! ? 'star' : 'star-border'}
                    size={20}
                    color="#FFD60A"
                  />
                ))}
                <Text style={styles.ratingText}>({bookData.rating}/5)</Text>
              </View>
            </View>
          )}
        </View>
      </View>

      {/* Description */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>책 소개</Text>
        <Text style={styles.description}>{bookData.book.description}</Text>
      </View>

      {/* Quick Actions */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>빠른 작업</Text>
        <View style={styles.actionGrid}>
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => navigation.navigate('AddNote')}
          >
            <Icon name="edit" size={24} color="#007AFF" />
            <Text style={styles.actionButtonText}>노트 작성</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => navigation.navigate('Notes')}
          >
            <Icon name="note" size={24} color="#34C759" />
            <Text style={styles.actionButtonText}>노트 보기</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Chapters */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>챕터 목록</Text>
        {bookData.chapters.map((chapter) => (
          <TouchableOpacity
            key={chapter.id}
            style={styles.chapterItem}
            onPress={() => {
              // TODO: 챕터 상세 페이지로 이동
              Alert.alert('준비중', '챕터 상세 기능을 준비중입니다.');
            }}
          >
            <View style={styles.chapterInfo}>
              <Text style={styles.chapterTitle}>{chapter.title}</Text>
              <Text style={styles.chapterPages}>
                {chapter.startPage} - {chapter.endPage} 페이지
              </Text>
            </View>
            <Icon name="chevron-right" size={20} color="#C7C7CC" />
          </TouchableOpacity>
        ))}
      </View>

      {/* Danger Zone */}
      <View style={styles.dangerSection}>
        <TouchableOpacity
          style={styles.deleteButton}
          onPress={handleDeleteBook}
        >
          <Icon name="delete" size={20} color="#FF3B30" />
          <Text style={styles.deleteButtonText}>책 삭제</Text>
        </TouchableOpacity>
      </View>

      <StatusModal />
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F2F2F7',
  },
  loadingText: {
    fontSize: 16,
    color: '#8E8E93',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 60,
    paddingBottom: 20,
    backgroundColor: '#FFFFFF',
  },
  backButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#F2F2F7',
    justifyContent: 'center',
    alignItems: 'center',
  },
  moreButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#F2F2F7',
    justifyContent: 'center',
    alignItems: 'center',
  },
  bookInfoSection: {
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 20,
    paddingBottom: 30,
    alignItems: 'center',
  },
  bookCover: {
    width: 160,
    height: 220,
    borderRadius: 12,
    marginBottom: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.2,
    shadowRadius: 5,
    elevation: 5,
  },
  bookDetails: {
    width: '100%',
    alignItems: 'center',
  },
  bookTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1C1C1E',
    textAlign: 'center',
    marginBottom: 8,
  },
  bookAuthor: {
    fontSize: 18,
    color: '#8E8E93',
    marginBottom: 4,
  },
  bookPublisher: {
    fontSize: 14,
    color: '#8E8E93',
    marginBottom: 16,
  },
  bookMeta: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginBottom: 20,
  },
  metaItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 12,
  },
  metaText: {
    fontSize: 14,
    color: '#8E8E93',
    marginLeft: 4,
  },
  statusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F2F2F7',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 8,
    marginBottom: 20,
  },
  statusBadge: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    marginRight: 8,
  },
  statusText: {
    fontSize: 14,
    fontWeight: '600',
  },
  progressSection: {
    width: '100%',
    marginBottom: 20,
  },
  progressHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  progressTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  progressPercentage: {
    fontSize: 16,
    fontWeight: '600',
    color: '#007AFF',
  },
  progressBar: {
    height: 8,
    backgroundColor: '#E5E5EA',
    borderRadius: 4,
    marginBottom: 8,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#007AFF',
    borderRadius: 4,
  },
  progressText: {
    fontSize: 14,
    color: '#8E8E93',
    textAlign: 'center',
  },
  ratingSection: {
    alignItems: 'center',
  },
  ratingTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 8,
  },
  ratingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  ratingText: {
    fontSize: 14,
    color: '#8E8E93',
    marginLeft: 8,
  },
  section: {
    backgroundColor: '#FFFFFF',
    marginTop: 12,
    paddingHorizontal: 20,
    paddingVertical: 20,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginBottom: 16,
  },
  description: {
    fontSize: 16,
    color: '#1C1C1E',
    lineHeight: 24,
  },
  actionGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  actionButton: {
    flex: 1,
    backgroundColor: '#F2F2F7',
    borderRadius: 12,
    padding: 20,
    alignItems: 'center',
    marginHorizontal: 6,
  },
  actionButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1C1C1E',
    marginTop: 8,
  },
  chapterItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F2F2F7',
    borderRadius: 8,
    padding: 16,
    marginBottom: 8,
  },
  chapterInfo: {
    flex: 1,
  },
  chapterTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 4,
  },
  chapterPages: {
    fontSize: 14,
    color: '#8E8E93',
  },
  dangerSection: {
    backgroundColor: '#FFFFFF',
    marginTop: 12,
    paddingHorizontal: 20,
    paddingVertical: 20,
    marginBottom: 40,
  },
  deleteButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FF3B3020',
    borderWidth: 1,
    borderColor: '#FF3B30',
    borderRadius: 8,
    padding: 16,
  },
  deleteButtonText: {
    color: '#FF3B30',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    width: width - 40,
    maxWidth: 300,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
    textAlign: 'center',
    marginBottom: 20,
  },
  statusOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    marginBottom: 8,
  },
  statusOptionActive: {
    backgroundColor: '#007AFF20',
  },
  statusDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 12,
  },
  statusOptionText: {
    flex: 1,
    fontSize: 16,
    color: '#1C1C1E',
  },
  statusOptionTextActive: {
    fontWeight: '600',
    color: '#007AFF',
  },
  modalCancelButton: {
    backgroundColor: '#F2F2F7',
    borderRadius: 8,
    padding: 16,
    alignItems: 'center',
    marginTop: 12,
  },
  modalCancelText: {
    fontSize: 16,
    color: '#8E8E93',
    fontWeight: '600',
  },
});

export default BookDetailScreen;