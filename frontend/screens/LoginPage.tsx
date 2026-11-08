import { StatusBar } from 'expo-status-bar';
import { Text, View, TextInput, Pressable, Alert } from 'react-native';
import '../global.css';
import { globalStyles } from '@/styles/globalStyles';
import { styles } from '@/styles/styles';
import { Image } from 'react-native';
import { useState } from 'react';
import { useContext } from 'react';
import { AuthContext } from '@/auth/AuthContext';
import { authenticate } from '@/api/auth';
import { showComingSoonAlert } from '@/components/ComingSoonAlert';
import { getUsernameValidationError } from '@/utils/username';

export default function LoginPage({ navigation }: any) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { login } = useContext(AuthContext);

  const handleLogin = async () => {
    const usernameError = getUsernameValidationError(username);
    if (usernameError) {
      Alert.alert('Check your username', usernameError);
      return;
    }

    try {
      const { token, user } = await authenticate(username, password);
      await login(token, user);
      Alert.alert('Login successful', 'You have been logged in successfully.');
    } catch (error) {
      console.error('Login error', error);
      Alert.alert('Login failed', 'Invalid username or password.');
    }
  };

  return (
    <View style={globalStyles.screen}>
      <Text style={globalStyles.title}>RESTful Housekeeping</Text>
      <Text style={globalStyles.subtitle}>
        Cleaning Accountability Made Simple
      </Text>

      <Image
        source={require('../assets/logonewest.png')}
        style={globalStyles.logoImage}
      />

      <View style={styles.formCard}>
        <Text>Username</Text>
        <TextInput
          style={styles.input}
          placeholder='Enter your username'
          value={username}
          onChangeText={setUsername}
          autoCapitalize='none'
          autoCorrect={false}
          textContentType='username'
        />

        <Text>Password</Text>
        <TextInput
          style={styles.input}
          placeholder='Enter your password'
          secureTextEntry
          value={password}
          onChangeText={setPassword}
        />

        <Pressable
          style={styles.button}
          className='mt-3 p-3'
          onPress={handleLogin}
        >
          <Text style={styles.buttonText}>Log In</Text>
        </Pressable>

        <Pressable onPress={() => navigation.navigate('SignUpPage')}>
          <Text style={styles.link}>Create Account</Text>
        </Pressable>
        <Pressable onPress={showComingSoonAlert}>
          <Text style={styles.link}>Forgot Password</Text>
        </Pressable>
      </View>

      <StatusBar style='auto' />
    </View>
  );
}
