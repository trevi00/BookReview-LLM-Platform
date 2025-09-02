import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { Picker } from '@react-native-picker/picker';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { useNavigation } from '@react-navigation/native';
import { AddNoteScreenNavigationProp } from '../../types/navigation';
import { NoteType } from '../../types';

interface BookSelection {
  id: number;
  title: string;
  author: string;
}

interface ChapterSelection {
  id: number;
  title: string;
  chapterNumber: number;
  startPage: number;
  endPage: number;
}

interface NoteFormData {
  content: string;
  noteType: NoteType;
  pageNumber: string;
  selectedBookId: number | null;
  selectedChapterId: number | null;
  isPrivate: boolean;
}

const AddNoteScreen = () => {
  const navigation = useNavigation<AddNoteScreenNavigationProp>();
  const [loading, setLoading] = useState(false);
  const [books, setBooks] = useState<BookSelection[]>([]);
  const [chapters, setChapters] = useState<ChapterSelection[]>([]);
  const [formData, setFormData] = useState<NoteFormData>({
    content: '',
    noteType: 'IMPRESSION',
    pageNumber: '',
    selectedBookId: null,
    selectedChapterId: null,
    isPrivate: false,
  });

  // Mock data
  const mockBooks: BookSelection[] = [
    { id: 1, title: '클린 아키텍처', author: '로버트 C. 마틴' },
    { id: 2, title: '이펙티브 자바', author: '조슈아 블로크' },
    { id: 3, title: '코틀린 인 액션', author: '드미트리 제메로프' },
  ];

  const mockChapters: ChapterSelection[] = [
    { id: 1, title: '1장. 소프트웨어 설계', chapterNumber: 1, startPage: 1, endPage: 50 },
    { id: 2, title: '2장. 아키텍처란 무엇인가', chapterNumber: 2, startPage: 51, endPage: 100 },
    { id: 3, title: '3장. 의존성 규칙', chapterNumber: 3, startPage: 101, endPage: 150 },
  ];

  const noteTypes = [
    { label: '감상', value: 'IMPRESSION' },
    { label: '요약', value: 'SUMMARY' },
    { label: '질문', value: 'QUESTION' },
    { label: '학습', value: 'LEARNING' },
    { label: '인용', value: 'QUOTE' },
  ];

  useEffect(() => {
    setBooks(mockBooks);
  }, []);

  useEffect(() => {
    if (formData.selectedBookId) {
      // TODO: API에서 해당 책의 챕터 목록 가져오기
      setChapters(mockChapters);
    } else {
      setChapters([]);
      setFormData(prev => ({ ...prev, selectedChapterId: null }));
    }
  }, [formData.selectedBookId]);

  const updateField = (field: keyof NoteFormData, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const validateForm = (): boolean => {
    const { content, selectedBookId, selectedChapterId, pageNumber } = formData;

    if (!content.trim()) {
      Alert.alert('오류', '노트 내용을 입력해주세요.');
      return false;
    }

    if (!selectedBookId) {
      Alert.alert('오류', '책을 선택해주세요.');
      return false;
    }

    if (!selectedChapterId) {
      Alert.alert('오류', '챕터를 선택해주세요.');
      return false;
    }

    if (pageNumber && isNaN(Number(pageNumber))) {
      Alert.alert('오류', '페이지 번호는 숫자로 입력해주세요.');
      return false;
    }

    // 페이지 번호 범위 검증
    if (pageNumber) {
      const selectedChapter = chapters.find(c => c.id === selectedChapterId);
      const page = Number(pageNumber);
      if (selectedChapter && (page < selectedChapter.startPage || page > selectedChapter.endPage)) {
        Alert.alert(
          '페이지 범위 오류',
          `선택한 챕터의 페이지 범위는 ${selectedChapter.startPage}~${selectedChapter.endPage} 입니다.`
        );
        return false;
      }
    }

    return true;
  };

  const handleSave = async () => {
    if (!validateForm()) return;

    setLoading(true);
    try {
      // TODO: API 호출
      // const noteData = {
      //   chapterId: formData.selectedChapterId,
      //   content: formData.content.trim(),
      //   noteType: formData.noteType,
      //   pageNumber: formData.pageNumber ? Number(formData.pageNumber) : undefined,
      //   isPrivate: formData.isPrivate,
      // };
      // await noteService.createNote(noteData);

      // Mock save for now
      setTimeout(() => {
        setLoading(false);
        Alert.alert(
          '성공',
          '노트가 저장되었습니다.',
          [
            {
              text: '확인',
              onPress: () => navigation.goBack(),
            },
          ]
        );
      }, 1000);
    } catch (error) {
      setLoading(false);
      Alert.alert('오류', '노트 저장에 실패했습니다. 다시 시도해주세요.');
    }
  };

  const handleAIFeedback = () => {
    if (!formData.content.trim()) {
      Alert.alert('안내', '노트 내용을 먼저 작성해주세요.');
      return;
    }

    // TODO: AI 피드백 요청
    Alert.alert('준비중', 'AI 피드백 기능을 준비중입니다.');
  };

  const getSelectedChapter = () => {
    return chapters.find(c => c.id === formData.selectedChapterId);
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView style={styles.scrollContainer} showsVerticalScrollIndicator={false}>
        <View style={styles.form}>
          {/* 책 & 챕터 선택 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>책 및 챕터 선택</Text>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>책 선택 *</Text>
              <View style={styles.pickerContainer}>
                <Picker
                  selectedValue={formData.selectedBookId}
                  onValueChange={(value) => updateField('selectedBookId', value)}
                  style={styles.picker}
                >
                  <Picker.Item label="책을 선택하세요" value={null} />
                  {books.map((book) => (
                    <Picker.Item
                      key={book.id}
                      label={`${book.title} - ${book.author}`}
                      value={book.id}
                    />
                  ))}
                </Picker>
              </View>
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>챕터 선택 *</Text>
              <View style={styles.pickerContainer}>
                <Picker
                  selectedValue={formData.selectedChapterId}
                  onValueChange={(value) => updateField('selectedChapterId', value)}
                  style={styles.picker}
                  enabled={chapters.length > 0}
                >
                  <Picker.Item 
                    label={chapters.length > 0 ? "챕터를 선택하세요" : "먼저 책을 선택하세요"} 
                    value={null} 
                  />
                  {chapters.map((chapter) => (
                    <Picker.Item
                      key={chapter.id}
                      label={`${chapter.title} (${chapter.startPage}-${chapter.endPage}p)`}
                      value={chapter.id}
                    />
                  ))}
                </Picker>
              </View>
            </View>

            <View style={styles.rowInputs}>
              <View style={[styles.inputContainer, { flex: 1, marginRight: 8 }]}>
                <Text style={styles.label}>노트 타입</Text>
                <View style={styles.pickerContainer}>
                  <Picker
                    selectedValue={formData.noteType}
                    onValueChange={(value) => updateField('noteType', value)}
                    style={styles.picker}
                  >
                    {noteTypes.map((type) => (
                      <Picker.Item
                        key={type.value}
                        label={type.label}
                        value={type.value}
                      />
                    ))}
                  </Picker>
                </View>
              </View>

              <View style={[styles.inputContainer, { flex: 1, marginLeft: 8 }]}>
                <Text style={styles.label}>페이지</Text>
                <TextInput
                  style={styles.input}
                  placeholder={
                    getSelectedChapter() 
                      ? `${getSelectedChapter()!.startPage}-${getSelectedChapter()!.endPage}`
                      : "페이지"
                  }
                  value={formData.pageNumber}
                  onChangeText={(value) => updateField('pageNumber', value)}
                  keyboardType="numeric"
                />
              </View>
            </View>
          </View>

          {/* 노트 내용 */}
          <View style={styles.section}>
            <View style={styles.contentHeader}>
              <Text style={styles.sectionTitle}>노트 내용</Text>
              <TouchableOpacity
                style={styles.aiButton}
                onPress={handleAIFeedback}
              >
                <Icon name="auto-awesome" size={16} color="#FF9500" />
                <Text style={styles.aiButtonText}>AI 도움</Text>
              </TouchableOpacity>
            </View>

            <View style={styles.inputContainer}>
              <TextInput
                style={[styles.input, styles.textArea]}
                placeholder="독서하면서 느낀 생각, 궁금한 점, 인상 깊은 내용 등을 자유롭게 작성해보세요."
                value={formData.content}
                onChangeText={(value) => updateField('content', value)}
                multiline
                numberOfLines={8}
                textAlignVertical="top"
              />
              <Text style={styles.charCount}>
                {formData.content.length} / 1000
              </Text>
            </View>
          </View>

          {/* 설정 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>공개 설정</Text>
            <TouchableOpacity
              style={styles.toggleContainer}
              onPress={() => updateField('isPrivate', !formData.isPrivate)}
            >
              <View style={styles.toggleInfo}>
                <Icon 
                  name={formData.isPrivate ? 'lock' : 'public'} 
                  size={20} 
                  color={formData.isPrivate ? '#FF9500' : '#34C759'} 
                />
                <View style={styles.toggleText}>
                  <Text style={styles.toggleTitle}>
                    {formData.isPrivate ? '비공개' : '공개'}
                  </Text>
                  <Text style={styles.toggleSubtitle}>
                    {formData.isPrivate 
                      ? '나만 볼 수 있는 노트입니다' 
                      : '다른 사용자도 볼 수 있는 노트입니다'
                    }
                  </Text>
                </View>
              </View>
              <Icon
                name={formData.isPrivate ? 'toggle-on' : 'toggle-off'}
                size={32}
                color={formData.isPrivate ? '#007AFF' : '#C7C7CC'}
              />
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>

      {/* 하단 버튼 */}
      <View style={styles.bottomContainer}>
        <TouchableOpacity
          style={[styles.saveButton, loading && styles.saveButtonDisabled]}
          onPress={handleSave}
          disabled={loading}
        >
          <Icon name="check" size={20} color="#FFFFFF" />
          <Text style={styles.saveButtonText}>
            {loading ? '저장 중...' : '노트 저장'}
          </Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  scrollContainer: {
    flex: 1,
  },
  form: {
    padding: 20,
  },
  section: {
    marginBottom: 32,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginBottom: 16,
  },
  contentHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  aiButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFF9E6',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#FFE066',
  },
  aiButtonText: {
    color: '#FF9500',
    fontSize: 12,
    fontWeight: '600',
    marginLeft: 4,
  },
  inputContainer: {
    marginBottom: 20,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 8,
  },
  input: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E5E5EA',
    borderRadius: 8,
    padding: 16,
    fontSize: 16,
    color: '#1C1C1E',
  },
  textArea: {
    height: 120,
    marginBottom: 8,
  },
  charCount: {
    fontSize: 12,
    color: '#8E8E93',
    textAlign: 'right',
  },
  pickerContainer: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E5E5EA',
    borderRadius: 8,
    overflow: 'hidden',
  },
  picker: {
    height: 50,
  },
  rowInputs: {
    flexDirection: 'row',
    alignItems: 'flex-end',
  },
  toggleContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#E5E5EA',
    borderRadius: 8,
    padding: 16,
  },
  toggleInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  toggleText: {
    marginLeft: 12,
  },
  toggleTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  toggleSubtitle: {
    fontSize: 14,
    color: '#8E8E93',
    marginTop: 2,
  },
  bottomContainer: {
    padding: 20,
    backgroundColor: '#FFFFFF',
    borderTopWidth: 1,
    borderTopColor: '#E5E5EA',
  },
  saveButton: {
    backgroundColor: '#007AFF',
    borderRadius: 12,
    padding: 16,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
  saveButtonDisabled: {
    backgroundColor: '#C7C7CC',
  },
  saveButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
});

export default AddNoteScreen;