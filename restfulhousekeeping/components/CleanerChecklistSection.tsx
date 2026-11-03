import { useState } from 'react';
import { Alert, Pressable, Text, View } from 'react-native';
import { Circle, CircleCheck, Save } from 'lucide-react-native';
import { Heading } from '@/components/ui/heading';
import { Icon } from '@/components/ui/icon';
import { CleaningChecklistItem } from '@/types/entityTypes';
import { styles } from '@/styles/styles';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';

export type ChecklistItemUpdate = { id: number; isComplete: boolean };

type CleanerChecklistItemProps = {
  item: CleaningChecklistItem;
  isComplete: boolean;
  onToggle: () => void;
  disabled?: boolean;
};

function CleanerChecklistItem({
  item,
  isComplete,
  onToggle,
  disabled = false,
}: CleanerChecklistItemProps) {
  //const [isComplete, setIsComplete] = useState(item.isComplete ?? false);

  return (
    <View className='flex-row items-center gap-2'>
      <Pressable
        //onPress={() => setIsComplete((currentValue) => !currentValue)}
        onPress={onToggle}
        accessibilityRole='checkbox'
        accessibilityState={{ checked: isComplete }}
        accessibilityLabel={`Mark ${item.description} as ${
          isComplete ? 'incomplete' : 'complete'
        }`}
      >
        <Icon as={isComplete ? CircleCheck : Circle} />
      </Pressable>
      <Text>{item.description}</Text>
    </View>
  );
}

type CleanerChecklistSectionProps = {
  items: CleaningChecklistItem[];
  onSave: (updates: ChecklistItemUpdate[]) => Promise<void>;
  isReadOnly?: boolean;
};

export function CleanerChecklistSection({
  items,
  onSave,
  isReadOnly = false,
}: CleanerChecklistSectionProps) {

  const [overrides, setOverrides] = useState<Record<number, boolean>>({});
  const [isSaving, setIsSaving] = useState(false);

  const pendingUpdates: ChecklistItemUpdate[] = items
    .filter(
      (item) => item.id in overrides && overrides[item.id] !== item.isComplete,
    )
    .map((item) => ({ id: item.id, isComplete: overrides[item.id] }));

  function toggleItem(item: CleaningChecklistItem) {
    setOverrides((prev) => ({
      ...prev,
      [item.id]: !(prev[item.id] ?? item.isComplete),
    }));
  }

  async function handleSave() {
    if (pendingUpdates.length === 0 || isSaving) return;

    setIsSaving(true);
    try {
      await onSave(pendingUpdates);
      setOverrides({});
    } catch (error) {
      console.error('Update cleaning checklist error:', error);
      Alert.alert(
        'Unable to update checklist',
        'Your changes could not be saved. Please try again.',
      );
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <View className='flex-col justify-center gap-2'>
      <Heading size='md'>Checklist</Heading>

      <View className='py-2 gap-3'>
        {items.length > 0 ? (
          items.map((item) => (
            <CleanerChecklistItem
              key={item.id}
              item={item}
              isComplete={overrides[item.id] ?? item.isComplete}
              onToggle={() => toggleItem(item)}
              disabled={isReadOnly || isSaving}
            />
          ))
        ) : (
          <Text className='text-center text-sm italic text-gray-500'>
            No items found
          </Text>
        )}
      </View>

      {!isReadOnly && items.length > 0 && (
        <Button
          className='rounded-full'
          style={styles.button}
          onPress={handleSave}
          isDisabled={pendingUpdates.length === 0 || isSaving}
        >
          <ButtonIcon style={styles.buttonText} as={Save} />
          <ButtonText style={styles.buttonText}>
            {isSaving ? 'Updating...' : 'Update Checklist'}
          </ButtonText>
        </Button>
      )}
    </View>
  );
}
