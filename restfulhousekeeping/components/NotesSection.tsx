import { View } from 'react-native';
import { Save } from 'lucide-react-native';
import { Button, ButtonIcon, ButtonText } from '@/components/ui/button';
import { Heading } from '@/components/ui/heading';
import { Textarea, TextareaInput } from '@/components/ui/textarea';
import { styles } from '@/styles/styles';

type NotesSectionProps = {
  notes: string;
  setNotes: (notes: string) => void;
  onSave?: () => void;
  isSaving?: boolean;
};

export function NotesSection({
  notes,
  setNotes,
  onSave,
  isSaving = false,
}: NotesSectionProps) {
  return (
    <View className='flex-col justify-center gap-2'>
      <Heading size='md'>Notes</Heading>
      <View className='py-2'>
        <Textarea className='w-full rounded-3xl px-2'>
          <TextareaInput
            placeholder='Add a note here...'
            value={notes}
            onChangeText={setNotes}
            editable={!isSaving}
            maxLength={onSave ? 2000 : undefined}
          />
        </Textarea>
      </View>
      {onSave && (
        <Button
          className='self-end rounded-full'
          style={styles.button}
          onPress={onSave}
          isDisabled={isSaving || !notes.trim()}
        >
          <ButtonIcon style={styles.buttonText} as={Save} />
          <ButtonText style={styles.buttonText}>
            {isSaving ? 'Saving...' : 'Save'}
          </ButtonText>
        </Button>
      )}
    </View>
  );
}
