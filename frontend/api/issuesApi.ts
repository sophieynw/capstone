import { request } from '@/api/apiClient';
import type { IssueType } from '@/types/entityTypes';

export interface CreateIssuePayload {
  cleaningId: number;
  issueType: IssueType;
  description: string;
}

export interface Issue {
  id: number;
  cleaningId: number;
  propertyId: number;
  reportedById: number;
  reportedByName: string;
  issueType: IssueType;
  description: string;
  cleaningStart: string | null;
  propertyName: string | null;
  reportedAt: string | null;
}

export function createIssue(payload: CreateIssuePayload): Promise<Issue> {
  return request<Issue>({
    method: 'POST',
    url: '/issues',
    data: payload,
  });
}

export function getIssues(): Promise<Issue[]> {
  return request<Issue[]>({
    method: 'GET',
    url: '/issues',
  });
}

export function resolveIssue(issueId: number): Promise<void> {
  return request<void>({
    method: 'DELETE',
    url: `/issues/${issueId}`,
  });
}
