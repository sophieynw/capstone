import { ScrollView, Text, View } from 'react-native';
import { Card } from '@/components/ui/card';
import { Heading } from '@/components/ui/heading';
import { styles } from '@/styles/styles';
import { useIssues, useResolveIssue } from '@/hooks/useIssues';
import { toFriendlyDate } from '@/utils/helpers';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';
import { EmptyState } from '@/components/EmptyState';
import { Button, ButtonText } from '@/components/ui/button';

export default function IssuesScreen() {
  const { data: issues, isPending, isError, error } = useIssues();
  const resolveIssueMutation = useResolveIssue();

  if (isPending) {
    return <LoadingSpinner label='Loading issues...' />;
  }

  if (isError) {
    console.error('Issues error:', error);
    return <ErrorState />;
  }

  const issueList = issues ?? [];

  return (
    <ScrollView
      style={styles.modalScreen}
      contentContainerStyle={styles.modalScreenContent}
    >
      <View style={styles.modalHeader}>
        <Heading size='2xl'>Issues</Heading>
      </View>

      <View style={styles.modalMain}>
        {issueList.length === 0 && (
          <EmptyState
            title='No issues reported'
            message="When a cleaner reports a problem, it'll show up here."
          />
        )}

        {issueList.map((issue) => (
          <Card key={issue.id} style={styles.mediumCard}>
            <View className='flex-row items-center gap-3'>
              <View className='flex-1 gap-0.5'>
                <Heading size='md'>{issue.issueType}</Heading>
                <Text>{issue.description}</Text>

                {issue.propertyName && (
                  <Text className='text-sm'>{issue.propertyName}</Text>
                )}
                <Text className='text-sm'>
                  Reported by {issue.reportedByName}
                </Text>
                <Text className='text-sm'>
                  {toFriendlyDate(issue.reportedAt)}
                </Text>
              </View>

              <Button
                size='sm'
                style={styles.button}
                disabled={resolveIssueMutation.isPending}
                accessibilityLabel={`Resolve ${issue.issueType} issue`}
                onPress={() => resolveIssueMutation.confirmResolve(issue.id)}
              >
                <ButtonText style={styles.buttonText}>
                  {resolveIssueMutation.isPending &&
                  resolveIssueMutation.variables === issue.id
                    ? 'Resolving...'
                    : 'Resolve'}
                </ButtonText>
              </Button>
            </View>
          </Card>
        ))}
      </View>
    </ScrollView>
  );
}
