// hooks/useCleanings.ts
import { useContext } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AuthContext } from '@/auth/AuthContext';
import {
  addCleaningChecklistItem,
  completeCleaning,
  createCleaning,
  deleteCleaning,
  deleteCleaningChecklistItem,
  getCleaningById,
  getCompletedCleanings,
  getNextCleaningByProperty,
  getUpcomingCleanings,
  updateCleaningCleaner,
  updateCleaningSchedule,
} from '@/api/cleaningsApi';
import { Cleaning } from '@/types/entityTypes';
import { Alert } from 'react-native';
import { useNavigation } from '@react-navigation/native';

// fetches all upcoming cleanings for a user
export function useUpcomingCleanings() {
  const { user } = useContext(AuthContext);

  return useQuery({
    queryKey: ['upcoming-cleanings', user?.id],
    queryFn: () => getUpcomingCleanings(user!.id),
    enabled: !!user?.id,

    select: (cleanings) =>
      [...cleanings].sort(
        (a, b) =>
          new Date(a.dateTimeStart).getTime() -
          new Date(b.dateTimeStart).getTime(),
      ),
  });
}

export function useCompletedCleanings() {
  const { user } = useContext(AuthContext);

  return useQuery({
    queryKey: ['completed-cleanings', user?.id],
    queryFn: () => getCompletedCleanings(user!.id),
    enabled: !!user?.id,
  });
}

export function useNextCleaningByProperty(propertyId: number) {
  const { user } = useContext(AuthContext);

  return useQuery({
    queryKey: ['next-property-cleaning', propertyId],
    queryFn: () => getNextCleaningByProperty(propertyId),
    enabled: !!propertyId,
  });
}

export function useCleaningById(id: number) {
  const { user } = useContext(AuthContext);

  return useQuery({
    queryKey: ['cleaning-id', id],
    queryFn: () => getCleaningById(id),
    enabled: !!user?.id,
  });
}

// creates a new Cleaning record
export function useCreateCleaning() {
  const queryClient = useQueryClient();
  const navigation = useNavigation();

  return useMutation({
    mutationFn: createCleaning,

    onSuccess: async (createdCleaning) => {
      queryClient.setQueryData(
        ['cleaning-id', createdCleaning.id],
        createdCleaning,
      );

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['upcoming-cleanings', createdCleaning.managerId],
          exact: true,
          refetchType: 'all',
        }),

        queryClient.invalidateQueries({
          queryKey: ['next-property-cleaning', createdCleaning.property.id],
          exact: true,
          refetchType: 'all',
        }),
      ]);

      Alert.alert('Success', 'The cleaning was created.', [
        {
          text: 'OK',
          onPress: () => navigation.goBack(),
        },
      ]);
    },

    onError: (error) => {
      console.error('Create cleaning error:', error);

      Alert.alert('Unable to save', 'The cleaning could not be created.');
    },
  });
}

// marks cleaning as complete
export function useCompleteCleaning() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: completeCleaning,
    onSuccess: (updatedCleaning) => {
      queryClient.setQueryData(
        ['cleaning-id', updatedCleaning.id],
        updatedCleaning,
      );
      queryClient.invalidateQueries({
        queryKey: ['upcoming-cleanings'],
        refetchType: 'all',
      });

      queryClient.invalidateQueries({
        queryKey: ['completed-cleanings'],
        refetchType: 'all',
      });
    },
  });
}

export function useUpdateCleaningCleaner() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      cleaningId,
      cleanerId,
    }: {
      cleaningId: number;
      cleanerId: number | null;
    }) => updateCleaningCleaner(cleaningId, cleanerId),

    onSuccess: async (updatedCleaning) => {
      queryClient.setQueryData(
        ['cleaning-id', updatedCleaning.id],
        updatedCleaning,
      );

      queryClient.setQueriesData<Cleaning[]>(
        { queryKey: ['upcoming-cleanings'] },
        (cleanings) =>
          cleanings?.map((cleaning) =>
            cleaning.id === updatedCleaning.id ? updatedCleaning : cleaning,
          ),
      );

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['upcoming-cleanings'],
          refetchType: 'all',
        }),
        queryClient.invalidateQueries({
          queryKey: ['next-property-cleaning', updatedCleaning.property.id],
          refetchType: 'all',
        }),
      ]);
    },
  });
}

export function useUpdateCleaningSchedule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      cleaningId,
      dateTimeStart,
      dateTimeEnd,
    }: {
      cleaningId: number;
      dateTimeStart: string;
      dateTimeEnd: string;
    }) => updateCleaningSchedule(cleaningId, dateTimeStart, dateTimeEnd),

    onSuccess: async (updatedCleaning) => {
      queryClient.setQueryData(
        ['cleaning-id', updatedCleaning.id],
        updatedCleaning,
      );

      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['upcoming-cleanings'],
          refetchType: 'all',
        }),
        queryClient.invalidateQueries({
          queryKey: ['next-property-cleaning', updatedCleaning.property.id],
          refetchType: 'all',
        }),
      ]);
    },
  });
}

export function useDeleteCleaning() {
  const queryClient = useQueryClient();
  const navigation = useNavigation();

  return useMutation({
    mutationFn: ({ cleaningId }: { cleaningId: number; propertyId: number }) =>
      deleteCleaning(cleaningId),

    onSuccess: (_, { cleaningId, propertyId }) => {
      Alert.alert('Success', 'The cleaning was deleted.', [
        {
          text: 'OK',
          onPress: () => {
            navigation.goBack();
            setTimeout(() => {
              queryClient.removeQueries({
                queryKey: ['cleaning-id', cleaningId],
                exact: true,
              });
            }, 0);
          },
        },
      ]);

      queryClient.invalidateQueries({
        queryKey: ['upcoming-cleanings'],
        refetchType: 'all',
      });
      queryClient.invalidateQueries({
        queryKey: ['next-property-cleaning', propertyId],
        exact: true,
        refetchType: 'all',
      });
    },

    onError: (error) => {
      console.error('Delete cleaning error:', error);

      Alert.alert('Unable to delete', 'The cleaning could not be deleted.');
    },
  });
}

export function useAddCleaningChecklistItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      cleaningId,
      description,
    }: {
      cleaningId: number;
      propertyId: number;
      description: string;
    }) => addCleaningChecklistItem(cleaningId, description),

    onSuccess: (createdItem, { cleaningId, propertyId }) => {
      const addItem = (cleaning: Cleaning | undefined) =>
        cleaning?.id === cleaningId
          ? {
              ...cleaning,
              cleaningChecklistItems: [
                ...cleaning.cleaningChecklistItems,
                createdItem,
              ],
            }
          : cleaning;

      queryClient.setQueryData<Cleaning>(['cleaning-id', cleaningId], addItem);

      queryClient.setQueriesData<Cleaning[]>(
        { queryKey: ['upcoming-cleanings'] },
        (cleanings) => cleanings?.map((cleaning) => addItem(cleaning)!),
      );

      queryClient.setQueryData<Cleaning>(
        ['next-property-cleaning', propertyId],
        addItem,
      );

      queryClient.invalidateQueries({
        queryKey: ['upcoming-cleanings'],
        refetchType: 'all',
      });

      queryClient.invalidateQueries({
        queryKey: ['next-property-cleaning', propertyId],
        exact: true,
        refetchType: 'all',
      });
    },
  });
}

export function useDeleteCleaningChecklistItem() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      cleaningId,
      itemId,
    }: {
      cleaningId: number;
      propertyId: number;
      itemId: number;
    }) => deleteCleaningChecklistItem(cleaningId, itemId),

    onSuccess: (_, { cleaningId, propertyId, itemId }) => {
      const removeItem = (cleaning: Cleaning | undefined) =>
        cleaning?.id === cleaningId
          ? {
              ...cleaning,
              cleaningChecklistItems: cleaning.cleaningChecklistItems.filter(
                (item) => item.id !== itemId,
              ),
            }
          : cleaning;

      queryClient.setQueryData<Cleaning>(
        ['cleaning-id', cleaningId],
        removeItem,
      );

      queryClient.setQueriesData<Cleaning[]>(
        { queryKey: ['upcoming-cleanings'] },
        (cleanings) => cleanings?.map((cleaning) => removeItem(cleaning)!),
      );

      queryClient.setQueryData<Cleaning>(
        ['next-property-cleaning', propertyId],
        removeItem,
      );

      queryClient.invalidateQueries({
        queryKey: ['upcoming-cleanings'],
        refetchType: 'all',
      });

      queryClient.invalidateQueries({
        queryKey: ['next-property-cleaning', propertyId],
        exact: true,
        refetchType: 'all',
      });
    },
  });
}
