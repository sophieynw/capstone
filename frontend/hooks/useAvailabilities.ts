// hooks/useAvailabilities.ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getAvailabilitySlots,
  updateAvailabilitySlot,
  createAvailabilitySlot,
  deleteAvailabilitySlot,
} from '@/api/availabilitiesApi';
import { Alert } from 'react-native';
import { DaysOfTheWeek } from '@/types/entityTypes';

const availabilityQueryKey = (cleanerId?: number) =>
  ['availability-slots', cleanerId] as const;

export function useAvailabilities(cleanerId?: number) {
  return useQuery({
    queryKey: availabilityQueryKey(cleanerId),
    queryFn: () => getAvailabilitySlots(cleanerId!),
    enabled: cleanerId !== undefined,
  });
}

export function useCreateAvailability(cleanerId?: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: {
      dayOfWeek: DaysOfTheWeek;
      startTime: string;
      endTime: string;
    }) => createAvailabilitySlot(payload),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: availabilityQueryKey(cleanerId),
        exact: true,
        refetchType: 'active',
      }),
    onError: () =>
      Alert.alert('Unable to save', 'The availability could not be added.'),
  });
}

export function useUpdateAvailability(cleanerId?: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      ...payload
    }: {
      id: number;
      startTime: string;
      endTime: string;
    }) => updateAvailabilitySlot(id, payload),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: availabilityQueryKey(cleanerId),
        exact: true,
        refetchType: 'active',
      }),
    onError: () =>
      Alert.alert('Unable to save', 'The availability could not be updated.'),
  });
}

export function useDeleteAvailability(cleanerId?: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteAvailabilitySlot(id),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: availabilityQueryKey(cleanerId),
        exact: true,
        refetchType: 'active',
      }),
    onError: () =>
      Alert.alert('Unable to delete', 'The availability could not be removed.'),
  });
}
