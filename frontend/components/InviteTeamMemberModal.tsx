import { useState } from 'react';
import { Alert, TextInput } from 'react-native';

import { getApiErrorMessage } from '@/api/apiClient';
import { Button, ButtonText } from '@/components/ui/button';
import {
  Modal,
  ModalBackdrop,
  ModalBody,
  ModalContent,
  ModalFooter,
} from '@/components/ui/modal';
import { Text } from '@/components/ui/text';
import { useCreateInvitation } from '@/hooks/useInvitations';
import { styles } from '@/styles/styles';
import { getEmailValidationError, normalizeEmail } from '@/utils/email';

type InviteTeamMemberModalProps = {
  isOpen: boolean;
  onClose: () => void;
};

export function InviteTeamMemberModal({
  isOpen,
  onClose,
}: InviteTeamMemberModalProps) {
  const [email, setEmail] = useState('');
  const createInvitation = useCreateInvitation();

  function close() {
    if (createInvitation.isPending) return;
    setEmail('');
    onClose();
  }

  async function invite() {
    const validationError = getEmailValidationError(email);
    if (validationError) {
      Alert.alert('Invalid email', validationError);
      return;
    }

    try {
      await createInvitation.mutateAsync({ email: normalizeEmail(email) });
      setEmail('');
      onClose();
      Alert.alert('Invitation sent', 'Your invitation has been sent!');
    } catch (error) {
      Alert.alert(
        'Unable to send invitation',
        getApiErrorMessage(
          error,
          'There was a problem sending the invitation. Please try again.',
        ),
      );
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={close} size='md' useRNModal>
      <ModalBackdrop />
      <ModalContent className='rounded-4xl'>
        <ModalBody>
          <Text className='mb-2'>Team member&apos;s email:</Text>
          <TextInput
            style={styles.input}
            placeholder='cleaner@example.com'
            value={email}
            onChangeText={setEmail}
            autoCapitalize='none'
            autoCorrect={false}
            keyboardType='email-address'
            textContentType='emailAddress'
            editable={!createInvitation.isPending}
            returnKeyType='send'
            onSubmitEditing={() => void invite()}
          />
        </ModalBody>

        <ModalFooter className='gap-2'>
          <Button
            className='flex-1 rounded-full'
            style={styles.buttonWarn}
            onPress={close}
            isDisabled={createInvitation.isPending}
          >
            <ButtonText style={styles.buttonTextWarn}>Cancel</ButtonText>
          </Button>

          <Button
            className='flex-1 rounded-full'
            style={styles.button}
            onPress={() => void invite()}
            isDisabled={createInvitation.isPending}
          >
            <ButtonText style={styles.buttonText}>
              {createInvitation.isPending ? 'Sending...' : 'Invite'}
            </ButtonText>
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
