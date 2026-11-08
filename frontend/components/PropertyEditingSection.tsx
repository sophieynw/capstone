import { Text, View } from 'react-native';
import { Heading } from '@/components/ui/heading';
import { Input, InputField } from '@/components/ui/input';
import { Card } from '@/components/ui/card';
import { styles } from '@/styles/styles';
import { Property } from '@/types/entityTypes';
import {
  DateTimePicker,
  DateTimePickerIcon,
  DateTimePickerInput,
  DateTimePickerTrigger,
} from '@/components/ui/date-time-picker';
import { Clock } from 'lucide-react-native';
import { dateToTimeString, timeStringToDate } from '@/utils/helpers';

export type EditableProperty = {
  name: string;
  street: string;
  unit: string;
  city: string;
  province: string;
  postalCode: string;
  country: string;
  checkoutTime: string;
  checkinTime: string;
  accessInstructions: string;
};

export function createEditableProperty(property?: Property): EditableProperty {
  return {
    name: property?.name ?? '',
    street: property?.street ?? '',
    unit: property?.unit ?? '',
    city: property?.city ?? '',
    province: property?.province ?? '',
    postalCode: property?.postalCode ?? '',
    country: property?.country ?? '',
    checkoutTime: property?.checkoutTime ?? '11:00',
    checkinTime: property?.checkinTime ?? '16:00',
    accessInstructions: property?.accessInstructions ?? '',
  };
}

type PropertyTimeInputProps = {
  label: string;
  value: string;
  onChange: (value: string) => void;
};

function PropertyTimeInput({ label, value, onChange }: PropertyTimeInputProps) {
  return (
    <View className='gap-1'>
      <Text>{label}</Text>
      <DateTimePicker
        value={timeStringToDate(value)}
        onChange={(date) => {
          if (date) onChange(dateToTimeString(date));
        }}
        mode='time'
        format='HH:mm'
      >
        <DateTimePickerTrigger className='rounded-full'>
          <DateTimePickerInput />
          <DateTimePickerIcon as={Clock} className='mr-3' />
        </DateTimePickerTrigger>
      </DateTimePicker>
    </View>
  );
}

type PropertyInputProps = {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  multiline?: boolean;
};

function PropertyInput({
  label,
  value,
  onChangeText,
  multiline = false,
}: PropertyInputProps) {
  return (
    <View className='gap-1'>
      <Text>{label}</Text>
      <Input
        className={multiline ? 'h-24 items-start rounded-2xl' : 'rounded-full'}
      >
        <InputField
          value={value}
          onChangeText={onChangeText}
          placeholder={label}
          multiline={multiline}
          textAlignVertical={multiline ? 'top' : 'center'}
        />
      </Input>
    </View>
  );
}

type PropertyEditingSectionProps = {
  property: EditableProperty;
  onPropertyChange: (property: EditableProperty) => void;
};

export function PropertyEditingSection({
  property,
  onPropertyChange,
}: PropertyEditingSectionProps) {
  function updatePropertyField(field: keyof EditableProperty, value: string) {
    onPropertyChange({
      ...property,
      [field]: value,
    });
  }

  return (
    <Card style={styles.mediumCard}>
      <View className='gap-3'>
        <Heading size='md'>Property</Heading>
        <PropertyInput
          label='Name'
          value={property.name}
          onChangeText={(value) => updatePropertyField('name', value)}
        />
        <PropertyInput
          label='Street number and name'
          value={property.street}
          onChangeText={(value) => updatePropertyField('street', value)}
        />
        <PropertyInput
          label='Unit'
          value={property.unit}
          onChangeText={(value) => updatePropertyField('unit', value)}
        />
        <PropertyInput
          label='City'
          value={property.city}
          onChangeText={(value) => updatePropertyField('city', value)}
        />
        <PropertyInput
          label='Province'
          value={property.province}
          onChangeText={(value) => updatePropertyField('province', value)}
        />
        <PropertyInput
          label='Postal code'
          value={property.postalCode}
          onChangeText={(value) => updatePropertyField('postalCode', value)}
        />
        <PropertyInput
          label='Country'
          value={property.country}
          onChangeText={(value) => updatePropertyField('country', value)}
        />
        <PropertyTimeInput
          label='Checkout time'
          value={property.checkoutTime}
          onChange={(value) => updatePropertyField('checkoutTime', value)}
        />
        <PropertyTimeInput
          label='Check-in time'
          value={property.checkinTime}
          onChange={(value) => updatePropertyField('checkinTime', value)}
        />
        <PropertyInput
          label='Access instructions'
          value={property.accessInstructions}
          onChangeText={(value) =>
            updatePropertyField('accessInstructions', value)
          }
          multiline
        />
      </View>
    </Card>
  );
}
