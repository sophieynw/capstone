import { Heading } from '@/components/ui/heading';
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
  setCleanerSelected,
  startDateSelected,
  setStartDateSelected,
  endDateSelected,
  setEndDateSelected,
}: DetailsSectionProps) {
  return (
    <>
      <Heading size='md'>Details</Heading>
      <View className='flex-row gap-4 justify-center'>
        {/* Properties */}
        <Select
          selectedValue={propertySelected}
          onValueChange={setPropertySelected}
        >
          <SelectTrigger size='md' variant='rounded'>
            <SelectInput placeholder='Property' />
            <SelectIcon className='mr-3' as={ChevronDown} />
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
        {/* Cleaner */}
        <Select
          selectedValue={cleanerSelected}
          onValueChange={(value) =>
            setCleanerSelected(value === 'unassigned' ? '' : value)
          }
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
              <SelectItem label='Unassigned' value='unassigned' />
              {cleaners?.map((cleanerItem) => (
                <SelectItem
                  key={cleanerItem.id}
                  label={`${cleanerItem.firstName} ${cleanerItem.lastName.charAt(0)}.`}
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
