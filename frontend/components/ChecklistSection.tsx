import { useEffect, useState } from 'react';
import { Alert, Pressable, View } from 'react-native';
import { Heading } from '@/components/ui/heading';
import { Input, InputField, InputIcon, InputSlot } from '@/components/ui/input';
import { Circle, CircleCheck, Plus, Trash } from 'lucide-react-native';
import { styles } from '@/styles/styles';
type ChecklistSectionItem = {
  id: number;
  description: string;
  isComplete?: boolean;
};

// region ChecklistItemInput

/**
 * This is the component which renders each item that is passed to the
 * ChecklistSection. It conditionally renders a Circle/CircleCheck icon
 * if the ChecklistItem has an 'isComplete' attribute, meaning that it is
 * a CleaningChecklistItem record in the backend (attached to a cleaning).
 */

type ChecklistItemInputProps = {
  item: ChecklistSectionItem;
  onDelete: () => void;
  onChangeDescription: (description: string) => void;
  isDeleting?: boolean;
  isReadOnly?: boolean;
};

function ChecklistItemInput({
  item,
  onDelete,
  onChangeDescription,
  isDeleting = false,
  isReadOnly = false,
}: ChecklistItemInputProps) {
  const isPreloadedItem = item.id > 0;

  return (
    <Input className='rounded-full'>
      {item.isComplete !== undefined && (
        <Pressable>
          <InputIcon as={item.isComplete ? CircleCheck : Circle} />
        </Pressable>
      )}
      <InputField
        value={item.description}
        editable={!isPreloadedItem}
        onChangeText={onChangeDescription}
        placeholder='Checklist item'
      />

      {!isReadOnly && (
        <InputSlot className='pl-3'>
          <Pressable onPress={onDelete} disabled={isDeleting} className='ml-3'>
            <InputIcon as={Trash} style={styles.buttonTextWarn} />
          </Pressable>
        </InputSlot>
      )}
    </Input>
  );
}

// endregion ChecklistItemInput

type ChecklistSectionProps<T extends ChecklistSectionItem> = {
  items: T[];
  onItemsChange: (items: T[]) => void;
  onAddItem?: (description: string) => Promise<T>;
  onDeleteItem?: (item: T) => Promise<void>;
  isReadOnly?: boolean;
};

export function ChecklistSection<T extends ChecklistSectionItem>({
  items,
  onItemsChange,
  onAddItem,
  onDeleteItem,
  isReadOnly = false,
}: ChecklistSectionProps<T>) {
  const [editableItems, setEditableItems] = useState<T[]>([]);
  const [newItemDescription, setNewItemDescription] = useState<string>('');
  const [isAdding, setIsAdding] = useState(false);
  const [deletingItemId, setDeletingItemId] = useState<number | null>(null);
  const isPersisting = isAdding || deletingItemId !== null;

  useEffect(() => {
    setEditableItems(items);
  }, [items]);

  function commitItems(nextItems: T[]) {
    setEditableItems(nextItems);
    onItemsChange(nextItems);
  }

  async function addItem() {
    const description = newItemDescription.trim();

    if (!description || isPersisting) return;

    if (onAddItem) {
      setIsAdding(true);

      try {
        const createdItem = await onAddItem(description);
        commitItems([...editableItems, createdItem]);
        setNewItemDescription('');
      } catch (error) {
        console.error('Add cleaning checklist item error:', error);
        Alert.alert(
          'Unable to add item',
          'The checklist item could not be added.',
        );
      } finally {
        setIsAdding(false);
      }

      return;
    }

    const newItem = {
      // Temporary client-side ID.
      id: -Date.now(),
      description,
      frequencyDays: 1,
      lastCompleted: null,
    } as unknown as T;

    commitItems([...editableItems, newItem]);
    setNewItemDescription('');
  }

  async function deleteItem(item: T) {
    if (!onDeleteItem) {
      commitItems(editableItems.filter(({ id }) => id !== item.id));
      return;
    }

    if (isPersisting) return;

    setDeletingItemId(item.id);

    try {
      await onDeleteItem(item);
      commitItems(editableItems.filter(({ id }) => id !== item.id));
    } catch (error) {
      console.error('Delete cleaning checklist item error:', error);
      Alert.alert(
        'Unable to delete item',
        'The checklist item could not be deleted.',
      );
    } finally {
      setDeletingItemId(null);
    }
  }

  function updateItemDescription(id: number, description: string) {
    commitItems(
      editableItems.map((item) =>
        item.id === id ? { ...item, description } : item,
      ),
    );
  }

  return (
    <View className='flex-col justify-center gap-2'>
      <Heading size='md'>Checklist</Heading>

      <View className='py-2 gap-3'>
        {editableItems.map((item) => (
          <ChecklistItemInput
            key={item.id}
            item={item}
            onDelete={() => deleteItem(item)}
            isDeleting={isPersisting}
            isReadOnly={isReadOnly}
            onChangeDescription={(description) =>
              updateItemDescription(item.id, description)
            }
          />
        ))}

        {!isReadOnly && (
          <Input className='w-full rounded-full'>
            <InputField
              value={newItemDescription}
              onChangeText={setNewItemDescription}
              onSubmitEditing={addItem}
              placeholder='Add checklist item'
              returnKeyType='done'
              editable={!isPersisting}
            />

            <InputSlot>
              <Pressable
                onPress={addItem}
                disabled={isPersisting}
                className='ml-3'
              >
                <InputIcon as={Plus} style={styles.buttonText} />
              </Pressable>
            </InputSlot>
          </Input>
        )}
      </View>
    </View>
  );
}
