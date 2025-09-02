import React from 'react';
import {
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  useColorScheme,
} from 'react-native';

import {
  Colors,
} from 'react-native/Libraries/NewAppScreen';

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  return (
    <SafeAreaView style={[backgroundStyle, styles.container]}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <View style={styles.content}>
        <Text style={[styles.title, { color: isDarkMode ? Colors.white : Colors.black }]}>
          📚 BookReview LLM Platform
        </Text>
        <Text style={[styles.subtitle, { color: isDarkMode ? Colors.lighter : Colors.dark }]}>
          AI 피드백과 함께하는 스마트 독서 플랫폼
        </Text>
        <View style={styles.featureList}>
          <Text style={[styles.feature, { color: isDarkMode ? Colors.white : Colors.black }]}>
            ✓ 책 등록 및 챕터 관리
          </Text>
          <Text style={[styles.feature, { color: isDarkMode ? Colors.white : Colors.black }]}>
            ✓ 독서 노트 작성
          </Text>
          <Text style={[styles.feature, { color: isDarkMode ? Colors.white : Colors.black }]}>
            ✓ AI 기반 개인화된 피드백
          </Text>
          <Text style={[styles.feature, { color: isDarkMode ? Colors.white : Colors.black }]}>
            ✓ 독서 목표 설정 및 통계
          </Text>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 16,
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 32,
    lineHeight: 24,
  },
  featureList: {
    alignItems: 'flex-start',
  },
  feature: {
    fontSize: 16,
    marginBottom: 12,
    lineHeight: 24,
  },
});

export default App;