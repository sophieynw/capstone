import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getAllCleaners,
  getProfilePicturePath,
  getUserById,
  removeCleanerFromTeam,
  updateUserById,
  uploadProfilePicture,
} from '@/api/usersApi';
import { AuthContext } from '@/auth/AuthContext';
import { useContext } from 'react';
import { useNavigation } from '@react-navigation/native';
import { UpdateUserPayload } from '@/types/entityTypes';
import { Alert } from 'react-native';
import { register } from '@/api/auth';
import { getApiErrorMessage } from '@/api/apiClient';

export function useCleanerById(userId: number | null) {
  return useQuery({
    queryKey: ['cleaner', userId],
    queryFn: () => getUserById(userId),
    enabled: !!userId,
  });
}

export function useCleaners(enabled = true) {
  const { user } = useContext(AuthContext);
  const organizationId = user?.organization?.id;

  return useQuery({
    queryKey: ['cleaners', organizationId],
    queryFn: () => getAllCleaners(organizationId!),
    enabled: enabled && !!organizationId,
  });
}

export function useCreateUser() {
  const { login } = useContext(AuthContext);

  return useMutation({
    mutationFn: register,

    onSuccess: async ({ token, user }) => {
      await login(token, user);
      Alert.alert('Account created', 'Your account was created successfully.');
    },

    onError: (error: unknown) => {
      console.error('Create user error:', error);
      Alert.alert(
        'Unable to register',
        getApiErrorMessage(error, 'Your account could not be created.'),
      );
    },
  });
}

export function useRemoveCleanerFromTeam() {
  const queryClient = useQueryClient();
  const { user } = useContext(AuthContext);

  return useMutation({
    mutationFn: removeCleanerFromTeam,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['cleaners', user?.organization?.id],
          exact: true,
          refetchType: 'all',
        }),
        queryClient.invalidateQueries({
          queryKey: ['upcoming-cleanings'],
          refetchType: 'all',
        }),
      ]);
    },
  });
}

export function useUpdateUser() {
  const queryClient = useQueryClient();
  const navigation = useNavigation();
  const { user, update } = useContext(AuthContext);

  return useMutation({
    mutationFn: ({
      userId,
      user,
    }: {
      userId: number;
      user: UpdateUserPayload;
    }) => updateUserById(userId, user),

    onSuccess: async (updatedUser) => {
      const isCurrentUser = updatedUser.id === user?.id;

      if (isCurrentUser) {
        await update(updatedUser);
      }

      await queryClient.invalidateQueries({
        queryKey: ['cleaner', updatedUser?.id],
      });

      Alert.alert(
        'Success',
        isCurrentUser
          ? 'Your profile was updated.'
          : 'The cleaner was successfully added to your organization.',
        [
          {
            text: 'OK',
            onPress: () => navigation.goBack(),
          },
        ],
      );
    },

    onError: (error) => {
      console.error('Update user error:', error);
      Alert.alert('Unable to update', 'The user could not be updated.');
    },
  });
}

// gets the ImageKit URL saved for a user
export function useProfilePicture(userId?: number) {
  return useQuery({
    queryKey: ['profile-picture', userId],
    queryFn: () => getProfilePicturePath(userId!),
    enabled: userId !== undefined,
  });
}

// uploads a new profile picture and updates the saved user
export function useUploadProfilePicture() {
  const queryClient = useQueryClient();
  const { update } = useContext(AuthContext);

  return useMutation({
    mutationFn: ({
      userId,
      image,
    }: {
      userId: number;
      image: Parameters<typeof uploadProfilePicture>[1];
    }) => uploadProfilePicture(userId, image),

    onSuccess: async (updatedUser) => {
      // response already has the new path so we don't need to refetch
      queryClient.setQueryData(
        ['profile-picture', updatedUser.id],
        updatedUser.profilePicturePath ?? null,
      );

      await update(updatedUser);
    },

    onError: (error) => {
      console.error('Upload profile picture error:', error);
      Alert.alert(
        'Upload failed',
        'Your profile picture could not be uploaded.',
      );
    },
  });
}
