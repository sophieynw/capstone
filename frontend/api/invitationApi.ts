import { request } from '@/api/apiClient';
import {
  Invitation,
  InvitationStatus,
  CreateInvitationPayload,
} from '@/types/entityTypes';

export function createInvitation(
  invitation: CreateInvitationPayload,
): Promise<Invitation> {
  return request<Invitation>({
    method: 'POST',
    url: '/invitations',
    data: invitation,
  });
}

export function getInvitationById(invitationId: number): Promise<Invitation> {
  return request<Invitation>({
    method: 'GET',
    url: `/invitations/${invitationId}`,
  });
}

export function getOrganizationInvitations(
  organizationId: number,
  status?: InvitationStatus,
): Promise<Invitation[]> {
  return request<Invitation[]>({
    method: 'GET',
    url: `/invitations/organization/${organizationId}`,
    params: status ? { status } : undefined,
  });
}

export function getCleanerInvitations(
  cleanerId: number,
  status?: InvitationStatus,
): Promise<Invitation[]> {
  return request<Invitation[]>({
    method: 'GET',
    url: `/invitations/cleaner/${cleanerId}`,
    params: status ? { status } : undefined,
  });
}

export function acceptInvitation(invitationId: number): Promise<Invitation> {
  return request<Invitation>({
    method: 'PATCH',
    url: `/invitations/${invitationId}/accept`,
  });
}

export function declineInvitation(invitationId: number): Promise<Invitation> {
  return request<Invitation>({
    method: 'PATCH',
    url: `/invitations/${invitationId}/decline`,
  });
}
