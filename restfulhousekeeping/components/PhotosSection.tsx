import { Image, Modal, Pressable, View } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { ImagePlus, X } from 'lucide-react-native';
import { Heading } from '@/components/ui/heading';
import { Text } from '@/components/ui/text';
import { useContext, useRef, useState } from 'react';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { API_BASE_URL, getApiErrorMessage } from '@/api/apiClient';
import { cleaningPhotosPath } from '@/api/cleaningPhotosApi';
import { AuthContext } from '@/auth/AuthContext';
import { useCleaningPhotos } from '@/hooks/useCleaningPhotos';

const MAX_PHOTOS = 10;

type PhotosSectionProps = {
  cleaningId: number;
  canUpload: boolean;
};

export function PhotosSection({ cleaningId, canUpload }: PhotosSectionProps) {
  const { token } = useContext(AuthContext);
  const insets = useSafeAreaInsets();
  const { photos, uploadPhoto } = useCleaningPhotos(cleaningId);
  const [selectedPhotoId, setSelectedPhotoId] = useState<number>();
  const [imageError, setImageError] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [busyLabel, setBusyLabel] = useState('');
  const uploadInFlight = useRef(false);
  const savedPhotos = photos.data ?? [];
  const remaining = MAX_PHOTOS - savedPhotos.length;

  function photoSource(photoId: number) {
    return {
      uri: `${API_BASE_URL}${cleaningPhotosPath(cleaningId)}/${photoId}`,
      headers: { Authorization: `Bearer ${token}` },
    };
  }

  async function pickPhotos() {
    if (uploadInFlight.current || remaining <= 0) return;

    uploadInFlight.current = true;
    setBusyLabel('Opening photos…');
    setUploadError('');
    let uploading = false;
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsMultipleSelection: true,
        selectionLimit: remaining,
        preferredAssetRepresentationMode:
          ImagePicker.UIImagePickerPreferredAssetRepresentationMode.Compatible,
        quality: 0.8,
      });

      if (result.canceled) return;

      const assets = result.assets.slice(0, remaining);
      uploading = true;
      for (const [index, asset] of assets.entries()) {
        setBusyLabel(`Uploading ${index + 1} of ${assets.length}…`);
        await uploadPhoto.mutateAsync(asset);
      }
    } catch (error) {
      setUploadError(
        uploading
          ? getApiErrorMessage(
              error,
              'Upload stopped. Choose any photos that were skipped and try again.',
            )
          : 'Could not open your photo library. Try again.',
      );
    } finally {
      uploadInFlight.current = false;
      setBusyLabel('');
      if (uploading) void photos.refetch();
    }
  }

  function openPhoto(photoId: number) {
    setImageError(false);
    setSelectedPhotoId(photoId);
  }

  function closePhoto() {
    setSelectedPhotoId(undefined);
  }

  return (
    <View className='gap-3'>
      <Heading size='md'>Photos</Heading>

      {photos.isPending && <Text>Loading photos…</Text>}
      {photos.isError && (
        <Pressable
          accessibilityRole='button'
          onPress={() => void photos.refetch()}
        >
          <Text>Unable to load photos. Tap to retry.</Text>
        </Pressable>
      )}
      {photos.isSuccess && savedPhotos.length === 0 && (
        <Text>No photos yet.</Text>
      )}

      {savedPhotos.length > 0 && token && (
        <View className='flex-row flex-wrap gap-3'>
          {savedPhotos.map((photo, index) => (
            <Pressable
              key={photo.id}
              accessibilityRole='button'
              accessibilityLabel={`Open cleaning photo ${index + 1} full screen`}
              onPress={() => openPhoto(photo.id)}
            >
              <Image
                source={photoSource(photo.id)}
                style={{
                  width: 96,
                  height: 96,
                  borderRadius: 12,
                  backgroundColor: '#e5e7eb',
                }}
                resizeMode='cover'
              />
            </Pressable>
          ))}
        </View>
      )}

      {canUpload && photos.isSuccess && remaining > 0 && (
        <Pressable
          onPress={pickPhotos}
          disabled={!!busyLabel}
          className='min-h-32 items-center justify-center gap-2 rounded-3xl border border-dashed border-gray-300 bg-gray-50 px-4 py-6'
          accessibilityRole='button'
          accessibilityLabel='Upload photos'
        >
          <ImagePlus size={28} />
          <Text className='font-medium'>{busyLabel || 'Upload photos'}</Text>
          <Text className='text-center text-sm text-gray-500'>
            {remaining} photo {remaining === 1 ? 'slot' : 'slots'} available
          </Text>
        </Pressable>
      )}
      {canUpload && uploadError && (
        <Text accessibilityLiveRegion='polite'>{uploadError}</Text>
      )}

      <Modal
        visible={selectedPhotoId !== undefined && !!token}
        presentationStyle='fullScreen'
        animationType='fade'
        onRequestClose={closePhoto}
      >
        <View
          style={{
            flex: 1,
            backgroundColor: 'black',
            paddingTop: insets.top,
            paddingBottom: insets.bottom,
          }}
        >
          <Pressable
            accessibilityRole='button'
            accessibilityLabel='Close full-screen photo'
            className='h-12 w-12 items-center justify-center self-end'
            onPress={closePhoto}
          >
            <X color='white' size={28} />
          </Pressable>
          {imageError ? (
            <Text className='p-4 text-center text-white'>
              Unable to load this photo.
            </Text>
          ) : (
            selectedPhotoId !== undefined && (
              <Image
                source={photoSource(selectedPhotoId)}
                style={{ flex: 1, width: '100%' }}
                resizeMode='contain'
                accessibilityLabel='Full-screen cleaning photo'
                onError={() => setImageError(true)}
              />
            )
          )}
        </View>
      </Modal>
    </View>
  );
}
