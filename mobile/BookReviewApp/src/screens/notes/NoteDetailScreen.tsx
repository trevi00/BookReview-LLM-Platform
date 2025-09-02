import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Alert,
  Modal,
  TextInput,
  Dimensions,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NoteDetailScreenNavigationProp } from '../../types/navigation';
import { ReadingNote, NoteType, AiFeedback } from '../../types';

const { width } = Dimensions.get('window');

interface NoteDetailRouteParams {
  noteId: number;
}

interface NoteDetailData extends ReadingNote {
  chapter: {
    title: string;
    chapterNumber: number;
    userBook: {
      book: {
        title: string;
        author: string;
      };
    };
  };
  aiFeedbacks: AiFeedback[];
}

const NoteDetailScreen = () => {
  const navigation = useNavigation<NoteDetailScreenNavigationProp>();
  const route = useRoute<RouteProp<{ params: NoteDetailRouteParams }, 'params'>>();
  const { noteId } = route.params;
  
  const [noteData, setNoteData] = useState<NoteDetailData | null>(null);
  const [loading, setLoading] = useState(true);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [aiModalVisible, setAiModalVisible] = useState(false);
  const [feedbackText, setFeedbackText] = useState('');
  const [feedbackLoading, setFeedbackLoading] = useState(false);

  // Mock data
  const mockNoteData: NoteDetailData = {
    id: 1,
    chapterId: 1,
    userId: 1,
    content: '클린 아키텍처의 핵심 원칙들을 이해했습니다. 의존성 역전 원칙이 특히 인상깊었어요. 소프트웨어 설계에서 추상화에 의존하고 구체적인 구현에 의존하지 않는다는 개념이 정말 중요한 것 같습니다.',
    noteType: 'IMPRESSION',
    pageNumber: 45,
    isPrivate: false,
    createdAt: new Date('2024-01-15'),
    updatedAt: new Date('2024-01-15'),
    chapter: {
      title: '1장. 소프트웨어 설계',
      chapterNumber: 1,
      userBook: {
        book: {
          title: '클린 아키텍처',
          author: '로버트 C. 마틴',
        },
      },
    },
    aiFeedbacks: [
      {
        id: 1,
        noteId: 1,
        content: '의존성 역전 원칙에 대한 좋은 이해를 보여주셨네요! 이를 실제 코드에서 어떻게 적용할 수 있는지 생각해보시면 좋을 것 같습니다. 예를 들어, 인터페이스를 통한 추상화나 의존성 주입 패턴 등을 고려해보세요.',
        createdAt: new Date('2024-01-15'),
      },
      {
        id: 2,
        noteId: 1,
        content: '추가로, SOLID 원칙의 다른 원칙들과 의존성 역전 원칙이 어떻게 연관되는지도 탐구해보시길 추천합니다. 특히 단일 책임 원칙과 개방-폐쇄 원칙과의 관계를 살펴보면 더 깊은 이해를 얻으실 수 있을 것입니다.',
        createdAt: new Date('2024-01-16'),
      },
    ],
  };

  useEffect(() => {
    loadNoteData();
  }, [noteId]);

  const loadNoteData = async () => {
    setLoading(true);
    try {
      // TODO: API 호출
      // const response = await noteService.getNoteDetail(noteId);
      // setNoteData(response.data);
      
      // Mock data for now
      setTimeout(() => {
        setNoteData(mockNoteData);
        setLoading(false);
      }, 500);
    } catch (error) {
      setLoading(false);
      Alert.alert('오류', '노트 정보를 불러오는데 실패했습니다.');
    }
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
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const handleEditNote = () => {
    // TODO: 노트 편집 화면으로 이동
    Alert.alert('준비중', '노트 편집 기능을 준비중입니다.');
  };

  const handleDeleteNote = () => {
    Alert.alert(
      '노트 삭제',
      '정말로 이 노트를 삭제하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: () => {
            // TODO: API 호출로 노트 삭제
            Alert.alert('성공', '노트가 삭제되었습니다.', [
              { text: '확인', onPress: () => navigation.goBack() },
            ]);
          },
        },
      ]
    );
  };

  const handleRequestAIFeedback = async () => {
    if (!noteData) return;
    
    setFeedbackLoading(true);
    try {
      // TODO: AI 피드백 API 호출
      // const response = await aiService.requestFeedback(noteData.id);
      
      // Mock feedback for now
      setTimeout(() => {
        const newFeedback: AiFeedback = {
          id: Date.now(),
          noteId: noteData.id,
          content: '이 노트에서 의존성 역전 원칙에 대한 이해가 잘 드러나네요. 실제 프로젝트에서 이를 적용해보시면서 어떤 장점과 어려움이 있는지 경험해보시길 추천합니다.',
          createdAt: new Date(),
        };
        
        setNoteData(prev => prev ? {
          ...prev,
          aiFeedbacks: [...prev.aiFeedbacks, newFeedback]
        } : null);
        
        setFeedbackLoading(false);
        setAiModalVisible(false);
        Alert.alert('성공', 'AI 피드백이 추가되었습니다.');
      }, 2000);
    } catch (error) {
      setFeedbackLoading(false);
      Alert.alert('오류', 'AI 피드백 요청에 실패했습니다.');
    }
  };

  const handleSendFeedback = async () => {
    if (!feedbackText.trim()) {
      Alert.alert('안내', '피드백 내용을 입력해주세요.');
      return;
    }

    setFeedbackLoading(true);
    try {
      // TODO: 사용자 피드백 API 호출
      setTimeout(() => {
        setFeedbackLoading(false);
        setFeedbackText('');
        setAiModalVisible(false);
        Alert.alert('성공', '피드백이 전송되었습니다.');
      }, 1000);
    } catch (error) {
      setFeedbackLoading(false);
      Alert.alert('오류', '피드백 전송에 실패했습니다.');
    }
  };

  const AIFeedbackModal = () => (
    <Modal
      visible={aiModalVisible}
      transparent
      animationType="slide"
      onRequestClose={() => setAiModalVisible(false)}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.aiModalContent}>
          <View style={styles.aiModalHeader}>
            <Icon name="auto-awesome" size={24} color="#FF9500" />
            <Text style={styles.aiModalTitle}>AI 피드백</Text>
            <TouchableOpacity onPress={() => setAiModalVisible(false)}>
              <Icon name="close" size={24} color="#8E8E93" />
            </TouchableOpacity>
          </View>

          <Text style={styles.aiModalDescription}>
            AI가 노트 내용을 분석하여 더 나은 학습을 위한 피드백을 제공합니다.
          </Text>

          <View style={styles.aiModalActions}>
            <TouchableOpacity
              style={[styles.aiActionButton, { backgroundColor: '#FF9500' }]}
              onPress={handleRequestAIFeedback}
              disabled={feedbackLoading}
            >
              <Icon name="auto-awesome" size={20} color="#FFFFFF" />
              <Text style={styles.aiActionButtonText}>
                {feedbackLoading ? 'AI 분석 중...' : 'AI 피드백 받기'}
              </Text>
            </TouchableOpacity>
          </View>

          <View style={styles.divider}>
            <View style={styles.dividerLine} />
            <Text style={styles.dividerText}>또는</Text>
            <View style={styles.dividerLine} />
          </View>

          <Text style={styles.feedbackLabel}>직접 피드백 요청하기</Text>
          <TextInput
            style={styles.feedbackInput}
            placeholder="구체적인 질문이나 요청사항을 입력해주세요..."
            value={feedbackText}
            onChangeText={setFeedbackText}
            multiline
            numberOfLines={3}
            textAlignVertical="top"
          />

          <TouchableOpacity
            style={[styles.aiActionButton, { backgroundColor: '#007AFF' }]}
            onPress={handleSendFeedback}
            disabled={feedbackLoading || !feedbackText.trim()}
          >
            <Icon name="send" size={20} color="#FFFFFF" />
            <Text style={styles.aiActionButtonText}>
              {feedbackLoading ? '전송 중...' : '피드백 요청'}
            </Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );

  if (loading || !noteData) {
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
        <Text style={styles.headerTitle}>노트 상세</Text>
        <TouchableOpacity
          style={styles.moreButton}
          onPress={handleEditNote}
        >
          <Icon name="edit" size={24} color="#1C1C1E" />
        </TouchableOpacity>
      </View>

      {/* Book & Chapter Info */}
      <View style={styles.bookInfoSection}>
        <View style={styles.bookInfo}>
          <Text style={styles.bookTitle}>{noteData.chapter.userBook.book.title}</Text>
          <Text style={styles.bookAuthor}>{noteData.chapter.userBook.book.author}</Text>
          <Text style={styles.chapterTitle}>{noteData.chapter.title}</Text>
        </View>
      </View>

      {/* Note Content */}
      <View style={styles.noteSection}>
        <View style={styles.noteHeader}>
          <View style={styles.noteMetaLeft}>
            <View style={[
              styles.typeBadge,
              { backgroundColor: getNoteTypeColor(noteData.noteType) + '20' }
            ]}>
              <Text style={[
                styles.typeText,
                { color: getNoteTypeColor(noteData.noteType) }
              ]}>
                {getNoteTypeText(noteData.noteType)}
              </Text>
            </View>
            <Text style={styles.pageNumber}>p. {noteData.pageNumber}</Text>
          </View>
          <View style={styles.noteMetaRight}>
            {noteData.isPrivate && (
              <Icon name="lock" size={16} color="#8E8E93" style={styles.privateIcon} />
            )}
            <Text style={styles.noteDate}>{formatDate(noteData.createdAt)}</Text>
          </View>
        </View>

        <Text style={styles.noteContent}>{noteData.content}</Text>
      </View>

      {/* AI Feedback Section */}
      <View style={styles.feedbackSection}>
        <View style={styles.feedbackHeader}>
          <Text style={styles.sectionTitle}>AI 피드백</Text>
          <TouchableOpacity
            style={styles.addFeedbackButton}
            onPress={() => setAiModalVisible(true)}
          >
            <Icon name="add" size={20} color="#FF9500" />
            <Text style={styles.addFeedbackText}>피드백 요청</Text>
          </TouchableOpacity>
        </View>

        {noteData.aiFeedbacks.length > 0 ? (
          noteData.aiFeedbacks.map((feedback) => (
            <View key={feedback.id} style={styles.feedbackItem}>
              <View style={styles.feedbackItemHeader}>
                <View style={styles.aiIcon}>
                  <Icon name="auto-awesome" size={16} color="#FF9500" />
                </View>
                <Text style={styles.feedbackDate}>
                  {formatDate(feedback.createdAt)}
                </Text>
              </View>
              <Text style={styles.feedbackContent}>{feedback.content}</Text>
            </View>
          ))
        ) : (
          <View style={styles.emptyFeedback}>
            <Icon name="auto-awesome" size={48} color="#C7C7CC" />
            <Text style={styles.emptyFeedbackTitle}>AI 피드백이 없습니다</Text>
            <Text style={styles.emptyFeedbackSubtitle}>
              AI의 도움을 받아 더 깊이 있는 학습을 해보세요
            </Text>
          </View>
        )}
      </View>

      {/* Quick Actions */}
      <View style={styles.actionsSection}>
        <Text style={styles.sectionTitle}>작업</Text>
        <View style={styles.actionGrid}>
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => navigation.navigate('Notes')}
          >
            <Icon name="note" size={24} color="#007AFF" />
            <Text style={styles.actionButtonText}>다른 노트 보기</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.actionButton}
            onPress={() => navigation.navigate('AddNote')}
          >
            <Icon name="add" size={24} color="#34C759" />
            <Text style={styles.actionButtonText}>새 노트 작성</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Danger Zone */}
      <View style={styles.dangerSection}>
        <TouchableOpacity
          style={styles.deleteButton}
          onPress={handleDeleteNote}
        >
          <Icon name="delete" size={20} color="#FF3B30" />
          <Text style={styles.deleteButtonText}>노트 삭제</Text>
        </TouchableOpacity>
      </View>

      <AIFeedbackModal />
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
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
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
    paddingVertical: 16,
  },
  bookInfo: {
    alignItems: 'center',
  },
  bookTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1C1C1E',
    textAlign: 'center',
    marginBottom: 4,
  },
  bookAuthor: {
    fontSize: 16,
    color: '#8E8E93',
    marginBottom: 8,
  },
  chapterTitle: {
    fontSize: 14,
    color: '#007AFF',
    fontWeight: '600',
  },
  noteSection: {
    backgroundColor: '#FFFFFF',
    marginTop: 12,
    paddingHorizontal: 20,
    paddingVertical: 20,
  },
  noteHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  noteMetaLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  noteMetaRight: {
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
  privateIcon: {
    marginRight: 4,
  },
  noteDate: {
    fontSize: 12,
    color: '#8E8E93',
  },
  noteContent: {
    fontSize: 16,
    color: '#1C1C1E',
    lineHeight: 24,
  },
  feedbackSection: {
    backgroundColor: '#FFFFFF',
    marginTop: 12,
    paddingHorizontal: 20,
    paddingVertical: 20,
  },
  feedbackHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1C1C1E',
  },
  addFeedbackButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFF9E6',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#FFE066',
  },
  addFeedbackText: {
    color: '#FF9500',
    fontSize: 12,
    fontWeight: '600',
    marginLeft: 4,
  },
  feedbackItem: {
    backgroundColor: '#FFF9E6',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#FFE066',
  },
  feedbackItemHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  aiIcon: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#FF9500',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 8,
  },
  feedbackDate: {
    fontSize: 12,
    color: '#8E8E93',
  },
  feedbackContent: {
    fontSize: 14,
    color: '#1C1C1E',
    lineHeight: 20,
  },
  emptyFeedback: {
    alignItems: 'center',
    paddingVertical: 40,
  },
  emptyFeedbackTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginTop: 16,
    marginBottom: 8,
  },
  emptyFeedbackSubtitle: {
    fontSize: 14,
    color: '#8E8E93',
    textAlign: 'center',
  },
  actionsSection: {
    backgroundColor: '#FFFFFF',
    marginTop: 12,
    paddingHorizontal: 20,
    paddingVertical: 20,
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
    justifyContent: 'flex-end',
  },
  aiModalContent: {
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    maxHeight: '80%',
  },
  aiModalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  aiModalTitle: {
    flex: 1,
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginLeft: 8,
  },
  aiModalDescription: {
    fontSize: 14,
    color: '#8E8E93',
    lineHeight: 20,
    marginBottom: 20,
  },
  aiModalActions: {
    marginBottom: 20,
  },
  aiActionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
  },
  aiActionButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 20,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: '#E5E5EA',
  },
  dividerText: {
    fontSize: 14,
    color: '#8E8E93',
    marginHorizontal: 16,
  },
  feedbackLabel: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 8,
  },
  feedbackInput: {
    backgroundColor: '#F2F2F7',
    borderRadius: 8,
    padding: 16,
    fontSize: 16,
    color: '#1C1C1E',
    height: 80,
    marginBottom: 16,
    textAlignVertical: 'top',
  },
});

export default NoteDetailScreen;