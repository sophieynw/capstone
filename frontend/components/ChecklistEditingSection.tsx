import { useEffect, useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { Heading } from '@/components/ui/heading';
import { Input, InputField } from '@/components/ui/input';
import { Plus, Trash } from 'lucide-react-native';
import { ChecklistItem } from '@/types/entityTypes';
import { styles } from '@/styles/styles';
import { Card } from '@/components/ui/card';
import { Icon } from '@/components/ui/icon';

type ChecklistItemInputProps = {
  item: ChecklistItem;
  onDelete: () => void;
  onChangeDescription: (description: string) => void;
  onChangeFrequencyDays: (frequencyDays: number | null) => void;
};

function ChecklistItemInput({
  item,
  onDelete,
  onChangeDescription,
  onChangeFrequencyDays,
}: ChecklistItemInputProps) {
  return (
    <View className='flex-row items-center gap-2'>
      <Input className='flex-1 rounded-full'>
        <InputField
          value={item.description}
          onChangeText={onChangeDescription}
          placeholder='Checklist item'
        />
      </Input>
      <Text>every</Text>
      <Input className='w-11 rounded-full'>
        <InputField
          value={item.frequencyDays?.toString() ?? ''}
          onChangeText={(value) =>
            onChangeFrequencyDays(parseFrequencyDays(value))
          }
          placeholder='Days'
          keyboardType='number-pad'
          accessibilityLabel='Frequency in days'
        />
      </Input>
      <Text>days</Text>
      <Pressable
        onPress={onDelete}
        className='h-10 w-10 items-center justify-center'
        accessibilityRole='button'
        accessibilityLabel={`Delete ${item.description}`}
      >
        <Icon as={Trash} size='sm' style={styles.buttonTextWarn} />
      </Pressable>
    </View>
  );
}

function parseFrequencyDays(value: string): number | null {
  const digits = value.replace(/\D/g, '');
  return digits ? Number(digits) : null;
}

type ChecklistSectionProps = {
  items: ChecklistItem[];
  onItemsChange: (items: ChecklistItem[]) => void;
};

export function ChecklistEditingSection({
  items,
  onItemsChange,
}: ChecklistSectionProps) {
  const [editableItems, setEditableItems] = useState<ChecklistItem[]>(items);
  const [newItemDescription, setNewItemDescription] = useState<string>('');
  const [newItemFrequencyDays, setNewItemFrequencyDays] = useState<
    number | null
  >(1);

  useEffect(() => {
    setEditableItems(items);
  }, [items]);

  function commitItems(nextItems: ChecklistItem[]) {
    setEditableItems(nextItems);
    onItemsChange(nextItems);
  }

  function addItem() {
    const description = newItemDescription.trim();

    if (!description) return;

    const newItem: ChecklistItem = {
      // Temporary client-side ID.
      id: -Date.now(),
      description,
      frequencyDays: newItemFrequencyDays,
      lastCompleted: null,
    };

    commitItems([...editableItems, newItem]);
    setNewItemDescription('');
    setNewItemFrequencyDays(1);
  }

  function deleteItem(id: number) {
    commitItems(editableItems.filter((item) => item.id !== id));
  }

  function updateItemDescription(id: number, description: string) {
    commitItems(
      editableItems.map((item) =>
        item.id === id ? { ...item, description } : item,
      ),
    );
  }

  function updateItemFrequencyDays(id: number, frequencyDays: number | null) {
    commitItems(
      editableItems.map((item) =>
        item.id === id ? { ...item, frequencyDays } : item,
      ),
    );
  }

  return (
    <Card style={styles.mediumCard}>
      <View className='flex-col justify-center gap-2'>
        <Heading size='md'>Checklist</Heading>

        <View className='py-2 gap-3'>
          {editableItems.map((item) => (
            <ChecklistItemInput
              key={item.id}
              item={item}
              onDelete={() => deleteItem(item.id)}
              onChangeDescription={(description) =>
                updateItemDescription(item.id, description)
              }
              onChangeFrequencyDays={(frequencyDays) =>
                updateItemFrequencyDays(item.id, frequencyDays)
              }
            />
          ))}

          <View className='flex-row items-center gap-2'>
            <Input className='flex-1 rounded-full'>
              <InputField
                value={newItemDescription}
                onChangeText={setNewItemDescription}
                onSubmitEditing={addItem}
                placeholder='Add checklist item'
                returnKeyType='done'
              />
            </Input>

            <Text>every</Text>
            <Input className='w-11 rounded-full'>
              <InputField
                value={newItemFrequencyDays?.toString() ?? ''}
                onChangeText={(value) =>
                  setNewItemFrequencyDays(parseFrequencyDays(value))
                }
                onSubmitEditing={addItem}
                placeholder='Days'
                keyboardType='number-pad'
                returnKeyType='done'
                accessibilityLabel='Frequency in days'
              />
            </Input>
            <Text>days</Text>
            <Pressable
              onPress={addItem}
              className='h-10 w-10 items-center justify-center'
              accessibilityRole='button'
              accessibilityLabel='Add checklist item'
            >
              <Icon as={Plus} size='sm' style={styles.buttonText} />
            </Pressable>
          </View>
        </View>
      </View>
    </Card>
  );
}
