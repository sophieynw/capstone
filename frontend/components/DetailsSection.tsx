import { Heading } from '@/components/ui/heading';
import { Text } from '@/components/ui/text';
import { View } from 'react-native';
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
import { ChevronDown } from 'lucide-react-native';
import { Property, User } from '@/types/entityTypes';
import { CleaningDateTimeFields } from '@/components/CleaningDateTimeFields';

type DetailsSectionProps = {
  properties: Property[] | undefined;
  propertySelected: string;
  setPropertySelected: (property: string) => void;
  cleaners: User[] | undefined;
  cleanerSelected: string;
  selectedCleanerLabel: string;
  setCleanerSelected: (cleaning: string) => void;
  startDateSelected: Date;
  setStartDateSelected: (startDate: Date) => void;
  endDateSelected: Date;
  setEndDateSelected: (endDate: Date) => void;
};

export function DetailsSection({
  properties,
  propertySelected,
  setPropertySelected,
  cleaners,
  cleanerSelected,
  selectedCleanerLabel,
  setCleanerSelected,
  startDateSelected,
  setStartDateSelected,
  endDateSelected,
  setEndDateSelected,
}: DetailsSectionProps) {
  return (
    <>
      <Heading size='md'>Details</Heading>
      <Select
        selectedValue={propertySelected}
        onValueChange={setPropertySelected}
      >
        <SelectTrigger size='md' variant='rounded'>
          <SelectInput className='min-w-0 flex-1' placeholder='Property' />
          <SelectIcon className='mr-3 shrink-0' as={ChevronDown} />
        </SelectTrigger>
        <SelectPortal useRNModal>
          <SelectBackdrop />
          <SelectContent>
            <SelectDragIndicatorWrapper>
              <SelectDragIndicator />
            </SelectDragIndicatorWrapper>
            {properties?.map((propertyItem) => (
              <SelectItem
                key={propertyItem.id}
                label={propertyItem.name}
                value={propertyItem.id.toString()}
              />
            ))}
          </SelectContent>
        </SelectPortal>
      </Select>
      <View className='flex-row items-center gap-2'>
        <Text className='w-24 shrink-0'>Cleaner:</Text>
        <Select
          className='min-w-0 flex-1'
          selectedValue={cleanerSelected}
          onValueChange={(value) =>
            setCleanerSelected(value === 'unassigned' ? '' : value)
          }
        >
          <SelectTrigger
            size='md'
            variant='rounded'
            accessibilityLabel={`Cleaner: ${selectedCleanerLabel}`}
          >
            <Text
              className='min-w-0 flex-1 px-4 text-foreground/90'
              numberOfLines={1}
              ellipsizeMode='tail'
            >
              {selectedCleanerLabel}
            </Text>
            <SelectIcon className='mr-3 shrink-0' as={ChevronDown} />
          </SelectTrigger>
          <SelectPortal useRNModal>
            <SelectBackdrop />
            <SelectContent>
              <SelectDragIndicatorWrapper>
                <SelectDragIndicator />
              </SelectDragIndicatorWrapper>
              <SelectItem label='Unassigned' value='unassigned' />
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
      </View>
      <CleaningDateTimeFields
        startDate={startDateSelected}
        endDate={endDateSelected}
        onStartDateChange={setStartDateSelected}
        onEndDateChange={setEndDateSelected}
      />
    </>
  );
}
