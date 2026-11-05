import { Cleaner, UpdateUserPayload, User } from '@/types/entityTypes';
import { request } from '@/api/apiClient';

export function getUserById(userId: number | null): Promise<User> {
  return request<User>({
    method: 'GET',
    url: `/cleaners/${userId}`,
  });
}

export function updateUserById(
  userId: number | null,
  user: UpdateUserPayload,
): Promise<User> {
  return request<User>({
    method: 'PATCH',
    url: `/cleaners/${userId}`,
    data: user,
  });
}

export function getAllCleaners(organizationId: number): Promise<Cleaner[]> {
  return request<Cleaner[]>({
    method: 'GET',
    url: `/cleaners/${organizationId}/cleaners`,
  });
}

export function removeCleanerFromTeam(cleanerId: number): Promise<void> {
  return request<void>({
    method: 'DELETE',
    url: `/cleaners/${cleanerId}`,
  });
}
// info we need from the image picker
type PickedImage = {
  uri: string;
  mimeType?: string | null;
  fileName?: string | null;
};

// upload a profile picture, returns the updated user
export function uploadProfilePicture(
  userId: number,
  image: PickedImage,
): Promise<User> {
  const formData = new FormData();
  formData.append('file', {
    uri: image.uri,
    type: image.mimeType ?? 'image/jpeg',
    name: image.fileName ?? `profile-${Date.now()}.jpg`,
  } as any);

  return request<User>({
    method: 'POST',
    url: `/cleaners/${userId}/profile-picture`,
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

// get the saved picture path for a user (null if they don't have one)
export async function getProfilePicturePath(
  userId: number,
): Promise<string | null> {
  const path = await request<string>({
    method: 'GET',
    url: `/cleaners/${userId}/profile-picture`,
    responseType: 'text',
  });
  return path ? path : null;
}
