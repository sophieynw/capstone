import { View, type ViewProps } from 'react-native';
import { Frown } from 'lucide-react-native';

import { Icon } from '@/components/ui/icon';
import { Text } from '@/components/ui/text';

const DEFAULT_ERROR_MESSAGE = 'Please try again later!';

type ErrorStateProps = ViewProps & {
  message?: string;
  inline?: boolean;
  className?: string;
};

export function ErrorState({
  message = DEFAULT_ERROR_MESSAGE,
  inline = false,
  className = '',
  ...viewProps
}: ErrorStateProps) {
  const containerClassName = inline
    ? 'min-h-24 w-full items-center justify-center gap-3 py-4'
    : 'flex-1 items-center justify-center gap-3 bg-background';

  return (
    <View
      accessible
      accessibilityLabel={message}
      accessibilityLiveRegion='assertive'
      {...viewProps}
      className={`${containerClassName} ${className}`.trim()}
    >
      <Icon as={Frown} size='xl' className='text-muted-foreground' />
      <Text className='px-6 text-center text-muted-foreground'>{message}</Text>
    </View>
  );
}
