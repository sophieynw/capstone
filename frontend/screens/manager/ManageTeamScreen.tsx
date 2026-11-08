// screens/home/ManageTeamScreen.tsx
import { useState } from 'react';
import { Text } from '@/components/ui/text';
import { Alert, Linking, Pressable, ScrollView, View } from 'react-native';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import {
  Icon,
  MailIcon,
  MessageCircleIcon,
  PhoneIcon,
  TrashIcon,
} from '@/components/ui/icon';
import { Heading } from '@/components/ui/heading';
import { Card } from '@/components/ui/card';
import { UserAvatar } from '@/components/UserAvatar';
import { styles } from '@/styles/styles';
import { useCleaners, useRemoveCleanerFromTeam } from '@/hooks/useUsers';
import { findOrCreateConversation } from '@/api/chatApi';
import { User } from '@/types/entityTypes';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';
import { InviteTeamMemberModal } from '@/components/InviteTeamMemberModal';

type CleanerCardProps = {
  cleaner: User;
  onMessagePress: () => void;
  onRemovePress: () => void;
  isRemoving: boolean;
};

function CleanerCard({
  cleaner,
  onMessagePress,
  onRemovePress,
  isRemoving,
}: CleanerCardProps) {
  const fullName = `${cleaner.firstName} ${cleaner.lastName}`;

  return (
    <Card className='w-full gap-1.5 rounded-4xl'>
      <View className='flex-row items-center'>
        {/* Left (Image) */}
        <UserAvatar userId={cleaner.id} name={fullName} className='h-10 w-10' />

        {/* Middle (Text) */}
        <View className='ml-3 flex-1 gap-0'>
          <Heading size='md'>{fullName}</Heading>

          <View className='flex-row items-center gap-2'>
            <Icon as={PhoneIcon} size='sm' />

            <Text
              className='underline'
              onPress={() =>
                Linking.openURL(`tel:${cleaner.phoneNumber.replace(/\D/g, '')}`)
              }
            >
              {cleaner.phoneNumber}
            </Text>
          </View>
        </View>

        {/* Right (Icons) */}
        <View className='flex-row gap-3'>
          <Pressable onPress={onMessagePress}>
            <Icon as={MessageCircleIcon} />
          </Pressable>

          <Pressable
            onPress={onRemovePress}
            disabled={isRemoving}
            accessibilityRole='button'
            accessibilityLabel={`Remove ${fullName} from the team`}
          >
            <Icon as={TrashIcon} style={styles.buttonTextWarn} />
          </Pressable>
        </View>
      </View>
    </Card>
  );
}

export default function ManageTeamScreen({ navigation }: any) {
  const { data: cleaners = [], isPending, isError, error } = useCleaners();
  const removeCleaner = useRemoveCleanerFromTeam();
  const [showInviteModal, setShowInviteModal] = useState(false);

  function handleRemoveCleaner(cleaner: User) {
    const fullName = `${cleaner.firstName} ${cleaner.lastName}`;

    Alert.alert(
      'Remove team member?',
      `${fullName} will be removed from your team and unassigned from all upcoming cleanings. Their account will remain active.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Remove',
          style: 'destructive',
          onPress: () => {
            removeCleaner.mutate(cleaner.id, {
              onSuccess: () => {
                Alert.alert(
                  'Team member removed',
                  `${fullName} was removed from your team.`,
                );
              },
              onError: (removeError) => {
                console.error('Remove cleaner error:', removeError);
                Alert.alert(
                  'Unable to remove team member',
                  'The cleaner could not be removed. Please try again.',
                );
              },
            });
          },
        },
      ],
    );
  }

  if (isPending) {
    return <LoadingSpinner label='Loading cleaners...' />;
  }

  if (isError) {
    console.error('Cleaners error:', error);
    return <ErrorState />;
  }

  return (
    <ScrollView
      style={styles.modalScreen}
      contentContainerStyle={styles.modalScreenContent}
    >
      {/* Header */}
      <View style={styles.modalHeader}>
        <Heading size='2xl'>My Cleaning Team</Heading>

        <Button
          className='rounded-full'
          style={[styles.button, styles.modalHeaderButton]}
          size='lg'
          onPress={() => setShowInviteModal(true)}
        >
          <ButtonIcon style={styles.buttonText} as={MailIcon} />
          <ButtonText style={styles.buttonText}>Invite</ButtonText>
        </Button>
      </View>

      {/* Content / Cleaner Cards */}
      <View style={styles.modalMain}>
        {cleaners.length > 0 ? (
          cleaners.map((cleaner) => (
            <CleanerCard
              key={cleaner.id}
              cleaner={cleaner}
              isRemoving={
                removeCleaner.isPending &&
                removeCleaner.variables === cleaner.id
              }
              onRemovePress={() => handleRemoveCleaner(cleaner)}
              onMessagePress={async () => {
                try {
                  const conversation = await findOrCreateConversation(
                    cleaner.id,
                  );

                  navigation.navigate('ChatScreen', {
                    conversationId: conversation.id,
                    otherUserName: conversation.otherUserName,
                  });
                } catch (error) {
                  console.error(error);
                  Alert.alert(
                    'Chat error',
                    'Could not open this conversation.',
                  );
                }
              }}
            />
          ))
        ) : (
          <Text className='text-center text-gray-500'>No cleaners found.</Text>
        )}
      </View>

      <InviteTeamMemberModal
        isOpen={showInviteModal}
        onClose={() => setShowInviteModal(false)}
      />
    </ScrollView>
  );
}
