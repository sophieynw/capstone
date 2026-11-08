import { useContext, useState } from 'react';
import { Alert, Pressable, ScrollView, Text, View } from 'react-native';
import { LogOut, UserPen } from 'lucide-react-native';

import { AuthContext } from '@/auth/AuthContext';
import {
  Avatar,
  AvatarFallbackText,
  AvatarImage,
} from '@/components/ui/avatar';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Heading } from '@/components/ui/heading';
import {
  useUpcomingCleanings,
  useCompletedCleanings,
} from '@/hooks/useCleanings';
import {
  useProfilePicture,
  useUpdateUser,
  useUploadProfilePicture,
} from '@/hooks/useUsers';
import { styles } from '@/styles/styles';
import { Role } from '@/types/entityTypes';
import { sortAvailabilitySlots, toTitleCase } from '@/utils/helpers';
import { usePropertyAll } from '@/hooks/useProperties';
import { useCleaners } from '@/hooks/useUsers';
import { useAvailabilities } from '@/hooks/useAvailabilities';
import { pickLibraryPhotos } from '@/utils/photoPicker';
// region Manager Summary Card

export default function ProfileScreen({ navigation }: any) {
  const { user, logout } = useContext(AuthContext);
  const [isOpeningPicturePicker, setIsOpeningPicturePicker] = useState(false);
  const { data: cleanings } = useUpcomingCleanings();
  const { data: completedCleanings } = useCompletedCleanings();
  const { data: properties } = usePropertyAll();
  const { data: cleaners } = useCleaners();
  const { data: availabilities } = useAvailabilities(
    user?.role === Role.CLEANER ? user.id : undefined,
  );
  const sortedAvailabilities = sortAvailabilitySlots(availabilities);

  // load profile picture from the backend
  const { data: profilePicture } = useProfilePicture(user?.id);
  const uploadMutation = useUploadProfilePicture();

  // show initials instead if the image doesn't load
  const [failedImage, setFailedImage] = useState<string | null>(null);
  const showImage = !!profilePicture && profilePicture !== failedImage;

  const handleLogout = async () => {
    try {
      await logout();
      Alert.alert(
        'Logout successful',
        'You have been logged out successfully.',
      );
    } catch (error) {
      console.error('Logout error', error);
      Alert.alert('Logout failed', 'Something went wrong while logging out.');
    }
  };

  // pick an image and upload it
  const handlePickProfilePicture = async () => {
    if (!user || isOpeningPicturePicker || uploadMutation.isPending) return;

    setIsOpeningPicturePicker(true);

    try {
      const [image] = await pickLibraryPhotos();
      if (image) uploadMutation.mutate({ userId: user.id, image });
    } catch {
      Alert.alert('Could not open photos', 'Please try again.');
    } finally {
      setIsOpeningPicturePicker(false);
    }
  };

  function formatTime(time: string) {
    const [hourString, minute] = time.split(':');
    const hour = Number(hourString);

    const period = hour >= 12 ? 'PM' : 'AM';
    const displayHour = hour % 12 || 12;

    return `${displayHour}:${minute} ${period}`;
  }

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.screenContent}
    >
      <Card style={styles.mediumCard}>
        {/* profile picture, tap to change */}
        <Pressable
          onPress={handlePickProfilePicture}
          disabled={isOpeningPicturePicker || uploadMutation.isPending}
          accessibilityRole='button'
          accessibilityLabel='Change profile picture'
          style={{ alignSelf: 'center', alignItems: 'center', gap: 4 }}
        >
          <Avatar style={{ width: 80, height: 80 }}>
            <AvatarFallbackText className='text-2xl'>
              {`${user?.firstName ?? ''} ${user?.lastName ?? ''}`}
            </AvatarFallbackText>
            {showImage && (
              <AvatarImage
                source={{ uri: profilePicture }}
                onError={() => setFailedImage(profilePicture)}
              />
            )}
          </Avatar>
          <Text style={{ fontSize: 12, color: '#888' }}>
            {isOpeningPicturePicker
              ? 'Opening photos...'
              : uploadMutation.isPending
                ? 'Uploading...'
                : 'Tap to change'}
          </Text>
        </Pressable>

        <Heading size='lg'>
          {user?.firstName} {user?.lastName}
        </Heading>

        <View className='gap-2'>
          <Text>Account Type: {toTitleCase(user?.role ?? '')}</Text>
          <Text>Username: {user?.username}</Text>
          <Text>Email: {user?.email}</Text>
          <Text>Phone: {user?.phoneNumber}</Text>
          <Text>
            Organization: {user?.organization?.name ?? 'No organization'}
          </Text>
        </View>
      </Card>

      {user?.role === Role.MANAGER && (
        <Card style={styles.mediumCard}>
          <Heading size='lg'>Account Summary</Heading>
          <View className='gap-2'>
            <Text>Upcoming Cleanings: {cleanings?.length ?? 0}</Text>
            <Text>Properties: {properties?.length ?? 0}</Text>
            <Text>Team Members: {cleaners?.length ?? 0}</Text>
          </View>
        </Card>
      )}

      {user?.role === Role.CLEANER && (
        <Card style={styles.mediumCard}>
          <Heading size='lg'>Cleaner Summary</Heading>
          <View className='gap-2'>
            <Text>Assigned Cleanings: {cleanings?.length ?? 0}</Text>
            <Text>Completed Cleanings: {completedCleanings?.length ?? 0}</Text>
            <Text>Availability:</Text>

            {sortedAvailabilities.map((availability) => (
              <Text key={availability.id}>
                {toTitleCase(availability.dayOfWeek)}:{' '}
                {formatTime(availability.startTime)} -{' '}
                {formatTime(availability.endTime)}
              </Text>
            ))}
          </View>
        </Card>
      )}

      <View style={styles.hStack}>
        <Button
          size='lg'
          variant='outline'
          className='rounded-full'
          style={styles.button}
          onPress={() => navigation.navigate('ProfileEditScreen')}
        >
          <ButtonIcon style={styles.buttonText} as={UserPen} />
          <ButtonText style={styles.buttonText}>Edit Profile</ButtonText>
        </Button>

        <Button
          size='lg'
          className='rounded-full'
          style={styles.buttonWarn}
          onPress={handleLogout}
        >
          <ButtonIcon style={styles.buttonTextWarn} as={LogOut} />
          <ButtonText style={styles.buttonTextWarn}>Log Out</ButtonText>
        </Button>
      </View>
    </ScrollView>
  );
}
