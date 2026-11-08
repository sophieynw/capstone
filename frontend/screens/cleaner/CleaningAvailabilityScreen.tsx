import { Alert, Pressable, Platform, ScrollView, View, Text } from 'react-native';
import { Card } from '@/components/ui/card';
import { styles } from '@/styles/styles';
import { Heading } from '@/components/ui/heading';
import { AvailabilitySlot, DaysOfTheWeek } from '@/types/entityTypes';
import {
  dateToTimeString,
  formatTime,
  sortAvailabilitySlots,
  timeStringToDate,
  toTitleCase,
} from '@/utils/helpers';
import { useContext, useState } from 'react';
import {
  CloseIcon,
  Icon,
  TrashIcon,
  EditIcon,
  ChevronDownIcon,
} from '@/components/ui/icon';
import {
  DateTimePicker,
  DateTimePickerIcon,
  DateTimePickerInput,
  DateTimePickerTrigger,
} from '@/components/ui/date-time-picker';
import {
  Modal,
  ModalBackdrop,
  ModalBody,
  ModalCloseButton,
  ModalContent,
  ModalFooter,
  ModalHeader,
} from '@/components/ui/modal';
import { Button, ButtonText } from '@/components/ui/button';
import { Clock } from 'lucide-react-native';
import {
  Select,
  SelectTrigger,
  SelectBackdrop,
  SelectIcon,
  SelectInput,
  SelectPortal,
  SelectContent,
  SelectDragIndicatorWrapper,
  SelectDragIndicator,
  SelectItem,
} from '@/components/ui/select';
import {
  useAvailabilities,
  useUpdateAvailability,
  useCreateAvailability,
  useDeleteAvailability,
} from '@/hooks/useAvailabilities';
import { AuthContext } from '@/auth/AuthContext';

function minutesSinceMidnight(date: Date): number {
  return date.getHours() * 60 + date.getMinutes();
}

function hasOverlappingAvailability(
  slots: AvailabilitySlot[] | undefined,
  dayOfWeek: DaysOfTheWeek,
  startTime: Date,
  endTime: Date,
  excludedSlotId?: number,
): boolean {
  const candidateStart = minutesSinceMidnight(startTime);
  const candidateEnd = minutesSinceMidnight(endTime);

  return (slots ?? []).some((slot) => {
    if (slot.id === excludedSlotId || slot.dayOfWeek !== dayOfWeek) {
      return false;
    }

    const existingStart = minutesSinceMidnight(
      timeStringToDate(slot.startTime),
    );
    const existingEnd = minutesSinceMidnight(timeStringToDate(slot.endTime));

    return candidateStart < existingEnd && candidateEnd > existingStart;
  });
}

function TimeField({
  label,
  value,
  onChange,
}: {
  label: string;
  value: Date;
  onChange: (date: Date) => void;
}) {
  return (
    <>
      <Text className='m-2'>{label}</Text>
      <DateTimePicker
        value={value}
        onChange={(date) => {
          if (date) onChange(date);
        }}
        mode='time'
        display={Platform.OS === 'ios' ? 'spinner' : 'default'}
        format='hh:mm A'
        is24Hour={false}
      >
        <DateTimePickerTrigger className='m-2 rounded-full'>
          <DateTimePickerInput />
          <DateTimePickerIcon as={Clock} className='mr-3' />
        </DateTimePickerTrigger>
      </DateTimePicker>
    </>
  );
}

export default function CleaningAvailabilityScreen() {
  const { user } = useContext(AuthContext);
  const { data: availabilitySlots } = useAvailabilities(user?.id);
  const [showModal, setShowModal] = useState(false);

  const [selectedDay, setSelectedDay] = useState<DaysOfTheWeek>(
    DaysOfTheWeek.MONDAY,
  );

  const [startTime, setStartTime] = useState<Date>(new Date());
  const [endTime, setEndTime] = useState<Date>(new Date());
  const [editingSlot, setEditingSlot] = useState<AvailabilitySlot | null>(null);

  const updateAvailability = useUpdateAvailability(user?.id);
  const createAvailability = useCreateAvailability(user?.id);
  const deleteAvailability = useDeleteAvailability(user?.id);
  const sortedAvailabilitySlots = sortAvailabilitySlots(availabilitySlots);
  // TODO: implement functionality for getting and updating cleaner availability

  function openEditor(slot: AvailabilitySlot) {
    setEditingSlot(slot);
    setStartTime(timeStringToDate(slot.startTime));
    setEndTime(timeStringToDate(slot.endTime));
    setShowModal(true);
  }

  function openCreator() {
    setEditingSlot(null);
    setSelectedDay(DaysOfTheWeek.MONDAY);
    setStartTime(new Date(new Date().setHours(9, 0, 0, 0)));
    setEndTime(new Date(new Date().setHours(17, 0, 0, 0)));
    setShowModal(true);
  }

  function handleDelete(slot: AvailabilitySlot) {
    Alert.alert('Delete availability', 'Remove this time slot?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: () => deleteAvailability.mutate(slot.id),
      },
    ]);
  }

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.screenContent}
    >
      <View style={styles.vStack}>
        {sortedAvailabilitySlots.map((slot) => (
          <Card
            key={slot.id}
            style={styles.mediumCardWithAvatar}
            // className='rounded-3xl flex-row items-center justify-between'
          >
            <View className='gap-1'>
              <Heading size='md'>
                {toTitleCase(slot?.dayOfWeek.toString())}
              </Heading>
              <Text>
                {formatTime(slot.startTime)} to {formatTime(slot.endTime)}
              </Text>
            </View>

            <View className='flex-row items-center gap-4'>
              <Pressable
                onPress={() => openEditor(slot)}
                hitSlop={12}
                accessibilityRole='button'
                accessibilityLabel='Edit availability slot'
              >
                <Icon as={EditIcon} size='xl' style={styles.buttonText} />
              </Pressable>

              <Pressable
                onPress={() => handleDelete(slot)}
                hitSlop={12}
                accessibilityRole='button'
                accessibilityLabel='Delete availability slot'
              >
                <Icon as={TrashIcon} size='xl' style={styles.buttonTextWarn} />
              </Pressable>
            </View>
          </Card>
        ))}
      </View>

      <Button
        className='rounded-full'
        style={styles.button}
        size='lg'
        onPress={openCreator}
      >
        <ButtonText style={styles.buttonText}>Add New Availability</ButtonText>
      </Button>

      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        closeOnOverlayClick={false}
        size='md'
      >
        <ModalBackdrop />
        <ModalContent className='rounded-4xl'>
          <ModalHeader>
            <Heading size='lg'>
              {editingSlot ? 'Edit Time' : 'New Availability'}
            </Heading>

            <ModalCloseButton>
              <Icon as={CloseIcon} />
            </ModalCloseButton>
          </ModalHeader>

          <ModalBody>
            {!editingSlot && (
              <>
                <Text className='m-2'>Day of Week</Text>
                <Select
                  selectedValue={selectedDay ?? undefined}
                  onValueChange={(value) =>
                    setSelectedDay(value as DaysOfTheWeek)
                  }
                >
                  <SelectTrigger
                    variant='outline'
                    size='md'
                    className='m-2 rounded-full'
                  >
                    <SelectInput
                      placeholder='Select Day'
                      value={selectedDay ? toTitleCase(selectedDay) : ''}
                    />
                    <SelectIcon className='mr-3' as={ChevronDownIcon} />
                  </SelectTrigger>
                  <SelectPortal>
                    <SelectBackdrop />
                    <SelectContent>
                      <SelectDragIndicatorWrapper>
                        <SelectDragIndicator />
                      </SelectDragIndicatorWrapper>
                      {Object.values(DaysOfTheWeek).map((day) => (
                        <SelectItem
                          key={day}
                          label={toTitleCase(day)}
                          value={day}
                        />
                      ))}
                    </SelectContent>
                  </SelectPortal>
                </Select>
              </>
            )}

            <TimeField
              label='Start Time'
              value={startTime}
              onChange={setStartTime}
            />
            <TimeField label='End Time' value={endTime} onChange={setEndTime} />
          </ModalBody>

          <ModalFooter>
            <Button
              className='rounded-full'
              style={styles.button}
              onPress={() => {
                if (!availabilitySlots) {
                  Alert.alert(
                    'Availability unavailable',
                    'Wait for your availability to load before saving.',
                  );
                  return;
                }

                if (endTime.getTime() <= startTime.getTime()) {
                  Alert.alert(
                    'Invalid time',
                    'End time must be later than start time.',
                  );
                  return;
                }

                const dayOfWeek = editingSlot?.dayOfWeek ?? selectedDay;
                if (
                  hasOverlappingAvailability(
                    availabilitySlots,
                    dayOfWeek,
                    startTime,
                    endTime,
                    editingSlot?.id,
                  )
                ) {
                  Alert.alert(
                    'Overlapping availability',
                    `This time overlaps with an existing availability period on ${toTitleCase(dayOfWeek)}.`,
                  );
                  return;
                }

                if (editingSlot) {
                  updateAvailability.mutate(
                    {
                      id: editingSlot.id,
                      startTime: dateToTimeString(startTime),
                      endTime: dateToTimeString(endTime),
                    },
                    { onSuccess: () => setShowModal(false) },
                  );
                } else {
                  createAvailability.mutate(
                    {
                      dayOfWeek: selectedDay,
                      startTime: dateToTimeString(startTime),
                      endTime: dateToTimeString(endTime),
                    },
                    { onSuccess: () => setShowModal(false) },
                  );
                }
              }}
            >
              <ButtonText style={styles.buttonText}>Done</ButtonText>
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </ScrollView>
  );
}
