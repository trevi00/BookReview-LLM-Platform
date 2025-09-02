import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createStackNavigator } from '@react-navigation/stack';
import { NavigationContainer } from '@react-navigation/navigation';
import Icon from 'react-native-vector-icons/MaterialIcons';

// Screens
import LoginScreen from '../screens/auth/LoginScreen';
import RegisterScreen from '../screens/auth/RegisterScreen';
import HomeScreen from '../screens/home/HomeScreen';
import BooksScreen from '../screens/books/BooksScreen';
import AddBookScreen from '../screens/books/AddBookScreen';
import BookDetailScreen from '../screens/books/BookDetailScreen';
import NotesScreen from '../screens/notes/NotesScreen';
import AddNoteScreen from '../screens/notes/AddNoteScreen';
import NoteDetailScreen from '../screens/notes/NoteDetailScreen';
import StatisticsScreen from '../screens/statistics/StatisticsScreen';
import ProfileScreen from '../screens/profile/ProfileScreen';

// Types
import { RootStackParamList, MainTabParamList } from '../types/navigation';

const Stack = createStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();

const MainTabs = () => {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName: string;

          switch (route.name) {
            case 'Home':
              iconName = 'home';
              break;
            case 'Books':
              iconName = 'book';
              break;
            case 'Notes':
              iconName = 'note';
              break;
            case 'Statistics':
              iconName = 'analytics';
              break;
            case 'Profile':
              iconName = 'person';
              break;
            default:
              iconName = 'help';
          }

          return <Icon name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: '#007AFF',
        tabBarInactiveTintColor: '#8E8E93',
        headerShown: false,
      })}
    >
      <Tab.Screen 
        name="Home" 
        component={HomeScreen} 
        options={{ title: '홈' }}
      />
      <Tab.Screen 
        name="Books" 
        component={BooksScreen} 
        options={{ title: '내 서재' }}
      />
      <Tab.Screen 
        name="Notes" 
        component={NotesScreen} 
        options={{ title: '독서 노트' }}
      />
      <Tab.Screen 
        name="Statistics" 
        component={StatisticsScreen} 
        options={{ title: '통계' }}
      />
      <Tab.Screen 
        name="Profile" 
        component={ProfileScreen} 
        options={{ title: '프로필' }}
      />
    </Tab.Navigator>
  );
};

const AppNavigator = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator 
        initialRouteName="Login"
        screenOptions={{ headerShown: false }}
      >
        {/* Auth Screens */}
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Register" component={RegisterScreen} />
        
        {/* Main App */}
        <Stack.Screen name="MainTabs" component={MainTabs} />
        
        {/* Detail Screens */}
        <Stack.Screen 
          name="AddBook" 
          component={AddBookScreen} 
          options={{ 
            headerShown: true, 
            title: '책 추가',
            headerBackTitle: '뒤로'
          }}
        />
        <Stack.Screen 
          name="BookDetail" 
          component={BookDetailScreen} 
          options={{ 
            headerShown: true, 
            title: '책 상세',
            headerBackTitle: '뒤로'
          }}
        />
        <Stack.Screen 
          name="AddNote" 
          component={AddNoteScreen} 
          options={{ 
            headerShown: true, 
            title: '노트 작성',
            headerBackTitle: '뒤로'
          }}
        />
        <Stack.Screen 
          name="NoteDetail" 
          component={NoteDetailScreen} 
          options={{ 
            headerShown: true, 
            title: '노트 상세',
            headerBackTitle: '뒤로'
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default AppNavigator;