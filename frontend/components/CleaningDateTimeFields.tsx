import { View } from 'react-native';
import { Calendar } from 'lucide-react-native';
import { Text } from '@/components/ui/text';
import {
  DateTimePicker,
  DateTimePickerIcon,
  DateTimePickerInput,
  DateTimePickerTrigger,
} from '@/components/ui/date-time-picker';

type CleaningDateTimeFieldsProps = {
  startDate: Date;
  endDate: Date;
  onStartDateChange: (date: Date) => void;
  onEndDateChange: (date: Date) => void;
  disabled?: boolean;
};

export function CleaningDateTimeFields({
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
  disabled = false,
}: CleaningDateTimeFieldsProps) {
  return (
    <View className='gap-2'>
      <View className='flex-row items-center gap-2'>
        <Text className='w-24 shrink-0'>Starts:</Text>
        <DateTimePicker
          className={`min-w-0 flex-1 ${disabled ? 'opacity-50' : ''}`}
          value={startDate}
          onChange={(date) => date && onStartDateChange(date)}
          mode='datetime'
          placeholder='Select date and time'
          is24Hour={false}
          disabled={disabled}
        >
          <DateTimePickerTrigger variant='rounded'>
            <DateTimePickerInput className='min-w-0 flex-1' />
            <DateTimePickerIcon as={Calendar} className='mr-3 shrink-0' />
          </DateTimePickerTrigger>
        </DateTimePicker>
      </View>

      <View className='flex-row items-center gap-2'>
        <Text className='w-24 shrink-0'>Ends:</Text>
        <DateTimePicker
          className={`min-w-0 flex-1 ${disabled ? 'opacity-50' : ''}`}
          value={endDate}
          onChange={(date) => date && onEndDateChange(date)}
          mode='datetime'
          placeholder='Select date and time'
          is24Hour={false}
          disabled={disabled}
        >
          <DateTimePickerTrigger variant='rounded'>
            <DateTimePickerInput className='min-w-0 flex-1' />
            <DateTimePickerIcon as={Calendar} className='mr-3 shrink-0' />
          </DateTimePickerTrigger>
        </DateTimePicker>
      </View>
    </View>
  );
}
