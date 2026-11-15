import {
  Alert,
  Image,
  Modal,
  Platform,
  Pressable,
  View,
  type ImageProps,
} from 'react-native';
import {
  GestureDetector,
  GestureHandlerRootView,
  usePinchGesture,
} from 'react-native-gesture-handler';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
} from 'react-native-reanimated';
import { ImagePlus, X } from 'lucide-react-native';
import { Heading } from '@/components/ui/heading';
import { Text } from '@/components/ui/text';
import { useContext, useRef, useState } from 'react';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { API_BASE_URL, getApiErrorMessage } from '@/api/apiClient';
import { cleaningPhotosPath } from '@/api/cleaningPhotosApi';
import { AuthContext } from '@/auth/AuthContext';
import { useCleaningPhotos } from '@/hooks/useCleaningPhotos';
import { pickLibraryPhotos } from '@/utils/photoPicker';

const MAX_PHOTOS = 50;
const MAX_ZOOM = 4;

function ZoomablePhoto({
  source,
  onError,
}: Pick<ImageProps, 'source' | 'onError'>) {
  const viewport = useSharedValue({ width: 0, height: 0 });
  const zoom = useSharedValue({ scale: 1, x: 0, y: 0 });
  const pinchStart = useSharedValue({ scale: 1, x: 0, y: 0, focalX: 0, focalY: 0 });

  const pinch = usePinchGesture({
    onActivate: ({ focalX, focalY }) => {
      'worklet';
      pinchStart.value = { ...zoom.value, focalX, focalY };
    },
    onUpdate: ({ scale, focalX, focalY }) => {
      'worklet';
      const start = pinchStart.value;
      const nextScale = Math.min(MAX_ZOOM, Math.max(1, start.scale * scale));
      const ratio = nextScale / start.scale;
      const centerX = viewport.value.width / 2;
      const centerY = viewport.value.height / 2;
      const x = focalX - centerX - (start.focalX - centerX - start.x) * ratio;
      const y = focalY - centerY - (start.focalY - centerY - start.y) * ratio;
      const limitX = centerX * (nextScale - 1);
      const limitY = centerY * (nextScale - 1);
      zoom.value = {
        scale: nextScale,
        x: Math.min(limitX, Math.max(-limitX, x)),
        y: Math.min(limitY, Math.max(-limitY, y)),
      };
    },
  });

  const imageStyle = useAnimatedStyle(() => {
    const { scale, x, y } = zoom.value;
    return { transform: [{ translateX: x }, { translateY: y }, { scale }] };
  });

  return (
    <GestureDetector gesture={pinch}>
      <View
        style={{ flex: 1, overflow: 'hidden' }}
        onLayout={({ nativeEvent: { layout: { width, height } } }) => {
          viewport.value = { width, height };
        }}
      >
        <Animated.Image
          source={source}
          style={[{ flex: 1, width: '100%' }, imageStyle]}
          resizeMode='contain'
          accessibilityLabel='Full-screen cleaning photo'
          onError={onError}
        />
      </View>
    </GestureDetector>
  );
}

type PhotosSectionProps = {
  cleaningId: number;
  canUpload: boolean;
};

export function PhotosSection({ cleaningId, canUpload }: PhotosSectionProps) {
  const { token, user } = useContext(AuthContext);
  const insets = useSafeAreaInsets();
  const { photos, uploadPhoto, deletePhoto } = useCleaningPhotos(cleaningId);
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
      const assets = (await pickLibraryPhotos(remaining)).slice(0, remaining);
      if (assets.length === 0) return;

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

  function confirmDeletePhoto(photoId: number) {
    const message = 'Permanently delete this photo?';

    if (Platform.OS === 'web') {
      if (window.confirm(message)) deletePhoto.mutate(photoId);
      return;
    }

    Alert.alert('Delete photo', message, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => deletePhoto.mutate(photoId),
      },
    ]);
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
        <View className='flex-row flex-wrap justify-center gap-3'>
          {savedPhotos.map((photo, index) => (
            <View key={photo.id}>
              <Pressable
                accessibilityRole='button'
                accessibilityLabel={`Open cleaning photo ${index + 1} full screen`}
                onPress={() => openPhoto(photo.id)}
              >
                <Image
                  source={photoSource(photo.id)}
                  style={{
                    width: 90,
                    height: 90,
                    borderRadius: 12,
                    backgroundColor: '#e5e7eb',
                  }}
                  resizeMode='cover'
                />
              </Pressable>
              {canUpload && photo.uploaderId === user?.id && (
                <Pressable
                  className='absolute right-1 top-1 h-6 w-6 items-center justify-center rounded-full bg-black/70'
                  accessibilityRole='button'
                  accessibilityLabel={`Delete cleaning photo ${index + 1}`}
                  hitSlop={6}
                  disabled={deletePhoto.isPending}
                  onPress={() => confirmDeletePhoto(photo.id)}
                >
                  <X color='white' size={14} />
                </Pressable>
              )}
            </View>
          ))}
        </View>
      )}

      {deletePhoto.isError && (
        <Text accessibilityLiveRegion='polite'>
          {getApiErrorMessage(
            deletePhoto.error,
            'Could not delete the photo. Try again.',
          )}
        </Text>
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
        <GestureHandlerRootView
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
              <ZoomablePhoto
                key={selectedPhotoId}
                source={photoSource(selectedPhotoId)}
                onError={() => setImageError(true)}
              />
            )
          )}
        </GestureHandlerRootView>
      </Modal>
    </View>
  );
}
