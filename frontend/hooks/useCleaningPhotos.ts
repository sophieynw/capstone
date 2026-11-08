import { useContext } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ImagePickerAsset } from 'expo-image-picker';
import {
  getCleaningPhotos,
  uploadCleaningPhoto,
} from '@/api/cleaningPhotosApi';
import { AuthContext } from '@/auth/AuthContext';
import type { CleaningPhoto } from '@/types/entityTypes';

export function useCleaningPhotos(cleaningId: number) {
  const { user } = useContext(AuthContext);
  const queryClient = useQueryClient();
  const queryKey = ['cleaning-photos', cleaningId, user?.id];

  const photos = useQuery({
    queryKey,
    queryFn: () => getCleaningPhotos(cleaningId),
    enabled: !!user?.id,
  });

  const uploadPhoto = useMutation({
    mutationFn: (asset: ImagePickerAsset) =>
      uploadCleaningPhoto(cleaningId, asset),
    onSuccess: async (photo) => {
      await queryClient.cancelQueries({ queryKey, exact: true });
      queryClient.setQueryData<CleaningPhoto[]>(queryKey, (current) => [
        ...(current ?? []).filter((item) => item.id !== photo.id),
        photo,
      ]);
    },
  });

  return { photos, uploadPhoto };
}
