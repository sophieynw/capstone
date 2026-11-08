// hooks/useProperties.ts
import { useContext } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AuthContext } from '@/auth/AuthContext';
import {
  createProperty,
  deletePropertyById,
  getAllProperties,
  getPropertyById,
  importAirbnbCalendar,
  uploadPropertyPicture,
  updatePropertyById,
} from '@/api/propertiesApi';
import { useNavigation } from '@react-navigation/native';
import { Alert } from 'react-native';
import { Property, Role, UpdatePropertyPayload } from '@/types/entityTypes';

export function usePropertyById(propertyId: number) {
  return useQuery({
    queryKey: ['property', propertyId],
    queryFn: () => getPropertyById(propertyId),
    enabled: !!propertyId,
  });
}

export function usePropertyAll() {
  const { user } = useContext(AuthContext);

  return useQuery({
    queryKey: ['properties', user?.id],
    queryFn: () => getAllProperties(),
    enabled: user?.role === Role.MANAGER,
  });
}

export function useUploadPropertyPicture() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      propertyId,
      image,
    }: {
      propertyId: number;
      image: Parameters<typeof uploadPropertyPicture>[1];
    }) => uploadPropertyPicture(propertyId, image),

    onSuccess: async (updatedProperty) => {
      queryClient.setQueryData(
        ['property', updatedProperty.id],
        updatedProperty,
      );

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['properties'] }),
        queryClient.invalidateQueries({ queryKey: ['upcoming-cleanings'] }),
        queryClient.invalidateQueries({ queryKey: ['cleaning-id'] }),
        queryClient.invalidateQueries({ queryKey: ['due-checklist-items'] }),
        queryClient.invalidateQueries({ queryKey: ['completed-cleanings'] }),
      ]);
    },

    onError: (error) => {
      console.error('Upload property picture error:', error);
      Alert.alert(
        'Upload failed',
        'The property picture could not be uploaded.',
      );
    },
  });
}

// creates a new Property record
export function useCreateProperty() {
  const queryClient = useQueryClient();
  const navigation = useNavigation();

  return useMutation({
    mutationFn: createProperty,

    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ['properties'],
        }),
        queryClient.invalidateQueries({
          queryKey: ['checklist-items'],
        }),
      ]);

      Alert.alert('Success', 'The property was created.', [
        {
          text: 'OK',
          onPress: () => navigation.goBack(),
        },
      ]);
    },

    onError: (error) => {
      console.error('Create property error:', error);

      Alert.alert('Unable to save', 'The property could not be created.');
    },
  });
}

export function useDeleteProperty() {
  const queryClient = useQueryClient();
  const navigation = useNavigation();
  const { user } = useContext(AuthContext);

  return useMutation({
    mutationFn: deletePropertyById,

    onSuccess: async (_, propertyId) => {
      navigation.goBack();
      queryClient.setQueryData<Property[]>(
        ['properties', user?.id],
        (properties = []) =>
          properties.filter((property) => property.id !== propertyId),
      );
      queryClient.removeQueries({
        queryKey: ['property', propertyId],
        exact: true,
      });
      queryClient.removeQueries({
        queryKey: ['checklist-items', propertyId],
        exact: true,
      });
      queryClient.removeQueries({
        queryKey: ['next-property-cleaning', propertyId],
        exact: true,
      });

      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['properties', user?.id] }),
        queryClient.invalidateQueries({ queryKey: ['upcoming-cleanings'] }),
        queryClient.invalidateQueries({ queryKey: ['cleaning-id'] }),
        queryClient.invalidateQueries({ queryKey: ['completed-cleanings'] }),
      ]);

      Alert.alert('Success', 'The property was deleted.');
    },
    onError: (error) => {
      console.error('Delete property error:', error);

      Alert.alert('Unable to delete', 'The property could not be deleted.');
    },
  });
}

export function useUpdateProperty() {
  const queryClient = useQueryClient();
  const navigation = useNavigation();
  const { user } = useContext(AuthContext);

  return useMutation({
    mutationFn: ({
      propertyId,
      property,
    }: {
      propertyId: number;
      property: UpdatePropertyPayload;
    }) => updatePropertyById(propertyId, property),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['properties', user?.id],
      });
      await queryClient.invalidateQueries({ queryKey: ['checklist-items'] });

      Alert.alert('Success', 'The property was updated.', [
        { text: 'OK', onPress: () => navigation.goBack() },
      ]);
    },

    onError: (error) => {
      console.error('Update property error:', error);
      Alert.alert('Unable to update', 'The property could not be updated.');
    },
  });
}

export function useImportAirbnbCalendar() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      propertyId,
      icalUrl,
    }: {
      propertyId: number;
      icalUrl: string;
    }) => importAirbnbCalendar(propertyId, icalUrl),

    onSuccess: async (result) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['property'] }),
        queryClient.invalidateQueries({ queryKey: ['properties'] }),
        queryClient.invalidateQueries({ queryKey: ['upcoming-cleanings'] }),
        queryClient.invalidateQueries({ queryKey: ['next-property-cleaning'] }),
        queryClient.invalidateQueries({ queryKey: ['cleaning-id'] }),
      ]);

      Alert.alert(
        'Calendar imported',
        [
          `${result.cleaningsCreated} cleaning(s) created.`,
          `${result.duplicatesSkipped} duplicate reservation(s) skipped.`,
          `${result.pastReservationsIgnored} past reservation(s) ignored.`,
          `${result.eventsIgnored} non-reservation event(s) ignored.`,
        ].join('\n'),
      );
    },

    onError: (error) => {
      console.error('Airbnb calendar import error:', error);
      Alert.alert(
        'Unable to import calendar',
        'Check the Airbnb calendar link and try again.',
      );
    },
  });
}
