import React, { useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Image,
  Alert,
  Switch,
  Modal,
  TextInput,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { useNavigation } from '@react-navigation/native';
import { ProfileScreenNavigationProp } from '../../types/navigation';

interface UserProfile {
  id: number;
  email: string;
  username: string;
  nickname: string;
  profileImageUrl?: string;
  bio: string;
  joinDate: Date;
  readingGoal: number;
  isPrivate: boolean;
}

interface AppSettings {
  notifications: boolean;
  darkMode: boolean;
  autoBackup: boolean;
  offlineReading: boolean;
}

const ProfileScreen = () => {
  const navigation = useNavigation<ProfileScreenNavigationProp>();
  
  const [userProfile] = useState<UserProfile>({
    id: 1,
    email: 'user@example.com',
    username: 'bookworm',
    nickname: '독서왕',
    profileImageUrl: 'https://via.placeholder.com/150',
    bio: '책을 사랑하는 개발자입니다. 매일 새로운 지식을 탐구하며 성장하고 있습니다.',
    joinDate: new Date('2024-01-15'),
    readingGoal: 24,
    isPrivate: false,
  });

  const [settings, setSettings] = useState<AppSettings>({
    notifications: true,
    darkMode: false,
    autoBackup: true,
    offlineReading: false,
  });

  const [goalModalVisible, setGoalModalVisible] = useState(false);
  const [newGoal, setNewGoal] = useState(userProfile.readingGoal.toString());

  const formatJoinDate = (date: Date) => {
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const updateSetting = (key: keyof AppSettings, value: boolean) => {
    setSettings(prev => ({ ...prev, [key]: value }));
    // TODO: API 호출로 설정 저장
  };

  const handleUpdateGoal = () => {
    const goal = parseInt(newGoal);
    if (isNaN(goal) || goal < 1 || goal > 365) {
      Alert.alert('오류', '올바른 목표 권수를 입력해주세요. (1-365)');
      return;
    }
    
    // TODO: API 호출로 목표 업데이트
    setGoalModalVisible(false);
    Alert.alert('성공', '독서 목표가 업데이트되었습니다.');
  };

  const handleEditProfile = () => {
    // TODO: 프로필 편집 화면으로 이동
    Alert.alert('준비중', '프로필 편집 기능을 준비중입니다.');
  };

  const handleExportData = () => {
    Alert.alert(
      '데이터 내보내기',
      '독서 기록과 노트를 JSON 파일로 내보내시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '내보내기',
          onPress: () => {
            // TODO: 데이터 내보내기 기능 구현
            Alert.alert('성공', '데이터가 내보내기되었습니다.');
          },
        },
      ]
    );
  };

  const handleBackupRestore = () => {
    Alert.alert(
      '백업 및 복원',
      '어떤 작업을 수행하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '백업',
          onPress: () => {
            // TODO: 백업 기능 구현
            Alert.alert('성공', '데이터가 백업되었습니다.');
          },
        },
        {
          text: '복원',
          onPress: () => {
            // TODO: 복원 기능 구현
            Alert.alert('준비중', '복원 기능을 준비중입니다.');
          },
        },
      ]
    );
  };

  const handleLogout = () => {
    Alert.alert(
      '로그아웃',
      '정말로 로그아웃하시겠습니까?',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '로그아웃',
          style: 'destructive',
          onPress: () => {
            // TODO: 로그아웃 처리
            navigation.navigate('Login');
          },
        },
      ]
    );
  };

  const handleDeleteAccount = () => {
    Alert.alert(
      '계정 삭제',
      '정말로 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: () => {
            Alert.alert(
              '최종 확인',
              '모든 데이터가 영구적으로 삭제됩니다. 계속하시겠습니까?',
              [
                { text: '취소', style: 'cancel' },
                {
                  text: '삭제',
                  style: 'destructive',
                  onPress: () => {
                    // TODO: 계정 삭제 API 호출
                    Alert.alert('완료', '계정이 삭제되었습니다.');
                  },
                },
              ]
            );
          },
        },
      ]
    );
  };

  const GoalModal = () => (
    <Modal
      visible={goalModalVisible}
      transparent
      animationType="fade"
      onRequestClose={() => setGoalModalVisible(false)}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.modalTitle}>연간 독서 목표 설정</Text>
          
          <View style={styles.goalInputContainer}>
            <TextInput
              style={styles.goalInput}
              value={newGoal}
              onChangeText={setNewGoal}
              keyboardType="numeric"
              maxLength={3}
              textAlign="center"
            />
            <Text style={styles.goalUnit}>권</Text>
          </View>
          
          <Text style={styles.goalDescription}>
            올해 읽고 싶은 책의 목표 권수를 설정해주세요.
          </Text>
          
          <View style={styles.modalButtons}>
            <TouchableOpacity
              style={[styles.modalButton, styles.modalCancelButton]}
              onPress={() => setGoalModalVisible(false)}
            >
              <Text style={styles.modalCancelText}>취소</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.modalButton, styles.modalConfirmButton]}
              onPress={handleUpdateGoal}
            >
              <Text style={styles.modalConfirmText}>저장</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );

  const SettingItem = ({ 
    icon, 
    title, 
    subtitle, 
    value, 
    onToggle, 
    showSwitch = false, 
    onPress 
  }: {
    icon: string;
    title: string;
    subtitle?: string;
    value?: boolean;
    onToggle?: (value: boolean) => void;
    showSwitch?: boolean;
    onPress?: () => void;
  }) => (
    <TouchableOpacity
      style={styles.settingItem}
      onPress={onPress}
      disabled={showSwitch}
    >
      <View style={styles.settingLeft}>
        <Icon name={icon} size={24} color="#8E8E93" />
        <View style={styles.settingText}>
          <Text style={styles.settingTitle}>{title}</Text>
          {subtitle && <Text style={styles.settingSubtitle}>{subtitle}</Text>}
        </View>
      </View>
      {showSwitch ? (
        <Switch
          value={value}
          onValueChange={onToggle}
          trackColor={{ false: '#E5E5EA', true: '#007AFF' }}
          thumbColor="#FFFFFF"
        />
      ) : (
        <Icon name="chevron-right" size={20} color="#C7C7CC" />
      )}
    </TouchableOpacity>
  );

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>프로필</Text>
        <TouchableOpacity
          style={styles.editButton}
          onPress={handleEditProfile}
        >
          <Icon name="edit" size={24} color="#007AFF" />
        </TouchableOpacity>
      </View>

      {/* Profile Section */}
      <View style={styles.profileSection}>
        <Image
          source={{ uri: userProfile.profileImageUrl || 'https://via.placeholder.com/150' }}
          style={styles.profileImage}
        />
        <Text style={styles.nickname}>{userProfile.nickname}</Text>
        <Text style={styles.username}>@{userProfile.username}</Text>
        <Text style={styles.email}>{userProfile.email}</Text>
        
        {userProfile.bio && (
          <Text style={styles.bio}>{userProfile.bio}</Text>
        )}
        
        <Text style={styles.joinDate}>
          {formatJoinDate(userProfile.joinDate)} 가입
        </Text>
      </View>

      {/* Reading Goal */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>독서 목표</Text>
        <TouchableOpacity
          style={styles.goalItem}
          onPress={() => setGoalModalVisible(true)}
        >
          <View style={styles.goalLeft}>
            <Icon name="flag" size={24} color="#FF9500" />
            <View style={styles.goalText}>
              <Text style={styles.goalTitle}>연간 독서 목표</Text>
              <Text style={styles.goalSubtitle}>{userProfile.readingGoal}권</Text>
            </View>
          </View>
          <Icon name="edit" size={20} color="#8E8E93" />
        </TouchableOpacity>
      </View>

      {/* Quick Actions */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>빠른 작업</Text>
        <SettingItem
          icon="bar-chart"
          title="독서 통계"
          subtitle="나의 독서 기록을 확인하세요"
          onPress={() => navigation.navigate('Statistics')}
        />
        <SettingItem
          icon="note"
          title="내 노트"
          subtitle="작성한 독서 노트를 모아보세요"
          onPress={() => navigation.navigate('Notes')}
        />
        <SettingItem
          icon="book"
          title="내 서재"
          subtitle="등록한 책들을 관리하세요"
          onPress={() => navigation.navigate('Books')}
        />
      </View>

      {/* App Settings */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>앱 설정</Text>
        <SettingItem
          icon="notifications"
          title="알림"
          subtitle="독서 알림 및 목표 리마인더"
          value={settings.notifications}
          onToggle={(value) => updateSetting('notifications', value)}
          showSwitch
        />
        <SettingItem
          icon="dark-mode"
          title="다크 모드"
          subtitle="어두운 테마 사용"
          value={settings.darkMode}
          onToggle={(value) => updateSetting('darkMode', value)}
          showSwitch
        />
        <SettingItem
          icon="cloud-upload"
          title="자동 백업"
          subtitle="클라우드에 데이터 자동 저장"
          value={settings.autoBackup}
          onToggle={(value) => updateSetting('autoBackup', value)}
          showSwitch
        />
        <SettingItem
          icon="offline-pin"
          title="오프라인 읽기"
          subtitle="인터넷 없이도 노트 작성 가능"
          value={settings.offlineReading}
          onToggle={(value) => updateSetting('offlineReading', value)}
          showSwitch
        />
      </View>

      {/* Data Management */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>데이터 관리</Text>
        <SettingItem
          icon="download"
          title="데이터 내보내기"
          subtitle="독서 기록을 JSON 파일로 저장"
          onPress={handleExportData}
        />
        <SettingItem
          icon="backup"
          title="백업 및 복원"
          subtitle="데이터 백업 또는 복원"
          onPress={handleBackupRestore}
        />
      </View>

      {/* Support */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>지원</Text>
        <SettingItem
          icon="help"
          title="도움말"
          subtitle="앱 사용법 및 FAQ"
          onPress={() => Alert.alert('준비중', '도움말 페이지를 준비중입니다.')}
        />
        <SettingItem
          icon="feedback"
          title="피드백 보내기"
          subtitle="개선 사항이나 버그 신고"
          onPress={() => Alert.alert('준비중', '피드백 기능을 준비중입니다.')}
        />
        <SettingItem
          icon="info"
          title="앱 정보"
          subtitle="버전 1.0.0"
          onPress={() => Alert.alert('BookReview', '버전 1.0.0\n개발자: BookReview Team')}
        />
      </View>

      {/* Account Actions */}
      <View style={styles.section}>
        <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
          <Icon name="logout" size={20} color="#FF9500" />
          <Text style={styles.logoutText}>로그아웃</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.deleteButton} onPress={handleDeleteAccount}>
          <Icon name="delete-forever" size={20} color="#FF3B30" />
          <Text style={styles.deleteText}>계정 삭제</Text>
        </TouchableOpacity>
      </View>

      <GoalModal />
    </ScrollView>
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
    paddingBottom: 20,
    backgroundColor: '#FFFFFF',
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#1C1C1E',
  },
  editButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#007AFF20',
    justifyContent: 'center',
    alignItems: 'center',
  },
  profileSection: {
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    paddingVertical: 30,
    paddingHorizontal: 20,
  },
  profileImage: {
    width: 100,
    height: 100,
    borderRadius: 50,
    marginBottom: 16,
  },
  nickname: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginBottom: 4,
  },
  username: {
    fontSize: 16,
    color: '#8E8E93',
    marginBottom: 4,
  },
  email: {
    fontSize: 14,
    color: '#8E8E93',
    marginBottom: 12,
  },
  bio: {
    fontSize: 16,
    color: '#1C1C1E',
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: 12,
  },
  joinDate: {
    fontSize: 14,
    color: '#8E8E93',
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
  goalItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F2F2F7',
    borderRadius: 12,
    padding: 16,
  },
  goalLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  goalText: {
    marginLeft: 12,
  },
  goalTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 2,
  },
  goalSubtitle: {
    fontSize: 14,
    color: '#8E8E93',
  },
  settingItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  settingLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  settingText: {
    marginLeft: 12,
  },
  settingTitle: {
    fontSize: 16,
    color: '#1C1C1E',
    marginBottom: 2,
  },
  settingSubtitle: {
    fontSize: 14,
    color: '#8E8E93',
  },
  logoutButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#FF950020',
    borderWidth: 1,
    borderColor: '#FF9500',
    borderRadius: 8,
    padding: 16,
    marginBottom: 12,
  },
  logoutText: {
    color: '#FF9500',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
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
    marginBottom: 40,
  },
  deleteText: {
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
    padding: 24,
    width: '80%',
    maxWidth: 300,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
    textAlign: 'center',
    marginBottom: 20,
  },
  goalInputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 16,
  },
  goalInput: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#007AFF',
    borderBottomWidth: 2,
    borderBottomColor: '#007AFF',
    paddingVertical: 8,
    paddingHorizontal: 12,
    minWidth: 60,
  },
  goalUnit: {
    fontSize: 18,
    fontWeight: '600',
    color: '#1C1C1E',
    marginLeft: 8,
  },
  goalDescription: {
    fontSize: 14,
    color: '#8E8E93',
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: 24,
  },
  modalButtons: {
    flexDirection: 'row',
    gap: 12,
  },
  modalButton: {
    flex: 1,
    borderRadius: 8,
    padding: 12,
    alignItems: 'center',
  },
  modalCancelButton: {
    backgroundColor: '#F2F2F7',
  },
  modalConfirmButton: {
    backgroundColor: '#007AFF',
  },
  modalCancelText: {
    color: '#8E8E93',
    fontSize: 16,
    fontWeight: '600',
  },
  modalConfirmText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default ProfileScreen;