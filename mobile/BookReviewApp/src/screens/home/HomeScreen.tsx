import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  RefreshControl,
  Dimensions,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { useNavigation } from '@react-navigation/native';
import { HomeScreenNavigationProp } from '../../types/navigation';

const { width } = Dimensions.get('window');

interface DashboardStats {
  totalBooks: number;
  currentBooks: number;
  totalNotes: number;
  yearlyGoal: number;
  yearlyProgress: number;
}

interface RecentActivity {
  id: number;
  type: 'book' | 'note' | 'goal';
  title: string;
  subtitle: string;
  time: string;
}

const HomeScreen = () => {
  const navigation = useNavigation<HomeScreenNavigationProp>();
  const [refreshing, setRefreshing] = useState(false);
  const [stats, setStats] = useState<DashboardStats>({
    totalBooks: 12,
    currentBooks: 3,
    totalNotes: 45,
    yearlyGoal: 24,
    yearlyProgress: 50,
  });

  const [recentActivities, setRecentActivities] = useState<RecentActivity[]>([
    {
      id: 1,
      type: 'book',
      title: '클린 아키텍처',
      subtitle: '새로운 책을 추가했습니다',
      time: '2시간 전',
    },
    {
      id: 2,
      type: 'note',
      title: '객체지향의 이해',
      subtitle: '새로운 노트를 작성했습니다',
      time: '5시간 전',
    },
    {
      id: 3,
      type: 'goal',
      title: '2025년 독서 목표',
      subtitle: '목표의 50%를 달성했습니다',
      time: '1일 전',
    },
  ]);

  const onRefresh = async () => {
    setRefreshing(true);
    // TODO: API에서 데이터 새로고침
    setTimeout(() => {
      setRefreshing(false);
    }, 1000);
  };

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'book': return 'book';
      case 'note': return 'note';
      case 'goal': return 'flag';
      default: return 'info';
    }
  };

  const getActivityColor = (type: string) => {
    switch (type) {
      case 'book': return '#007AFF';
      case 'note': return '#34C759';
      case 'goal': return '#FF9500';
      default: return '#8E8E93';
    }
  };

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text style={styles.greeting}>안녕하세요! 👋</Text>
          <Text style={styles.userName}>독서왕님</Text>
        </View>
        <TouchableOpacity style={styles.profileButton}>
          <Icon name="account-circle" size={40} color="#007AFF" />
        </TouchableOpacity>
      </View>

      {/* Quick Stats */}
      <View style={styles.statsContainer}>
        <View style={styles.statsRow}>
          <View style={styles.statCard}>
            <Icon name="book" size={24} color="#007AFF" />
            <Text style={styles.statNumber}>{stats.totalBooks}</Text>
            <Text style={styles.statLabel}>총 도서</Text>
          </View>
          <View style={styles.statCard}>
            <Icon name="book-open" size={24} color="#34C759" />
            <Text style={styles.statNumber}>{stats.currentBooks}</Text>
            <Text style={styles.statLabel}>읽는 중</Text>
          </View>
        </View>
        <View style={styles.statsRow}>
          <View style={styles.statCard}>
            <Icon name="note" size={24} color="#FF9500" />
            <Text style={styles.statNumber}>{stats.totalNotes}</Text>
            <Text style={styles.statLabel}>작성 노트</Text>
          </View>
          <View style={styles.statCard}>
            <Icon name="flag" size={24} color="#FF3B30" />
            <Text style={styles.statNumber}>{stats.yearlyProgress}%</Text>
            <Text style={styles.statLabel}>연간 목표</Text>
          </View>
        </View>
      </View>

      {/* Progress Card */}
      <View style={styles.progressCard}>
        <View style={styles.progressHeader}>
          <Text style={styles.progressTitle}>2025년 독서 목표</Text>
          <Text style={styles.progressSubtitle}>
            {Math.floor(stats.yearlyGoal * stats.yearlyProgress / 100)} / {stats.yearlyGoal} 권
          </Text>
        </View>
        <View style={styles.progressBar}>
          <View 
            style={[
              styles.progressFill, 
              { width: `${stats.yearlyProgress}%` }
            ]} 
          />
        </View>
        <Text style={styles.progressText}>
          목표까지 {stats.yearlyGoal - Math.floor(stats.yearlyGoal * stats.yearlyProgress / 100)}권 남았습니다
        </Text>
      </View>

      {/* Quick Actions */}
      <View style={styles.quickActions}>
        <Text style={styles.sectionTitle}>빠른 작업</Text>
        <View style={styles.actionRow}>
          <TouchableOpacity 
            style={styles.actionButton}
            onPress={() => navigation.navigate('AddBook')}
          >
            <Icon name="add" size={24} color="#FFFFFF" />
            <Text style={styles.actionButtonText}>책 추가</Text>
          </TouchableOpacity>
          <TouchableOpacity 
            style={[styles.actionButton, { backgroundColor: '#34C759' }]}
            onPress={() => navigation.navigate('AddNote')}
          >
            <Icon name="edit" size={24} color="#FFFFFF" />
            <Text style={styles.actionButtonText}>노트 작성</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Recent Activities */}
      <View style={styles.recentSection}>
        <Text style={styles.sectionTitle}>최근 활동</Text>
        {recentActivities.map((activity) => (
          <View key={activity.id} style={styles.activityItem}>
            <View style={[
              styles.activityIcon,
              { backgroundColor: getActivityColor(activity.type) + '20' }
            ]}>
              <Icon 
                name={getActivityIcon(activity.type)} 
                size={20} 
                color={getActivityColor(activity.type)} 
              />
            </View>
            <View style={styles.activityContent}>
              <Text style={styles.activityTitle}>{activity.title}</Text>
              <Text style={styles.activitySubtitle}>{activity.subtitle}</Text>
            </View>
            <Text style={styles.activityTime}>{activity.time}</Text>
          </View>
        ))}
      </View>

      {/* AI Recommendation */}
      <View style={styles.aiCard}>
        <View style={styles.aiHeader}>
          <Icon name="auto-awesome" size={24} color="#FF9500" />
          <Text style={styles.aiTitle}>AI 추천</Text>
        </View>
        <Text style={styles.aiMessage}>
          최근 기술 도서를 많이 읽으시는군요! 📚
          "마이크로서비스 패턴"을 추천해드립니다.
        </Text>
        <TouchableOpacity style={styles.aiButton}>
          <Text style={styles.aiButtonText}>자세히 보기</Text>
        </TouchableOpacity>
      </View>
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
  greeting: {
    fontSize: 16,
    color: '#8E8E93',
  },
  userName: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginTop: 4,
  },
  profileButton: {
    padding: 4,
  },
  statsContainer: {
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  statsRow: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  statCard: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginHorizontal: 6,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 3,
  },
  statNumber: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginTop: 8,
  },
  statLabel: {
    fontSize: 12,
    color: '#8E8E93',
    marginTop: 4,
  },
  progressCard: {
    marginHorizontal: 20,
    marginBottom: 20,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 20,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3,
    elevation: 3,
  },
  progressHeader: {
    marginBottom: 16,
  },
  progressTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
  },
  progressSubtitle: {
    fontSize: 14,
    color: '#8E8E93',
    marginTop: 4,
  },
  progressBar: {
    height: 8,
    backgroundColor: '#E5E5EA',
    borderRadius: 4,
    marginBottom: 12,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#007AFF',
    borderRadius: 4,
  },
  progressText: {
    fontSize: 14,
    color: '#8E8E93',
  },
  quickActions: {
    paddingHorizontal: 20,
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginBottom: 16,
  },
  actionRow: {
    flexDirection: 'row',
  },
  actionButton: {
    flex: 1,
    backgroundColor: '#007AFF',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginHorizontal: 6,
    flexDirection: 'row',
    justifyContent: 'center',
  },
  actionButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
    marginLeft: 8,
  },
  recentSection: {
    paddingHorizontal: 20,
    marginBottom: 20,
  },
  activityItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  activityIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  activityContent: {
    flex: 1,
  },
  activityTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
  },
  activitySubtitle: {
    fontSize: 14,
    color: '#8E8E93',
    marginTop: 2,
  },
  activityTime: {
    fontSize: 12,
    color: '#8E8E93',
  },
  aiCard: {
    marginHorizontal: 20,
    marginBottom: 40,
    backgroundColor: '#FFF9E6',
    borderRadius: 12,
    padding: 20,
    borderWidth: 1,
    borderColor: '#FFE066',
  },
  aiHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  aiTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginLeft: 8,
  },
  aiMessage: {
    fontSize: 14,
    color: '#1C1C1E',
    lineHeight: 20,
    marginBottom: 16,
  },
  aiButton: {
    backgroundColor: '#FF9500',
    borderRadius: 8,
    paddingVertical: 12,
    paddingHorizontal: 16,
    alignItems: 'center',
  },
  aiButtonText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '600',
  },
});

export default HomeScreen;