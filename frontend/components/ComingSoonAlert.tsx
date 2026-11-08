import { Alert } from 'react-native';

export const showComingSoonAlert = () => {
  Alert.alert('Coming Soon!', 'This feature is still under development', [
    { text: 'Ok' },
  ]);
};
