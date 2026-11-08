import { useState } from 'react';
import { StyleProp, ViewStyle } from 'react-native';
import {
  Avatar,
  AvatarFallbackText,
  AvatarImage,
} from '@/components/ui/avatar';
import { useProfilePicture } from '@/hooks/useUsers';

type UserAvatarProps = {
  userId?: number | null;
  name: string;
  className?: string;
  style?: StyleProp<ViewStyle>;
};

// shows a user's profile picture, or their initials if they don't have one
export function UserAvatar({ userId, name, className, style }: UserAvatarProps) {
  const { data: picture } = useProfilePicture(userId ?? undefined);

  // show initials instead if the image doesn't load
  const [failedImage, setFailedImage] = useState<string | null>(null);
  const showImage = !!picture && picture !== failedImage;

  return (
    <Avatar className={className} style={style}>
      <AvatarFallbackText>{name}</AvatarFallbackText>
      {showImage && (
        <AvatarImage
          source={{ uri: picture }}
          onError={() => setFailedImage(picture)}
        />
      )}
    </Avatar>
  );
}
