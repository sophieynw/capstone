// screens/manager/NewCleaningScreen.tsx
import { Alert, ScrollView, View } from 'react-native';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import { Heading } from '@/components/ui/heading';
import { useContext, useMemo, useState, useEffect } from 'react';
import { Text } from '@/components/ui/text';
import { Card } from '@/components/ui/card';
import { styles } from '@/styles/styles';
import { SaveIcon } from 'lucide-react-native';
import { usePropertyAll } from '@/hooks/useProperties';
import { ChecklistItem, Role } from '@/types/entityTypes';
import { useCleaners } from '@/hooks/useUsers';
import { ChecklistSection } from '@/components/ChecklistSection';
import { AuthContext } from '@/auth/AuthContext';
import { useCreateCleaning } from '@/hooks/useCleanings';
import { NotesSection } from '@/components/NotesSection';
import { DetailsSection } from '@/components/DetailsSection';
import { useChecklistItems } from '@/hooks/useChecklistItems';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';
import { formatLocalDateTime, getAvailableCleaners } from '@/utils/helpers';

function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function filterDueChecklistItems(items: ChecklistItem[]): ChecklistItem[] {
  const today = startOfDay(new Date());

  return items.filter((item) => {
    if (!item.lastCompleted) {
      return true;
    }

    const lastCompleted = startOfDay(new Date(item.lastCompleted));
    const nextDueDate = new Date(lastCompleted);

    nextDueDate.setDate(nextDueDate.getDate() + (item.frequencyDays ?? 0));

    return nextDueDate <= today;
  });
}

export default function NewCleaningScreen() {
  const { user } = useContext(AuthContext);
  const createCleaningMutation = useCreateCleaning();

  const [propertySelected, setPropertySelected] = useState('');
  const [cleanerSelected, setCleanerSelected] = useState('');
  const [startDateSelected, setStartDateSelected] = useState(new Date());
  const [endDateSelected, setEndDateSelected] = useState(new Date());
  const [notes, setNotes] = useState('');
  const [checklistItems, setChecklistItems] = useState<ChecklistItem[]>([]);

  const {
    data: properties,
    isLoading: isPropertiesLoading,
    isError: isPropertiesError,
    error: propertiesError,
  } = usePropertyAll();
  const {
    data: cleaners,
    isLoading: isCleanersLoading,
    isError: isCleanersError,
    error: cleanersError,
    refetch: refetchCleaners,
  } = useCleaners();
  const propertyId = propertySelected ? Number(propertySelected) : undefined;
  const {
    data: propertyChecklistItems,
    isLoading: isChecklistItemsLoading,
    isError: isChecklistItemsError,
    error: checklistItemsError,
  } = useChecklistItems(propertyId);

  const dueChecklistItems = useMemo(
    () => filterDueChecklistItems(propertyChecklistItems ?? []),
    [propertyChecklistItems],
  );

  useEffect(() => {
    setChecklistItems(dueChecklistItems);
  }, [dueChecklistItems]);

  const dateTimeStart = formatLocalDateTime(startDateSelected);
  const dateTimeEnd = formatLocalDateTime(endDateSelected);
  const availableCleaners = getAvailableCleaners(
    cleaners,
    dateTimeStart,
    dateTimeEnd,
  );
  const selectedCleaner = availableCleaners.find(
    (cleaner) => cleaner.id.toString() === cleanerSelected,
  );

  function handleSubmit() {
    if (!user) {
      Alert.alert('Error', 'You must be signed in.');
      return;
    }

    if (!propertySelected) {
      Alert.alert('Missing property', 'Please select a property.');
      return;
    }

    if (dateTimeEnd <= dateTimeStart) {
      Alert.alert('Invalid time', 'The end time must be after the start time.');
      return;
    }

    if (cleanerSelected && !selectedCleaner) {
      Alert.alert(
        'Cleaner unavailable',
        'Select an available cleaner or choose Unassigned.',
      );
      return;
    }

    const cleaningChecklistItems = checklistItems
      .filter((item) => item.description.trim())
      .map((item) =>
        item.id > 0
          ? { checklistItem: { id: item.id } }
          : { customDescription: item.description.trim() },
      );

    createCleaningMutation.mutate(
      {
        manager: { id: user.id, role: Role.MANAGER },
        cleaner: selectedCleaner
          ? { id: selectedCleaner.id, role: Role.CLEANER }
          : null,
        property: { id: Number(propertySelected) },
        dateTimeStart,
        dateTimeEnd,
        notes: notes.trim() || null,
        cleaningChecklistItems,
      },
      {
        onError: () => {
          if (user.organization) void refetchCleaners();
        },
      },
    );
  }

  if (isPropertiesLoading || isCleanersLoading) {
    return <LoadingSpinner label='Loading form options...' />;
  }

  if (isPropertiesError || isCleanersError) {
    console.error('Properties error:', propertiesError);
    console.error('Cleaners error:', cleanersError);
    return <ErrorState />;
  }

  if (isChecklistItemsError) {
    console.error('Checklist error:', checklistItemsError);
  }

  return (
    <ScrollView
      style={styles.modalScreen}
      contentContainerStyle={styles.modalScreenContent}
    >
      {/* Header */}
      <View style={styles.modalHeader}>
        <Heading size='2xl'>New Cleaning</Heading>
        <Button
          className='rounded-full'
          style={[styles.button, styles.modalHeaderButton]}
          size='lg'
          onPress={handleSubmit}
          isDisabled={createCleaningMutation.isPending}
        >
          <ButtonIcon style={styles.buttonText} as={SaveIcon} />
          <ButtonText style={styles.buttonText}>
            {createCleaningMutation.isPending ? 'Saving...' : 'Save'}
          </ButtonText>
        </Button>
      </View>

      {/* Content */}
      <View style={styles.modalMain}>
        <Card style={styles.bigCard}>
          <DetailsSection
            properties={properties}
            propertySelected={propertySelected}
            setPropertySelected={setPropertySelected}
            cleaners={availableCleaners}
            cleanerSelected={cleanerSelected}
            setCleanerSelected={setCleanerSelected}
            startDateSelected={startDateSelected}
            setStartDateSelected={setStartDateSelected}
            endDateSelected={endDateSelected}
            setEndDateSelected={setEndDateSelected}
          />
          {cleanerSelected && !selectedCleaner && (
            <Text>The selected cleaner is unavailable for this time.</Text>
          )}
          {!propertyId ? (
            <View className='gap-2'>
              <Heading size='md'>Checklist</Heading>
              <Text>Select a property to load its checklist.</Text>
            </View>
          ) : isChecklistItemsLoading ? (
            <LoadingSpinner inline label='Loading checklist...' />
          ) : isChecklistItemsError ? (
            <ErrorState inline />
          ) : (
            <ChecklistSection
              items={checklistItems}
              onItemsChange={setChecklistItems}
            />
          )}

          <NotesSection notes={notes} setNotes={setNotes} />
        </Card>
      </View>
    </ScrollView>
  );
}
