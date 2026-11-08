import { Alert, ScrollView, View } from 'react-native';
import { useContext, useState } from 'react';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import { Heading } from '@/components/ui/heading';
import { styles } from '@/styles/styles';
import { Save } from 'lucide-react-native';
import { ChecklistEditingSection } from '@/components/ChecklistEditingSection';
import {
  createEditableProperty,
  EditableProperty,
  PropertyEditingSection,
} from '@/components/PropertyEditingSection';
import { useCreateProperty } from '@/hooks/useProperties';
import { AuthContext } from '@/auth/AuthContext';
import {
  ChecklistItem,
  CreatePropertyPayload,
  Role,
} from '@/types/entityTypes';

export default function NewPropertyScreen() {
  const { user } = useContext(AuthContext);
  const createPropertyMutation = useCreateProperty();

  const [newPropertyForm, setNewPropertyForm] = useState<EditableProperty>(() =>
    createEditableProperty(),
  );
  const [checklistItems, setChecklistItems] = useState<ChecklistItem[]>([]);

  function handleSubmit() {
    if (!user) {
      Alert.alert('Error', 'You must be signed in.');
      return;
    }

    if (user.role !== Role.MANAGER) {
      Alert.alert('Error', 'You must be signed in.');
      return;
    }

    const name = newPropertyForm.name;
    const street = newPropertyForm.street;
    const city = newPropertyForm.city;
    const province = newPropertyForm.province;
    const postalCode = newPropertyForm.postalCode;
    const country = newPropertyForm.country;
    const checkoutTime = newPropertyForm.checkoutTime;
    const checkinTime = newPropertyForm.checkinTime;

    if (!name || !street || !city || !province || !postalCode || !country) {
      Alert.alert(
        'Missing information',
        'Please complete all required property fields.',
      );
      return;
    }

    if (checkinTime <= checkoutTime) {
      Alert.alert(
        'Invalid times',
        'Check-in time must be later than checkout time.',
      );
      return;
    }

    const payload: CreatePropertyPayload = {
      property: {
        manager: {
          id: user.id,
          role: Role.MANAGER,
        },
        name,
        street,
        unit: newPropertyForm.unit.trim(),
        city,
        province,
        postalCode,
        country,
        checkoutTime,
        checkinTime,
        accessInstructions: newPropertyForm.accessInstructions.trim() || null,
      },

      checklistItems: checklistItems
        .map((item) => ({
          description: item.description.trim(),
          frequencyDays: item.frequencyDays,
        }))
        .filter((item) => item.description.length > 0),
    };

    createPropertyMutation.mutate(payload);
  }

  return (
    <ScrollView
      style={styles.modalScreen}
      contentContainerStyle={styles.modalScreenContent}
    >
      {/* Heading */}
      <View style={styles.modalHeader}>
        <Heading size='2xl'>Add Property</Heading>
        <Button
          className='rounded-full'
          style={[styles.button, styles.modalHeaderButton]}
          size='lg'
          onPress={handleSubmit}
          isDisabled={createPropertyMutation.isPending}
        >
          <ButtonIcon style={styles.buttonText} as={Save} />
          <ButtonText style={styles.buttonText}>
            {createPropertyMutation.isPending ? 'Saving...' : 'Save'}
          </ButtonText>
        </Button>
      </View>

      {/* Content */}
      <View style={styles.modalMain}>
        {/* Property Details */}
        <PropertyEditingSection
          property={newPropertyForm}
          onPropertyChange={setNewPropertyForm}
        />

        {/* Checklist Items */}
        <ChecklistEditingSection
          items={checklistItems}
          onItemsChange={setChecklistItems}
        />
      </View>
    </ScrollView>
  );
}
