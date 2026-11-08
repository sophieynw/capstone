import {
  launchImageLibraryAsync,
  UIImagePickerPreferredAssetRepresentationMode,
} from 'expo-image-picker';

export async function pickLibraryPhotos(selectionLimit?: number) {
  const result = await launchImageLibraryAsync({
    mediaTypes: ['images'],
    allowsEditing: false,
    allowsMultipleSelection: selectionLimit !== undefined,
    selectionLimit,
    preferredAssetRepresentationMode:
      UIImagePickerPreferredAssetRepresentationMode.Compatible,
    quality: 0.8,
  });

  return result.canceled ? [] : result.assets;
}
