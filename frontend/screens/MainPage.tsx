// screens/MainPage.tsx
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import HomeScreen from './HomeScreen';
import PropertyListScreen from './manager/PropertyListScreen';
import CleaningAvailabilityScreen from './cleaner/CleaningAvailabilityScreen';
import { useContext } from 'react';
import { AuthContext } from '@/auth/AuthContext';
import ProfileScreen from './ProfileScreen';
import { Role } from '@/types/entityTypes';
import { Icon } from '@/components/ui/icon';
import { CalendarCheck, Home, ListTodo, UserRound } from 'lucide-react-native';

const Tab = createBottomTabNavigator();

export default function MainPage() {
  const user = useContext(AuthContext);

  const role = user.user?.role;

  return (
    <Tab.Navigator>
      <Tab.Screen
        name='Dashboard'
        component={HomeScreen}
        options={{
          tabBarIcon: ({ color, size }) => (
            <Icon as={ListTodo} color={color} width={size} height={size} />
          ),
        }}
      />
      {role === Role.MANAGER && (
        <>
          <Tab.Screen
            name='Properties'
            component={PropertyListScreen}
            options={{
              tabBarIcon: ({ color, size }) => (
                <Icon as={Home} color={color} width={size} height={size} />
              ),
            }}
          />
        </>
      )}
      {/*<Tab.Screen name='Profile / Availability' component={AccountScreen} />*/}
      {role === Role.CLEANER && (
        <>
          <Tab.Screen
            name='Availability'
            component={CleaningAvailabilityScreen}
            options={{
              tabBarIcon: ({ color, size }) => (
                <Icon
                  as={CalendarCheck}
                  color={color}
                  width={size}
                  height={size}
                />
              ),
            }}
          />
        </>
      )}
      <Tab.Screen
        name='Profile'
        component={ProfileScreen}
        options={{
          tabBarIcon: ({ color, size }) => (
            <Icon as={UserRound} color={color} width={size} height={size} />
          ),
        }}
      />
    </Tab.Navigator>
  );
}
