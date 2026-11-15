import { ScrollView, View } from 'react-native';
import { styles } from '@/styles/styles';
import { Heading } from '@/components/ui/heading';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import { Save } from 'lucide-react-native';
import {
  createEditableProfile,
  EditableProfile,
  ProfileEditingSection,
} from '@/components/ProfileEditingSection';
import { useContext, useEffect, useState } from 'react';
import { AuthContext } from '@/auth/AuthContext';
import { useUpdateUser } from '@/hooks/useUsers';

export default function ProfileEditScreen() {
  const { user } = useContext(AuthContext);
  const [profileForm, setProfileForm] = useState<EditableProfile>(() =>
    createEditableProfile(),
  );

  useEffect(() => {
    if (user) {
      setProfileForm(createEditableProfile(user));
    }
  }, [user]);

  const updateUserMutation = useUpdateUser();

  function handleUpdate() {
    if (!user) return;
    updateUserMutation.mutate({
      userId: user.id,
      user: profileForm,
    });
  }

  return (
    <ScrollView
      style={styles.modalScreen}
      contentContainerStyle={styles.modalScreenContent}
    >
      <View style={styles.modalHeader}>
        <Heading size='2xl'>Edit Profile</Heading>
      </View>

      <View style={styles.modalMain}>
        <ProfileEditingSection
          user={profileForm}
          onProfileChange={setProfileForm}
        />

        <Button
          className='rounded-full'
          style={styles.button}
          size='lg'
          onPress={handleUpdate}
        >
          <ButtonIcon style={styles.buttonText} as={Save} />
          <ButtonText style={styles.buttonText}>
            {updateUserMutation.isPending ? 'Saving...' : 'Save'}
          </ButtonText>
        </Button>
      </View>
    </ScrollView>
  );
}
