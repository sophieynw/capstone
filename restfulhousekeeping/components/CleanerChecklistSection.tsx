import { Pressable, Text, View } from 'react-native';
import { Circle, CircleCheck } from 'lucide-react-native';
import { Heading } from '@/components/ui/heading';
import { Icon } from '@/components/ui/icon';
import { CleaningChecklistItem } from '@/types/entityTypes';

type CleanerChecklistSectionProps = {
  items: CleaningChecklistItem[];
  onToggle: (item: CleaningChecklistItem) => void;
  pendingUpdate?: { id: number; isComplete: boolean };
  isReadOnly?: boolean;
};

export function CleanerChecklistSection({
  items,
  onToggle,
  pendingUpdate,
  isReadOnly = false,
}: CleanerChecklistSectionProps) {
  const disabled = isReadOnly || pendingUpdate !== undefined;

  return (
    <View className='flex-col justify-center gap-2'>
      <Heading size='md'>Checklist</Heading>

      <View className='py-2 gap-3'>
        {items.length > 0 ? (
          items.map((item) => {
            const isSaving = pendingUpdate?.id === item.id;
            const isComplete = isSaving
              ? pendingUpdate.isComplete
              : item.isComplete;

            return (
              <Pressable
                key={item.id}
                className='flex-row items-center gap-2'
                onPress={() => onToggle(item)}
                disabled={disabled}
                accessibilityRole='checkbox'
                accessibilityState={{
                  checked: isComplete,
                  disabled,
                  busy: isSaving,
                }}
                accessibilityLabel={item.description}
              >
                <Icon as={isComplete ? CircleCheck : Circle} />
                <Text>{item.description}</Text>
              </Pressable>
            );
          })
        ) : (
          <Text className='text-center text-sm italic text-gray-500'>
            No items found
          </Text>
        )}
      </View>
    </View>
  );
}
