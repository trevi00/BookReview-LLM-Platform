import React, { useState } from 'react';
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
import { AddBookScreenNavigationProp } from '../../types/navigation';
import { BookCategory } from '../../types';

interface BookFormData {
  title: string;
  author: string;
  publisher: string;
  isbn: string;
  publishedYear: string;
  totalPages: string;
  category: BookCategory;
  description: string;
}

const AddBookScreen = () => {
  const navigation = useNavigation<AddBookScreenNavigationProp>();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<BookFormData>({
    title: '',
    author: '',
    publisher: '',
    isbn: '',
    publishedYear: '',
    totalPages: '',
    category: 'OTHER',
    description: '',
  });

  const categories = [
    { label: '소설', value: 'FICTION' },
    { label: '비문학', value: 'NON_FICTION' },
    { label: '기술/IT', value: 'TECHNOLOGY' },
    { label: '과학', value: 'SCIENCE' },
    { label: '역사', value: 'HISTORY' },
    { label: '자기계발', value: 'SELF_HELP' },
    { label: '경영/비즈니스', value: 'BUSINESS' },
    { label: '교육', value: 'EDUCATION' },
    { label: '건강', value: 'HEALTH' },
    { label: '여행', value: 'TRAVEL' },
    { label: '요리', value: 'COOKING' },
    { label: '예술', value: 'ART' },
    { label: '철학', value: 'PHILOSOPHY' },
    { label: '심리학', value: 'PSYCHOLOGY' },
    { label: '기타', value: 'OTHER' },
  ];

  const updateField = (field: keyof BookFormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const validateForm = (): boolean => {
    const { title, author } = formData;

    if (!title.trim()) {
      Alert.alert('오류', '책 제목을 입력해주세요.');
      return false;
    }

    if (!author.trim()) {
      Alert.alert('오류', '저자를 입력해주세요.');
      return false;
    }

    if (formData.totalPages && isNaN(Number(formData.totalPages))) {
      Alert.alert('오류', '페이지 수는 숫자로 입력해주세요.');
      return false;
    }

    if (formData.publishedYear && isNaN(Number(formData.publishedYear))) {
      Alert.alert('오류', '출간년도는 숫자로 입력해주세요.');
      return false;
    }

    return true;
  };

  const handleSave = async () => {
    if (!validateForm()) return;

    setLoading(true);
    try {
      // TODO: API 호출
      // const bookData = {
      //   ...formData,
      //   totalPages: formData.totalPages ? Number(formData.totalPages) : undefined,
      //   publishedYear: formData.publishedYear ? Number(formData.publishedYear) : undefined,
      // };
      // await bookService.createBook(bookData);

      // Mock save for now
      setTimeout(() => {
        setLoading(false);
        Alert.alert(
          '성공',
          '책이 추가되었습니다.',
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
      Alert.alert('오류', '책 추가에 실패했습니다. 다시 시도해주세요.');
    }
  };

  const handleISBNScan = () => {
    // TODO: 바코드 스캐너 구현
    Alert.alert('준비중', 'ISBN 스캔 기능을 준비중입니다.');
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView style={styles.scrollContainer} showsVerticalScrollIndicator={false}>
        <View style={styles.form}>
          {/* 기본 정보 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>기본 정보</Text>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>책 제목 *</Text>
              <TextInput
                style={styles.input}
                placeholder="책 제목을 입력하세요"
                value={formData.title}
                onChangeText={(value) => updateField('title', value)}
              />
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>저자 *</Text>
              <TextInput
                style={styles.input}
                placeholder="저자명을 입력하세요"
                value={formData.author}
                onChangeText={(value) => updateField('author', value)}
              />
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>출판사</Text>
              <TextInput
                style={styles.input}
                placeholder="출판사를 입력하세요"
                value={formData.publisher}
                onChangeText={(value) => updateField('publisher', value)}
              />
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>카테고리</Text>
              <View style={styles.pickerContainer}>
                <Picker
                  selectedValue={formData.category}
                  onValueChange={(value) => updateField('category', value)}
                  style={styles.picker}
                >
                  {categories.map((category) => (
                    <Picker.Item
                      key={category.value}
                      label={category.label}
                      value={category.value}
                    />
                  ))}
                </Picker>
              </View>
            </View>
          </View>

          {/* 상세 정보 */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>상세 정보</Text>

            <View style={styles.inputContainer}>
              <View style={styles.labelRow}>
                <Text style={styles.label}>ISBN</Text>
                <TouchableOpacity
                  style={styles.scanButton}
                  onPress={handleISBNScan}
                >
                  <Icon name="qr-code-scanner" size={20} color="#007AFF" />
                  <Text style={styles.scanButtonText}>스캔</Text>
                </TouchableOpacity>
              </View>
              <TextInput
                style={styles.input}
                placeholder="ISBN을 입력하거나 스캔하세요"
                value={formData.isbn}
                onChangeText={(value) => updateField('isbn', value)}
              />
            </View>

            <View style={styles.rowInputs}>
              <View style={[styles.inputContainer, { flex: 1, marginRight: 8 }]}>
                <Text style={styles.label}>출간년도</Text>
                <TextInput
                  style={styles.input}
                  placeholder="2024"
                  value={formData.publishedYear}
                  onChangeText={(value) => updateField('publishedYear', value)}
                  keyboardType="numeric"
                />
              </View>

              <View style={[styles.inputContainer, { flex: 1, marginLeft: 8 }]}>
                <Text style={styles.label}>총 페이지</Text>
                <TextInput
                  style={styles.input}
                  placeholder="300"
                  value={formData.totalPages}
                  onChangeText={(value) => updateField('totalPages', value)}
                  keyboardType="numeric"
                />
              </View>
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.label}>책 소개</Text>
              <TextInput
                style={[styles.input, styles.textArea]}
                placeholder="책에 대한 간단한 소개를 입력하세요"
                value={formData.description}
                onChangeText={(value) => updateField('description', value)}
                multiline
                numberOfLines={4}
                textAlignVertical="top"
              />
            </View>
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
            {loading ? '저장 중...' : '책 추가'}
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
  inputContainer: {
    marginBottom: 20,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 8,
  },
  labelRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
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
    height: 100,
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
  scanButton: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#007AFF20',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
  },
  scanButtonText: {
    color: '#007AFF',
    fontSize: 14,
    fontWeight: '600',
    marginLeft: 4,
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

export default AddBookScreen;