import { useContext } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AuthContext } from '@/auth/AuthContext';
import LoginPage from './screens/LoginPage';
import SignUpPage from './screens/SignUpPage';
import MainPage from './screens/MainPage';
import CleaningDetailsScreen from '@/screens/CleaningDetailsScreen';
import ManageTeamScreen from '@/screens/manager/ManageTeamScreen';
import NewCleaningScreen from '@/screens/manager/NewCleaningScreen';
import PropertyDetailsScreen from '@/screens/manager/PropertyDetailsScreen';
import NewPropertyScreen from '@/screens/manager/NewPropertyScreen';
import ProfileEditScreen from '@/screens/ProfileEditScreen';
import ChatScreen from '@/screens/ChatScreen';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import IssuesScreen from '@/screens/manager/IssuesScreen';

const Stack = createNativeStackNavigator();

export default function Navigation() {
  const { token, isLoading } = useContext(AuthContext);

  if (isLoading) return <LoadingSpinner label='Restoring your session...' />;

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {token ? (
          <Stack.Group navigationKey='authenticated'>
            <Stack.Screen name='MainPage' component={MainPage} />
            <Stack.Screen
              name='CleaningDetailsScreen'
              component={CleaningDetailsScreen}
              options={{ presentation: 'modal' }}
            />
            <Stack.Screen
              name='ManageTeamScreen'
              component={ManageTeamScreen}
              options={{ presentation: 'modal' }}
            />
            <Stack.Screen
              name='NewCleaningScreen'
              component={NewCleaningScreen}
              options={{ presentation: 'modal' }}
            />
            <Stack.Screen
              name='PropertyDetailsScreen'
              component={PropertyDetailsScreen}
              options={{ presentation: 'modal' }}
            />
            <Stack.Screen
              name='NewPropertyScreen'
              component={NewPropertyScreen}
              options={{ presentation: 'modal' }}
            />
            <Stack.Screen
              name='ProfileEditScreen'
              component={ProfileEditScreen}
              options={{ presentation: 'modal' }}
            />
            <Stack.Screen
              name='ChatScreen'
              component={ChatScreen}
              options={{ presentation: 'modal' }}
            />
            <Stack.Screen
              name='IssuesScreen'
              component={IssuesScreen}
              options={{ presentation: 'modal' }}
            />
          </Stack.Group>
        ) : (
          <Stack.Group navigationKey='guest'>
            <Stack.Screen name='LoginPage' component={LoginPage} />
            <Stack.Screen
              name='SignUpPage'
              component={SignUpPage}
              options={{ presentation: 'modal' }}
            />
          </Stack.Group>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
