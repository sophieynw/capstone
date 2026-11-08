import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert } from 'react-native';

import {
  acceptInvitation,
  createInvitation,
  declineInvitation,
  getCleanerInvitations,
} from '@/api/invitationApi';
import { CreateInvitationPayload, InvitationStatus } from '@/types/entityTypes';
import { AuthContext } from '@/auth/AuthContext';
import { useContext } from 'react';

export function useCreateInvitation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateInvitationPayload) => createInvitation(payload),

    onSuccess: async (invitation) => {
      queryClient.setQueryData(['invitation-id', invitation.id], invitation);

      await queryClient.invalidateQueries({
        queryKey: ['organization-invitations', invitation.organizationId],
      });
    },

    onError: (error) => {
      console.error('Create invitation error:', error);
    },
  });
}

export function useCleanerInvitations(cleanerId?: number) {
  return useQuery({
    queryKey: ['cleaner-invitations', cleanerId],
    queryFn: () => getCleanerInvitations(cleanerId!, InvitationStatus.PENDING),
    enabled: cleanerId != null,
  });
}

export function useAcceptInvitation() {
  const queryClient = useQueryClient();
  const { refreshUser } = useContext(AuthContext);

  return useMutation({
    mutationFn: (invitationId: number) => acceptInvitation(invitationId),

    onSuccess: async () => {
      await refreshUser();

      await queryClient.invalidateQueries({
        queryKey: ['cleaner-invitations'],
      });

      await queryClient.invalidateQueries({
        queryKey: ['organization-invitations'],
      });

      Alert.alert('Success', 'You have accepted the invitation.');
    },

    onError: (error) => {
      console.error('Accept invitation error:', error);
      Alert.alert('Error', 'Could not accept the invitation.');
    },
  });
}

export function useDeclineInvitation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: number) => declineInvitation(invitationId),

    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['cleaner-invitations'],
      });

      await queryClient.invalidateQueries({
        queryKey: ['organization-invitations'],
      });

      Alert.alert('Invitation declined', 'The invitation has been declined.');
    },

    onError: (error) => {
      console.error('Decline invitation error:', error);
      Alert.alert('Error', 'Could not decline the invitation.');
    },
  });
}
