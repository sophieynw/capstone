import { Alert, Pressable, ScrollView, Text, View } from 'react-native';
import { useEffect, useState } from 'react';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import { Heading } from '@/components/ui/heading';
import { styles } from '@/styles/styles';
import { Save, Trash } from 'lucide-react-native';
import {
  useDeleteProperty,
  useImportAirbnbCalendar,
  usePropertyById,
  useUpdateProperty,
  useUploadPropertyPicture,
} from '@/hooks/useProperties';
import { ReportedIssuesSection } from '@/components/ReportedIssuesSection';
import { useChecklistItems } from '@/hooks/useChecklistItems';
import { ChecklistEditingSection } from '@/components/ChecklistEditingSection';
import {
  createEditableProperty,
  EditableProperty,
  PropertyEditingSection,
} from '@/components/PropertyEditingSection';

import { Card } from '@/components/ui/card';
import {
  Avatar,
  AvatarFallbackText,
  AvatarImage,
} from '@/components/ui/avatar';
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
import { Input, InputField } from '@/components/ui/input';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';
import { useIssues } from '@/hooks/useIssues';
import { pickLibraryPhotos } from '@/utils/photoPicker';

export default function PropertyDetailsScreen({ route }: any) {
  const { propertyId } = route.params;
  const deletePropertyMutation = useDeleteProperty();
  const updatePropertyMutation = useUpdateProperty();
  const importAirbnbCalendarMutation = useImportAirbnbCalendar();
  const uploadPropertyPictureMutation = useUploadPropertyPicture();
  const isPropertyMutationPending =
    deletePropertyMutation.isPending ||
    updatePropertyMutation.isPending ||
    uploadPropertyPictureMutation.isPending;
  const [showAirbnbModal, setShowAirbnbModal] = useState(false);
  const [icalUrl, setIcalUrl] = useState('');
  const [isOpeningPicturePicker, setIsOpeningPicturePicker] = useState(false);
  const [failedPropertyPicture, setFailedPropertyPicture] = useState<
    string | null
  >(null);
  const [propertyForm, setPropertyForm] = useState<EditableProperty>(() =>
    createEditableProperty(),
  );
  const {
    data: property,
    isPending: isPropertyPending,
    isError: isPropertyError,
    error: propertyError,
  } = usePropertyById(propertyId);
  const propertyPicture = property?.propertyPicturePath;
  const {
    data: checklistItems,
    isPending: isChecklistItemsPending,
    isError: isChecklistItemsError,
    error: checklistItemsError,
  } = useChecklistItems(propertyId);
  const {
    data: issues,
    isPending: isIssuesPending,
    isError: isIssuesError,
    error: issuesError,
  } = useIssues(true, propertyId);

  useEffect(() => {
    if (!property) return;

    setPropertyForm(createEditableProperty(property));
  }, [property]);

  if (isPropertyPending || isChecklistItemsPending || isIssuesPending) {
    return <LoadingSpinner label='Loading property details...' />;
  }

  if (isPropertyError || isChecklistItemsError || isIssuesError) {
    console.error('Property error:', propertyError);
    console.error('ChecklistItems error:', checklistItemsError);
    console.error('Issues error:', issuesError);
    return <ErrorState />;
  }

  function handleDelete() {
    if (isPropertyMutationPending) return;

    Alert.alert(
      'Delete property?',
      `Are you sure you want to delete ${propertyForm.name}? This action cannot be undone.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => deletePropertyMutation.mutate(propertyId),
        },
      ],
    );
  }

  function handleUpdate() {
    if (isPropertyMutationPending) return;

    if (propertyForm.checkinTime <= propertyForm.checkoutTime) {
      Alert.alert(
        'Invalid times',
        'Check-in time must be later than checkout time.',
      );
      return;
    }

    updatePropertyMutation.mutate({
      propertyId,
      property: propertyForm,
    });
  }

  async function handlePickPropertyPicture() {
    if (isOpeningPicturePicker || uploadPropertyPictureMutation.isPending) {
      return;
    }

    setIsOpeningPicturePicker(true);

    try {
      const [image] = await pickLibraryPhotos();
      if (image) uploadPropertyPictureMutation.mutate({ propertyId, image });
    } catch {
      Alert.alert('Could not open photos', 'Please try again.');
    } finally {
      setIsOpeningPicturePicker(false);
    }
  }

  function closeAirbnbModal() {
    if (importAirbnbCalendarMutation.isPending) return;
    setShowAirbnbModal(false);
    setIcalUrl('');
  }

  function handleAirbnbImport() {
    const trimmedUrl = icalUrl.trim();

    if (!trimmedUrl) {
      Alert.alert('Missing link', 'Enter your Airbnb calendar link.');
      return;
    }

    if (!trimmedUrl.startsWith('https://') || !trimmedUrl.includes('.ics')) {
      Alert.alert(
        'Invalid link',
        'Enter a secure Airbnb calendar link containing .ics.',
      );
      return;
    }

    importAirbnbCalendarMutation.mutate(
      { propertyId, icalUrl: trimmedUrl },
      {
        onSuccess: () => {
          setShowAirbnbModal(false);
          setIcalUrl('');
        },
      },
    );
  }

  return (
    <ScrollView
      style={styles.modalScreen}
      contentContainerStyle={styles.modalScreenContent}
    >
      {/* Heading */}
      <View style={styles.modalHeader}>
        <Heading size='2xl'>Property Details</Heading>

        <View style={styles.modalHeaderActions}>
          <Button
            className='rounded-full'
            style={[styles.button, styles.modalHeaderButton]}
            size='lg'
            onPress={handleUpdate}
            isDisabled={isPropertyMutationPending}
          >
            <ButtonIcon style={styles.buttonText} as={Save} />
            <ButtonText style={styles.buttonText}>
              {updatePropertyMutation.isPending ? 'Saving...' : 'Save'}
            </ButtonText>
          </Button>
        </View>
      </View>

      {/* Content */}
      <View style={styles.modalMain}>
        <Pressable
          onPress={handlePickPropertyPicture}
          disabled={
            isOpeningPicturePicker || uploadPropertyPictureMutation.isPending
          }
          accessibilityRole='button'
          accessibilityLabel={
            property.propertyPicturePath
              ? 'Edit property picture'
              : 'Upload property picture'
          }
        >
          <Card style={styles.mediumCardWithAvatar}>
            <View style={styles.mediumCardWithAvatarLeft}>
              <Avatar>
                <AvatarFallbackText>Property</AvatarFallbackText>
                <AvatarImage
                  source={
                    propertyPicture && propertyPicture !== failedPropertyPicture
                      ? { uri: propertyPicture }
                      : require('@/assets/house.jpg')
                  }
                  onError={() => {
                    if (propertyPicture) {
                      setFailedPropertyPicture(propertyPicture);
                    }
                  }}
                />
              </Avatar>
            </View>
            <View className='w-full'>
              <Text>
                {isOpeningPicturePicker
                  ? 'Opening photos...'
                  : uploadPropertyPictureMutation.isPending
                    ? 'Uploading property picture...'
                    : property.propertyPicturePath
                      ? 'Edit property picture'
                      : 'Upload property picture'}
              </Text>
            </View>
          </Card>
        </Pressable>

        <Pressable
          onPress={() => setShowAirbnbModal(true)}
          disabled={property.airbnbCalendarConnected}
          accessibilityState={{
            disabled: property.airbnbCalendarConnected,
          }}
        >
          <Card style={styles.mediumCardWithAvatar}>
            <View style={styles.mediumCardWithAvatarLeft}>
              <Avatar>
                <AvatarFallbackText>Airbnb Logo</AvatarFallbackText>
                <AvatarImage source={require('@/assets/airbnb.png')} />
              </Avatar>
            </View>
            <View className='w-full'>
              <Text>
                {property.airbnbCalendarConnected
                  ? 'Your Airbnb calendar is connected!'
                  : 'Import Airbnb calendar'}
              </Text>
            </View>
          </Card>
        </Pressable>

        {/* Property Details */}
        <PropertyEditingSection
          property={propertyForm}
          onPropertyChange={setPropertyForm}
        />

        {/* Checklist Items */}
        <ChecklistEditingSection
          items={checklistItems}
          onItemsChange={() => console.log('Items changed.')}
        />

        {/* Issues */}
        <ReportedIssuesSection issues={issues ?? []} />

        {/* Delete Button */}
        <Button
          className='rounded-full'
          style={[styles.buttonWarn, styles.modalHeaderButton]}
          size='lg'
          onPress={handleDelete}
          isDisabled={isPropertyMutationPending}
        >
          <ButtonIcon style={styles.buttonTextWarn} as={Trash} />
          <ButtonText style={styles.buttonTextWarn}>
            {deletePropertyMutation.isPending
              ? 'Deleting Property...'
              : 'Delete Property'}
          </ButtonText>
        </Button>
      </View>

      <Modal
        isOpen={showAirbnbModal}
        onClose={closeAirbnbModal}
        size='md'
        useRNModal
      >
        <ModalBackdrop />
        <ModalContent className='rounded-4xl'>
          <ModalHeader>
            <Heading size='lg'>Import Airbnb calendar</Heading>
            <ModalCloseButton onPress={closeAirbnbModal}>
              <Icon as={CloseIcon} />
            </ModalCloseButton>
          </ModalHeader>

          <ModalBody>
            <View className='gap-2'>
              <Text>Paste the Airbnb calendar link for this property.</Text>
              <Input className='rounded-2xl'>
                <InputField
                  value={icalUrl}
                  onChangeText={setIcalUrl}
                  placeholder='https://www.airbnb.com/calendar/ical/...ics'
                  autoCapitalize='none'
                  autoCorrect={false}
                  keyboardType='url'
                />
              </Input>
            </View>
          </ModalBody>

          <ModalFooter>
            <Button
              variant='outline'
              style={styles.button}
              onPress={closeAirbnbModal}
              isDisabled={importAirbnbCalendarMutation.isPending}
            >
              <ButtonText style={styles.buttonText}>Cancel</ButtonText>
            </Button>
            <Button
              style={styles.button}
              onPress={handleAirbnbImport}
              isDisabled={importAirbnbCalendarMutation.isPending}
            >
              <ButtonText style={styles.buttonText}>
                {importAirbnbCalendarMutation.isPending
                  ? 'Importing...'
                  : 'Confirm'}
              </ButtonText>
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </ScrollView>
  );
}
