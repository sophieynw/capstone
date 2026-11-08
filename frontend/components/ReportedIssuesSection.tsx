import { Text, View } from 'react-native';
import { Card } from '@/components/ui/card';
import { Heading } from '@/components/ui/heading';
import { styles } from '@/styles/styles';
import { Issue } from '@/api/issuesApi';
import { EmptyState } from '@/components/EmptyState';
import { toFriendlyDate } from '@/utils/helpers';
import { Button, ButtonText } from '@/components/ui/button';
import { useResolveIssue } from '@/hooks/useIssues';

type ReportedIssuesSectionProps = {
  issues: Issue[];
};

export function ReportedIssuesSection({ issues }: ReportedIssuesSectionProps) {
  const resolveIssueMutation = useResolveIssue();

  return (
    <Card style={styles.mediumCard}>
      <Heading size='md'>Issues</Heading>

      {issues.length === 0 && <EmptyState title='No issues reported' inline />}

      {issues.map((issue) => (
        <View key={issue.id} className='flex-row items-center gap-3'>
          <View className='flex-1 gap-0.5'>
            <Text className='font-semibold'>{issue.issueType}</Text>
            <Text>{issue.description}</Text>
            <Text className='text-sm'>Reported by {issue.reportedByName}</Text>
            {issue.cleaningStart && (
              <Text className='text-sm'>
                {toFriendlyDate(issue.cleaningStart)}
              </Text>
            )}
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
      ))}
    </Card>
  );
}
