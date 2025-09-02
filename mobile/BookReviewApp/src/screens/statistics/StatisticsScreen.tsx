import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  Dimensions,
  RefreshControl,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialIcons';
import { useNavigation } from '@react-navigation/native';
import { StatisticsScreenNavigationProp } from '../../types/navigation';

const { width } = Dimensions.get('window');

interface MonthlyStats {
  month: string;
  booksRead: number;
  notesWritten: number;
  pagesRead: number;
}

interface CategoryStats {
  category: string;
  count: number;
  percentage: number;
  color: string;
}

interface ReadingGoal {
  yearly: number;
  current: number;
  percentage: number;
}

interface OverallStats {
  totalBooks: number;
  totalPages: number;
  totalNotes: number;
  totalReadingTime: number; // in minutes
  averageRating: number;
  completionRate: number;
}

const StatisticsScreen = () => {
  const navigation = useNavigation<StatisticsScreenNavigationProp>();
  const [refreshing, setRefreshing] = useState(false);
  const [selectedPeriod, setSelectedPeriod] = useState<'monthly' | 'yearly'>('monthly');
  
  // Mock data
  const [overallStats] = useState<OverallStats>({
    totalBooks: 12,
    totalPages: 3450,
    totalNotes: 45,
    totalReadingTime: 2840, // 47시간 20분
    averageRating: 4.2,
    completionRate: 75,
  });

  const [readingGoal] = useState<ReadingGoal>({
    yearly: 24,
    current: 12,
    percentage: 50,
  });

  const [monthlyStats] = useState<MonthlyStats[]>([
    { month: '1월', booksRead: 2, notesWritten: 8, pagesRead: 560 },
    { month: '2월', booksRead: 1, notesWritten: 5, pagesRead: 320 },
    { month: '3월', booksRead: 3, notesWritten: 12, pagesRead: 780 },
    { month: '4월', booksRead: 2, notesWritten: 7, pagesRead: 650 },
    { month: '5월', booksRead: 1, notesWritten: 4, pagesRead: 290 },
    { month: '6월', booksRead: 3, notesWritten: 9, pagesRead: 850 },
  ]);

  const [categoryStats] = useState<CategoryStats[]>([
    { category: '기술/IT', count: 5, percentage: 42, color: '#007AFF' },
    { category: '자기계발', count: 3, percentage: 25, color: '#34C759' },
    { category: '소설', count: 2, percentage: 17, color: '#FF9500' },
    { category: '비즈니스', count: 1, percentage: 8, color: '#5856D6' },
    { category: '기타', count: 1, percentage: 8, color: '#FF3B30' },
  ]);

  const onRefresh = async () => {
    setRefreshing(true);
    // TODO: API에서 데이터 새로고침
    setTimeout(() => {
      setRefreshing(false);
    }, 1000);
  };

  const formatReadingTime = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}시간 ${mins}분`;
  };

  const StatCard = ({ 
    title, 
    value, 
    subtitle, 
    icon, 
    color 
  }: { 
    title: string; 
    value: string; 
    subtitle?: string; 
    icon: string; 
    color: string; 
  }) => (
    <View style={styles.statCard}>
      <Icon name={icon} size={24} color={color} />
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statTitle}>{title}</Text>
      {subtitle && <Text style={styles.statSubtitle}>{subtitle}</Text>}
    </View>
  );

  const ProgressBar = ({ 
    percentage, 
    color, 
    height = 8 
  }: { 
    percentage: number; 
    color: string; 
    height?: number; 
  }) => (
    <View style={[styles.progressBar, { height }]}>
      <View
        style={[
          styles.progressFill,
          { width: `${percentage}%`, backgroundColor: color, height }
        ]}
      />
    </View>
  );

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
      showsVerticalScrollIndicator={false}
    >
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>독서 통계</Text>
        <TouchableOpacity style={styles.shareButton}>
          <Icon name="share" size={24} color="#007AFF" />
        </TouchableOpacity>
      </View>

      {/* Reading Goal */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>2025년 독서 목표</Text>
        <View style={styles.goalCard}>
          <View style={styles.goalHeader}>
            <Text style={styles.goalProgress}>
              {readingGoal.current} / {readingGoal.yearly} 권
            </Text>
            <Text style={styles.goalPercentage}>{readingGoal.percentage}%</Text>
          </View>
          <ProgressBar percentage={readingGoal.percentage} color="#007AFF" height={12} />
          <Text style={styles.goalSubtitle}>
            목표까지 {readingGoal.yearly - readingGoal.current}권 남았습니다
          </Text>
        </View>
      </View>

      {/* Overall Statistics */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>전체 통계</Text>
        <View style={styles.statsGrid}>
          <StatCard
            title="총 도서 수"
            value={overallStats.totalBooks.toString()}
            icon="book"
            color="#007AFF"
          />
          <StatCard
            title="총 페이지"
            value={overallStats.totalPages.toLocaleString()}
            icon="article"
            color="#34C759"
          />
          <StatCard
            title="작성 노트"
            value={overallStats.totalNotes.toString()}
            icon="note"
            color="#FF9500"
          />
          <StatCard
            title="독서 시간"
            value={formatReadingTime(overallStats.totalReadingTime)}
            icon="schedule"
            color="#5856D6"
          />
          <StatCard
            title="평균 평점"
            value={overallStats.averageRating.toFixed(1)}
            subtitle="/ 5.0"
            icon="star"
            color="#FFD60A"
          />
          <StatCard
            title="완독률"
            value={`${overallStats.completionRate}%`}
            icon="check-circle"
            color="#FF3B30"
          />
        </View>
      </View>

      {/* Period Selection */}
      <View style={styles.section}>
        <View style={styles.periodHeader}>
          <Text style={styles.sectionTitle}>기간별 통계</Text>
          <View style={styles.periodSelector}>
            <TouchableOpacity
              style={[
                styles.periodButton,
                selectedPeriod === 'monthly' && styles.periodButtonActive
              ]}
              onPress={() => setSelectedPeriod('monthly')}
            >
              <Text style={[
                styles.periodButtonText,
                selectedPeriod === 'monthly' && styles.periodButtonTextActive
              ]}>
                월별
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.periodButton,
                selectedPeriod === 'yearly' && styles.periodButtonActive
              ]}
              onPress={() => setSelectedPeriod('yearly')}
            >
              <Text style={[
                styles.periodButtonText,
                selectedPeriod === 'yearly' && styles.periodButtonTextActive
              ]}>
                연도별
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Monthly Chart */}
        <View style={styles.chartContainer}>
          <Text style={styles.chartTitle}>월별 독서량</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <View style={styles.chartContent}>
              {monthlyStats.map((stat, index) => {
                const maxBooks = Math.max(...monthlyStats.map(s => s.booksRead));
                const barHeight = (stat.booksRead / maxBooks) * 100;
                
                return (
                  <View key={index} style={styles.chartBar}>
                    <View style={styles.barContainer}>
                      <View
                        style={[
                          styles.bar,
                          { 
                            height: `${barHeight}%`,
                            backgroundColor: '#007AFF'
                          }
                        ]}
                      />
                    </View>
                    <Text style={styles.barValue}>{stat.booksRead}</Text>
                    <Text style={styles.barLabel}>{stat.month}</Text>
                  </View>
                );
              })}
            </View>
          </ScrollView>
        </View>
      </View>

      {/* Category Statistics */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>카테고리별 통계</Text>
        <View style={styles.categoryContainer}>
          {categoryStats.map((category, index) => (
            <View key={index} style={styles.categoryItem}>
              <View style={styles.categoryInfo}>
                <View style={[styles.categoryDot, { backgroundColor: category.color }]} />
                <Text style={styles.categoryName}>{category.category}</Text>
                <Text style={styles.categoryCount}>({category.count}권)</Text>
              </View>
              <View style={styles.categoryProgress}>
                <ProgressBar percentage={category.percentage} color={category.color} />
                <Text style={styles.categoryPercentage}>{category.percentage}%</Text>
              </View>
            </View>
          ))}
        </View>
      </View>

      {/* Reading Achievements */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>독서 성취</Text>
        <View style={styles.achievementsContainer}>
          <View style={styles.achievementItem}>
            <View style={styles.achievementIcon}>
              <Icon name="emoji-events" size={24} color="#FFD60A" />
            </View>
            <View style={styles.achievementContent}>
              <Text style={styles.achievementTitle}>첫 번째 완독</Text>
              <Text style={styles.achievementDescription}>첫 번째 책을 완독했습니다</Text>
            </View>
            <Text style={styles.achievementDate}>2024.01.15</Text>
          </View>
          
          <View style={styles.achievementItem}>
            <View style={styles.achievementIcon}>
              <Icon name="grade" size={24} color="#FF9500" />
            </View>
            <View style={styles.achievementContent}>
              <Text style={styles.achievementTitle}>연속 독서 7일</Text>
              <Text style={styles.achievementDescription}>7일 연속으로 독서했습니다</Text>
            </View>
            <Text style={styles.achievementDate}>2024.03.22</Text>
          </View>
          
          <View style={styles.achievementItem}>
            <View style={styles.achievementIcon}>
              <Icon name="auto-awesome" size={24} color="#5856D6" />
            </View>
            <View style={styles.achievementContent}>
              <Text style={styles.achievementTitle}>노트 마스터</Text>
              <Text style={styles.achievementDescription}>50개의 노트를 작성했습니다</Text>
            </View>
            <Text style={styles.achievementDate}>진행중</Text>
          </View>
        </View>
      </View>

      {/* Detailed Stats */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>상세 통계</Text>
        <View style={styles.detailStats}>
          <View style={styles.detailStatItem}>
            <Text style={styles.detailStatLabel}>하루 평균 독서 시간</Text>
            <Text style={styles.detailStatValue}>
              {Math.round(overallStats.totalReadingTime / 180)}분
            </Text>
          </View>
          <View style={styles.detailStatItem}>
            <Text style={styles.detailStatLabel}>책 한 권당 평균 노트 수</Text>
            <Text style={styles.detailStatValue}>
              {Math.round(overallStats.totalNotes / overallStats.totalBooks)}개
            </Text>
          </View>
          <View style={styles.detailStatItem}>
            <Text style={styles.detailStatLabel}>평균 책 두께</Text>
            <Text style={styles.detailStatValue}>
              {Math.round(overallStats.totalPages / overallStats.totalBooks)} 페이지
            </Text>
          </View>
          <View style={styles.detailStatItem}>
            <Text style={styles.detailStatLabel}>가장 많이 읽은 카테고리</Text>
            <Text style={styles.detailStatValue}>{categoryStats[0].category}</Text>
          </View>
        </View>
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
  headerTitle: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#1C1C1E',
  },
  shareButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#007AFF20',
    justifyContent: 'center',
    alignItems: 'center',
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
  goalCard: {
    backgroundColor: '#F2F2F7',
    borderRadius: 12,
    padding: 20,
  },
  goalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  goalProgress: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1C1C1E',
  },
  goalPercentage: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#007AFF',
  },
  goalSubtitle: {
    fontSize: 14,
    color: '#8E8E93',
    marginTop: 8,
  },
  progressBar: {
    backgroundColor: '#E5E5EA',
    borderRadius: 4,
  },
  progressFill: {
    borderRadius: 4,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  statCard: {
    width: (width - 60) / 2,
    backgroundColor: '#F2F2F7',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginBottom: 12,
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#1C1C1E',
    marginTop: 8,
    marginBottom: 4,
  },
  statTitle: {
    fontSize: 14,
    color: '#8E8E93',
    textAlign: 'center',
  },
  statSubtitle: {
    fontSize: 12,
    color: '#8E8E93',
    marginTop: 2,
  },
  periodHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  periodSelector: {
    flexDirection: 'row',
    backgroundColor: '#F2F2F7',
    borderRadius: 8,
    padding: 2,
  },
  periodButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
  },
  periodButtonActive: {
    backgroundColor: '#FFFFFF',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  periodButtonText: {
    fontSize: 14,
    color: '#8E8E93',
    fontWeight: '500',
  },
  periodButtonTextActive: {
    color: '#1C1C1E',
    fontWeight: '600',
  },
  chartContainer: {
    backgroundColor: '#F2F2F7',
    borderRadius: 12,
    padding: 16,
  },
  chartTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 16,
  },
  chartContent: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    height: 120,
  },
  chartBar: {
    alignItems: 'center',
    marginHorizontal: 8,
    width: 40,
  },
  barContainer: {
    height: 80,
    justifyContent: 'flex-end',
    width: '100%',
  },
  bar: {
    width: '100%',
    borderRadius: 4,
    minHeight: 4,
  },
  barValue: {
    fontSize: 12,
    fontWeight: '600',
    color: '#1C1C1E',
    marginTop: 4,
  },
  barLabel: {
    fontSize: 10,
    color: '#8E8E93',
    marginTop: 2,
  },
  categoryContainer: {
    gap: 16,
  },
  categoryItem: {
    gap: 8,
  },
  categoryInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  categoryDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 8,
  },
  categoryName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    flex: 1,
  },
  categoryCount: {
    fontSize: 14,
    color: '#8E8E93',
  },
  categoryProgress: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  categoryPercentage: {
    fontSize: 14,
    fontWeight: '600',
    color: '#1C1C1E',
    minWidth: 40,
    textAlign: 'right',
  },
  achievementsContainer: {
    gap: 16,
  },
  achievementItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F2F2F7',
    borderRadius: 12,
    padding: 16,
  },
  achievementIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  achievementContent: {
    flex: 1,
  },
  achievementTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: '#1C1C1E',
    marginBottom: 2,
  },
  achievementDescription: {
    fontSize: 14,
    color: '#8E8E93',
  },
  achievementDate: {
    fontSize: 12,
    color: '#8E8E93',
  },
  detailStats: {
    gap: 16,
  },
  detailStatItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  detailStatLabel: {
    fontSize: 16,
    color: '#1C1C1E',
  },
  detailStatValue: {
    fontSize: 16,
    fontWeight: '600',
    color: '#007AFF',
  },
});

export default StatisticsScreen;