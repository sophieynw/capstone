import { View } from 'react-native';
import { CircleCheck, type LucideIcon } from 'lucide-react-native';
import { Heading } from '@/components/ui/heading';
import { Text } from '@/components/ui/text';

type EmptyStateProps = {
  title: string;
  message?: string;
  icon?: LucideIcon;
  inline?: boolean; // smaller version for inside a card
};

// friendly message for when a list has nothing in it yet
export function EmptyState({
  title,
  message,
  icon = CircleCheck,
  inline = false,
}: EmptyStateProps) {
  const IconComponent = icon;

  return (
    <View
      accessible
      accessibilityLabel={message ? `${title}. ${message}` : title}
      className={
        inline
          ? 'w-full items-center gap-2 py-4'
          : 'w-full items-center gap-3 px-6 py-16'
      }
    >
      {/* green circle with the icon in it */}
      <View
        className={
          inline
            ? 'h-12 w-12 items-center justify-center rounded-full'
            : 'h-20 w-20 items-center justify-center rounded-full'
        }
        style={{ backgroundColor: '#D8E7DD' }}
      >
        <IconComponent size={inline ? 24 : 36} color='#365841' />
      </View>

      <Heading size={inline ? 'sm' : 'lg'} className='text-center'>
        {title}
      </Heading>

      {message && (
        <Text className='text-center text-muted-foreground'>{message}</Text>
      )}
    </View>
  );
}
