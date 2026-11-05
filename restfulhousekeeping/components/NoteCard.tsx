import { Text } from 'react-native';
import { Card } from '@/components/ui/card';
import { Heading } from '@/components/ui/heading';
import { styles } from '@/styles/styles';
import { CleaningNote } from '@/types/entityTypes';

export function NoteCard({ note }: { note: CleaningNote }) {
  const createdAt = new Date(note.createdAt);
  const date = createdAt.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  });
  const time = createdAt.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });

  return (
    <Card style={styles.mediumCard}>
      <Heading size='md'>
        {`${note.authorFirstName || 'Unknown author'} · ${date} ${time}`}
      </Heading>
      <Text>{note.content}</Text>
    </Card>
  );
}
