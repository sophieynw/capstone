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
    <View className='flex-col items-start gap-2'>
      <View className='w-full flex-row items-center justify-start gap-2'>
        <Text className='w-24'>Starts:</Text>
        <DateTimePicker
          className={disabled ? 'opacity-50' : undefined}
          value={startDate}
          onChange={(date) => date && onStartDateChange(date)}
          mode='datetime'
          placeholder='Select date and time'
          format='YYYY-MM-DD hh:mm A'
          is24Hour={false}
          disabled={disabled}
        >
          <DateTimePickerTrigger variant='rounded'>
            <DateTimePickerInput />
            <DateTimePickerIcon as={Calendar} className='mr-3' />
          </DateTimePickerTrigger>
        </DateTimePicker>
      </View>

      <View className='w-full flex-row items-center justify-start gap-2'>
        <Text className='w-24'>Ends:</Text>
        <DateTimePicker
          className={disabled ? 'opacity-50' : undefined}
          value={endDate}
          onChange={(date) => date && onEndDateChange(date)}
          mode='datetime'
          placeholder='Select date and time'
          format='YYYY-MM-DD hh:mm A'
          is24Hour={false}
          disabled={disabled}
        >
          <DateTimePickerTrigger variant='rounded'>
            <DateTimePickerInput />
            <DateTimePickerIcon as={Calendar} className='mr-3' />
          </DateTimePickerTrigger>
        </DateTimePicker>
      </View>
    </View>
  );
}
