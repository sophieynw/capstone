import { Alert, ScrollView, Text, View } from 'react-native';
import { Card } from '@/components/ui/card';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import {
  Cleaning,
  CleaningChecklistItem,
  IssueType,
  Role,
} from '@/types/entityTypes';
import { formatLocalDateTime, toFriendlyDate } from '@/utils/helpers';
import { useContext, useEffect, useRef, useState } from 'react';
import { useCleaners } from '@/hooks/useUsers';
import { styles } from '@/styles/styles';
import { Heading } from '@/components/ui/heading';
import {
  ChevronDown,
  CircleAlert,
  CircleCheckBig,
  LoaderCircle,
  SquareCheckBig,
  Trash,
} from 'lucide-react-native';
import { AuthContext } from '@/auth/AuthContext';
import { NotesSection } from '@/components/NotesSection';
import { NoteCard } from '@/components/NoteCard';
import { ChecklistSection } from '@/components/ChecklistSection';
import { CleanerChecklistSection } from '@/components/CleanerChecklistSection';
import { PhotosSection } from '@/components/PhotosSection';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';
import { CleaningDateTimeFields } from '@/components/CleaningDateTimeFields';

import {
  Select,
  SelectBackdrop,
  SelectContent,
  SelectDragIndicator,
  SelectDragIndicatorWrapper,
  SelectIcon,
  SelectInput,
  SelectItem,
  SelectPortal,
  SelectTrigger,
} from '@/components/ui/select';

import {
  useAddCleaningChecklistItem,
  useCleaningById,
  useCleaningNotes,
  useCompleteCleaning,
  useCreateCleaningNote,
  useDeleteCleaning,
  useDeleteCleaningChecklistItem,
  useUpdateCleaningChecklistItems,
  useUpdateCleaningCleaner,
  useUpdateCleaningSchedule,
} from '@/hooks/useCleanings';

import {
  Modal,
  ModalBackdrop,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
} from '@/components/ui/modal';

import { CloseIcon, Icon } from '@/components/ui/icon';
import { Textarea, TextareaInput } from '@/components/ui/textarea';
import { useCreateIssue } from '@/hooks/useIssues';

const UNASSIGNED_CLEANER_VALUE = 'unassigned';

export default function CleaningDetailsScreen({ route, navigation }: any) {
  const { user } = useContext(AuthContext);
  const initialCleaning = route.params.cleaning as Cleaning;
  const {
    data: fetchedCleaning,
    isPending: isCleaningPending,
    isError: isCleaningError,
    error: cleaningError,
  } = useCleaningById(initialCleaning.id);
  const cleaning = fetchedCleaning ?? initialCleaning;
  const property = cleaning.property;

  const [notes, setNotes] = useState('');
  const cleaningNotes = useCleaningNotes(cleaning.id);
  const createNoteMutation = useCreateCleaningNote(cleaning.id);
  const noteSaveInFlight = useRef(false);

  const [cleanerSelected, setCleanerSelected] = useState(
    cleaning?.cleanerId?.toString() ?? UNASSIGNED_CLEANER_VALUE,
  );

  const [checklistItems, setChecklistItems] = useState<CleaningChecklistItem[]>(
    cleaning?.cleaningChecklistItems ?? [],
  );
  const [startDateSelected, setStartDateSelected] = useState(
    new Date(cleaning.dateTimeStart),
  );
  const [endDateSelected, setEndDateSelected] = useState(
    new Date(cleaning.dateTimeEnd),
  );

  const [showIssueModal, setShowIssueModal] = useState(false);
  const [issueType, setIssueType] = useState<IssueType | ''>('');
  const [issueDescription, setIssueDescription] = useState('');

  useEffect(() => {
    if (!fetchedCleaning) return;

    setChecklistItems(fetchedCleaning.cleaningChecklistItems);
    setCleanerSelected(
      fetchedCleaning.cleanerId?.toString() ?? UNASSIGNED_CLEANER_VALUE,
    );
    setStartDateSelected(new Date(fetchedCleaning.dateTimeStart));
    setEndDateSelected(new Date(fetchedCleaning.dateTimeEnd));
  }, [fetchedCleaning]);

  const {
    data: cleaners,
    isPending: isCleanersPending,
    isError: isCleanersError,
    error: cleanersError,
  } = useCleaners();

  const completeCleaningMutation = useCompleteCleaning();
  const deleteCleaningMutation = useDeleteCleaning();
  const addChecklistItemMutation = useAddCleaningChecklistItem();
  const deleteChecklistItemMutation = useDeleteCleaningChecklistItem();
  const updateChecklistItemsMutation = useUpdateCleaningChecklistItems();
  const checklistSaveInFlight = useRef(false);
  const pendingChecklistUpdate = updateChecklistItemsMutation.isPending
    ? updateChecklistItemsMutation.variables.updates[0]
    : undefined;
  const updateCleanerMutation = useUpdateCleaningCleaner();
  const updateScheduleMutation = useUpdateCleaningSchedule();
  const createIssueMutation = useCreateIssue();

  const selectedCleaner = cleaners?.find(
    (cleaner) => cleaner.id.toString() === cleanerSelected,
  );

  const selectedCleanerLabel =
    cleanerSelected === UNASSIGNED_CLEANER_VALUE
      ? 'Unassigned'
      : selectedCleaner
        ? `${selectedCleaner.firstName} ${selectedCleaner.lastName}`
        : undefined;

  if (isCleanersPending || isCleaningPending) {
    return <LoadingSpinner label='Loading cleaning details...' />;
  }

  if (isCleanersError || isCleaningError) {
    console.error('Cleaners error:', cleanersError);
    console.error('Cleaning error:', cleaningError);

    return <ErrorState />;
  }

  async function handleSaveNote() {
    const content = notes.trim();
    if (!content || noteSaveInFlight.current) return;

    noteSaveInFlight.current = true;
    try {
      await createNoteMutation.mutateAsync(content);
      setNotes('');
    } catch (error) {
      console.error('Create cleaning note error:', error);
      Alert.alert('Unable to save note', 'Please try again.');
    } finally {
      noteSaveInFlight.current = false;
    }
  }

  function handleCompleteCleaning() {
    Alert.alert(
      'Complete Cleaning',
      'Are you sure you want to mark this cleaning as complete?',
      [
        {
          text: 'Cancel',
          style: 'cancel',
        },
        {
          text: 'Yes',
          onPress: () => {
            if (checklistSaveInFlight.current) return;

            completeCleaningMutation.mutate(cleaning.id, {
              onSuccess: () => {
                navigation.goBack();
              },
              onError: () => {
                Alert.alert(
                  'Error marking cleaning as complete',
                  'Please try again later.',
                );
              },
            });
          },
        },
      ],
    );
  }

  function handleDeleteCleaning() {
    Alert.alert(
      'Delete Cleaning',
      'Are you sure you want to delete this cleaning?',
      [
        {
          text: 'Cancel',
          style: 'cancel',
        },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => {
            deleteCleaningMutation.mutate({
              cleaningId: cleaning.id,
              propertyId: cleaning.property.id,
            });
          },
        },
      ],
    );
  }

  function handleCleanerChange(value: string) {
    if (value === cleanerSelected || updateCleanerMutation.isPending) {
      return;
    }

    const previousValue = cleanerSelected;
    const cleanerId = value === UNASSIGNED_CLEANER_VALUE ? null : Number(value);

    setCleanerSelected(value);

    updateCleanerMutation.mutate(
      { cleaningId: cleaning.id, cleanerId },
      {
        onError: (error) => {
          console.error('Update cleaning cleaner error:', error);
          setCleanerSelected(previousValue);
          Alert.alert(
            'Unable to assign cleaner',
            'The cleaner assignment could not be updated.',
          );
        },
      },
    );
  }

  function handleAddChecklistItem(description: string) {
    return addChecklistItemMutation.mutateAsync({
      cleaningId: cleaning.id,
      propertyId: cleaning.property.id,
      description,
    });
  }

  function handleDeleteChecklistItem(item: CleaningChecklistItem) {
    return deleteChecklistItemMutation.mutateAsync({
      cleaningId: cleaning.id,
      propertyId: cleaning.property.id,
      itemId: item.id,
    });
  }

  async function handleToggleChecklistItem(item: CleaningChecklistItem) {
    if (
      cleaning.isComplete ||
      completeCleaningMutation.isPending ||
      checklistSaveInFlight.current
    ) {
      return;
    }

    checklistSaveInFlight.current = true;
    try {
      await updateChecklistItemsMutation.mutateAsync({
        cleaningId: cleaning.id,
        propertyId: cleaning.property.id,
        updates: [{ id: item.id, isComplete: !item.isComplete }],
      });
    } catch (error) {
      console.error('Update cleaning checklist error:', error);
      Alert.alert(
        'Unable to update checklist',
        'This item could not be saved. Please try again.',
      );
    } finally {
      checklistSaveInFlight.current = false;
    }
  }

  function handleScheduleChange(nextStart: Date, nextEnd: Date) {
    if (nextEnd <= nextStart) {
      Alert.alert('Invalid time', 'The end time must be after the start time.');
      return;
    }

    const previousStart = startDateSelected;
    const previousEnd = endDateSelected;

    setStartDateSelected(nextStart);
    setEndDateSelected(nextEnd);

    updateScheduleMutation.mutate(
      {
        cleaningId: cleaning.id,
        dateTimeStart: formatLocalDateTime(nextStart),
        dateTimeEnd: formatLocalDateTime(nextEnd),
      },
      {
        onError: (error) => {
          console.error('Update cleaning schedule error:', error);
          setStartDateSelected(previousStart);
          setEndDateSelected(previousEnd);
          Alert.alert(
            'Unable to update time',
            'The cleaning time could not be updated.',
          );
        },
      },
    );
  }

  return (
    <ScrollView
      style={styles.modalScreen}
      contentContainerStyle={styles.modalScreenContent}
    >
      {/* Header */}
      <View style={styles.modalHeader}>
        <Heading size='2xl'>Cleaning Details</Heading>

        {user?.role === Role.MANAGER && !cleaning.isComplete ? (
          <Button
            className='rounded-full'
            style={[styles.buttonWarn, styles.modalHeaderButton]}
            size='lg'
            onPress={handleDeleteCleaning}
            isDisabled={deleteCleaningMutation.isPending}
          >
            <ButtonIcon style={styles.buttonTextWarn} as={Trash} />

            <ButtonText style={styles.buttonTextWarn}>
              {deleteCleaningMutation.isPending ? 'Deleting...' : 'Delete'}
            </ButtonText>
          </Button>
        ) : user?.role !== Role.MANAGER ? (
          <Button
            className='rounded-full'
            style={[styles.button, styles.modalHeaderButton]}
            size='lg'
            onPress={handleCompleteCleaning}
            isDisabled={
              completeCleaningMutation.isPending ||
              updateChecklistItemsMutation.isPending ||
              cleaning.isComplete
            }
          >
            <ButtonIcon style={styles.buttonText} as={SquareCheckBig} />

            <ButtonText style={styles.buttonText}>
              {completeCleaningMutation.isPending ? 'Completing...' : 'Done'}
            </ButtonText>
          </Button>
        ) : null}
      </View>

      {/* Content */}
      <View style={styles.modalMain}>
        {/* Cleaning Info & Status Card */}
        <Card style={styles.mediumCard}>
          <View className='flex-row items-center justify-between'>
            <Heading size='md'>Cleaning Info</Heading>
            <Icon
              as={cleaning?.isComplete ? CircleCheckBig : LoaderCircle}
              size='lg'
              className={
                cleaning?.isComplete
                  ? 'text-success-shadow'
                  : 'text-neutral-shadow'
              }
            />
          </View>

          <View className='gap-2'>
            {cleaning.isComplete ? (
              <>
                <View className='flex-row items-center gap-2'>
                  <Text className='w-24'>Started:</Text>
                  <Text>
                    {toFriendlyDate(
                      cleaning.dateTimeStarted ?? cleaning.dateTimeStart,
                    )}
                  </Text>
                </View>
                <View className='flex-row items-center gap-2'>
                  <Text className='w-24'>Completed:</Text>
                  <Text>
                    {toFriendlyDate(
                      cleaning.dateTimeCompleted ?? cleaning.dateTimeEnd,
                    )}
                  </Text>
                </View>
              </>
            ) : user?.role === Role.MANAGER ? (
              <CleaningDateTimeFields
                startDate={startDateSelected}
                endDate={endDateSelected}
                onStartDateChange={(date) =>
                  handleScheduleChange(date, endDateSelected)
                }
                onEndDateChange={(date) =>
                  handleScheduleChange(startDateSelected, date)
                }
                disabled={updateScheduleMutation.isPending}
              />
            ) : (
              <>
                <Text>Starts: {toFriendlyDate(cleaning.dateTimeStart)}</Text>
                <Text>Ends: {toFriendlyDate(cleaning.dateTimeEnd)}</Text>
              </>
            )}

            {user?.role == Role.MANAGER && (
              <View className='flex-row items-center gap-2'>
                <Text className='w-24'>Cleaner:</Text>

                {cleaning.isComplete ? (
                  <Text>{selectedCleanerLabel ?? 'Unassigned'}</Text>
                ) : (
                  <Select
                    selectedValue={cleanerSelected}
                    initialLabel={selectedCleanerLabel}
                    onValueChange={handleCleanerChange}
                  >
                    <SelectTrigger size='md' variant='rounded'>
                      <SelectInput placeholder='Unassigned' />
                      <SelectIcon className='mr-3' as={ChevronDown} />
                    </SelectTrigger>

                    <SelectPortal useRNModal>
                      <SelectBackdrop />

                      <SelectContent>
                        <SelectDragIndicatorWrapper>
                          <SelectDragIndicator />
                        </SelectDragIndicatorWrapper>

                        <SelectItem
                          label='Unassigned'
                          value={UNASSIGNED_CLEANER_VALUE}
                        />

                        {cleaners?.map((cleanerItem) => (
                          <SelectItem
                            key={cleanerItem.id}
                            label={`${cleanerItem.firstName} ${cleanerItem.lastName}`}
                            value={cleanerItem.id.toString()}
                          />
                        ))}
                      </SelectContent>
                    </SelectPortal>
                  </Select>
                )}
              </View>
            )}
          </View>
        </Card>

        {/* Property Info Card */}
        <Card style={styles.mediumCard}>
          <Heading size='md'>Property Info</Heading>

          <View className='gap-1'>
            <Text>{property?.name}</Text>

            <Text>
              {property?.unit ? `${property?.unit}-` : ''}
              {property?.street}, {property?.city}
            </Text>

            <Text>{property?.accessInstructions}</Text>
          </View>
        </Card>

        {cleaning.notes?.trim() && (
          <Card style={styles.mediumCard}>
            <Heading size='md'>Cleaning Instructions</Heading>
            <Text>{cleaning.notes}</Text>
          </Card>
        )}

        {/* Checklist Card */}
        <Card style={styles.mediumCard}>
          {/* Manager View */}
          {user?.role == Role.MANAGER && (
            <ChecklistSection
              items={checklistItems}
              onItemsChange={setChecklistItems}
              onAddItem={handleAddChecklistItem}
              onDeleteItem={handleDeleteChecklistItem}
              isReadOnly={cleaning.isComplete}
            />
          )}

          {/* Cleaner View */}
          {user?.role == Role.CLEANER && (
            <CleanerChecklistSection
              items={cleaning.cleaningChecklistItems}
              onToggle={handleToggleChecklistItem}
              pendingUpdate={pendingChecklistUpdate}
              isReadOnly={
                cleaning.isComplete || completeCleaningMutation.isPending
              }
            />
          )}
        </Card>

        {cleaningNotes.isLoading && <Text>Loading notes...</Text>}
        {cleaningNotes.isError && (
          <Text>Unable to load notes. Reopen this screen to try again.</Text>
        )}
        {[...(cleaningNotes.data ?? [])]
          .sort(
            (a, b) =>
              Date.parse(a.createdAt) - Date.parse(b.createdAt) || a.id - b.id,
          )
          .map((note) => <NoteCard key={note.id} note={note} />)}

        <Card style={styles.mediumCard}>
          <NotesSection
            notes={notes}
            setNotes={setNotes}
            onSave={handleSaveNote}
            isSaving={createNoteMutation.isPending}
          />
        </Card>

        <Card style={styles.mediumCard}>
          <PhotosSection
            key={cleaning.id}
            cleaningId={cleaning.id}
            canUpload={user?.role === Role.CLEANER && user.id === cleaning.cleanerId}
          />
        </Card>
      </View>

      {/* Report Issue Button */}
      {user?.role == Role.CLEANER && (
        <Button
          size='lg'
          variant='outline'
          className='rounded-full'
          style={styles.buttonWarn}
          onPress={() => setShowIssueModal(true)}
        >
          <ButtonIcon
            className='text-red-500'
            style={styles.buttonTextWarn}
            as={CircleAlert}
          />

          <ButtonText className='text-red-500' style={styles.buttonTextWarn}>
            Report an Issue
          </ButtonText>
        </Button>
      )}

      {/* Report Issue Modal */}
      <Modal
        isOpen={showIssueModal}
        onClose={() => setShowIssueModal(false)}
        size='md'
        useRNModal
      >
        <ModalBackdrop />

        <ModalContent className='rounded-4xl'>
          <ModalHeader>
            <Heading size='lg'>Report an Issue</Heading>

            <ModalCloseButton>
              <Icon as={CloseIcon} />
            </ModalCloseButton>
          </ModalHeader>

          <ModalBody>
            <Text className='mb-2'>Issue Type</Text>

            <Select
              selectedValue={issueType}
              onValueChange={(value) => setIssueType(value as IssueType)}
            >
              <SelectTrigger size='md' variant='rounded' className='mb-4'>
                <SelectInput
                  className='flex-1'
                  placeholder='Select issue type'
                />
                <SelectIcon className='mr-3' as={ChevronDown} />
              </SelectTrigger>

              <SelectPortal useRNModal>
                <SelectBackdrop />

                <SelectContent>
                  <SelectDragIndicatorWrapper>
                    <SelectDragIndicator />
                  </SelectDragIndicatorWrapper>

                  <SelectItem label='Damage' value={IssueType.DAMAGE} />

                  <SelectItem
                    label='Missing Supplies'
                    value={IssueType.MISSING_SUPPLIES}
                  />

                  <SelectItem
                    label='Access Problem'
                    value={IssueType.ACCESS_PROBLEM}
                  />

                  <SelectItem label='Other' value={IssueType.OTHER} />
                </SelectContent>
              </SelectPortal>
            </Select>

            <Text className='mb-2'>Describe the issue</Text>

            <Textarea className='rounded-2xl'>
              <TextareaInput
                placeholder='Example: Broken lamp beside the bed...'
                value={issueDescription}
                onChangeText={setIssueDescription}
              />
            </Textarea>
          </ModalBody>

          <ModalFooter className='gap-2'>
            <Button
              variant='outline'
              className='rounded-full'
              style={styles.button}
              onPress={() => setShowIssueModal(false)}
            >
              <ButtonText style={styles.buttonText}>Cancel</ButtonText>
            </Button>

            <Button
              className='rounded-full'
              style={styles.button}
              isDisabled={createIssueMutation.isPending}
              onPress={() => {
                if (!issueType) {
                  Alert.alert(
                    'Missing issue type',
                    'Please select an issue type.',
                  );
                  return;
                }

                if (!issueDescription.trim()) {
                  Alert.alert(
                    'Missing description',
                    'Please describe the issue.',
                  );
                  return;
                }

                createIssueMutation.mutate(
                  {
                    cleaningId: cleaning.id,
                    issueType,
                    description: issueDescription.trim(),
                  },
                  {
                    onSuccess: () => {
                      setIssueType('');
                      setIssueDescription('');
                      setShowIssueModal(false);
                    },
                  },
                );
              }}
            >
              <ButtonText style={styles.buttonText}>Submit</ButtonText>
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </ScrollView>
  );
}
