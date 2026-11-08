import { request } from '@/api/apiClient';
import type { CleaningPhoto } from '@/types/entityTypes';
import type { ImagePickerAsset } from 'expo-image-picker';

export function cleaningPhotosPath(cleaningId: number) {
  return `/photos/cleaning/${cleaningId}`;
}

export function getCleaningPhotos(cleaningId: number) {
  return request<CleaningPhoto[]>({
    method: 'GET',
    url: cleaningPhotosPath(cleaningId),
  });
}

export function uploadCleaningPhoto(
  cleaningId: number,
  image: ImagePickerAsset,
) {
  const data = new FormData();
  if (image.file) {
    data.append('file', image.file);
  } else {
    // React Native accepts a local URI as a multipart file.
    data.append('file', {
      uri: image.uri,
      type: image.mimeType ?? 'image/jpeg',
      name: image.fileName ?? 'cleaning-photo.jpg',
    } as unknown as Blob);
  }

  return request<CleaningPhoto>({
    method: 'POST',
    url: cleaningPhotosPath(cleaningId),
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60_000,
  });
}
