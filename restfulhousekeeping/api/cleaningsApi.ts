// api/cleaningsApi.ts
import { request } from '@/api/apiClient';
import {
  Cleaning,
  CleaningChecklistItem,
  CreateCleaningPayload,
} from '@/types/entityTypes';

export function getUpcomingCleanings(userId: number): Promise<Cleaning[]> {
  return request<Cleaning[]>({
    method: 'GET',
    url: `/cleanings/upcoming/${userId}`,
  });
}

export function getNextCleaningByProperty(
  propertyId: number,
): Promise<Cleaning> {
  return request<Cleaning>({
    method: 'GET',
    url: `/cleanings/upcoming/${propertyId}/first`,
  });
}

export function getCleaningById(id: number): Promise<Cleaning> {
  return request<Cleaning>({
    method: 'GET',
    url: `/cleanings/${id}`,
  });
}

export function createCleaning(
  cleaning: CreateCleaningPayload,
): Promise<Cleaning> {
  return request<Cleaning>({
    method: 'POST',
    url: '/cleanings',
    data: cleaning,
  });
}

export function completeCleaning(cleaningId: number): Promise<Cleaning> {
  return request<Cleaning>({
    method: 'PATCH',
    url: `/cleanings/${cleaningId}/complete`,
  });
}

export function updateCleaningCleaner(
  cleaningId: number,
  cleanerId: number | null,
): Promise<Cleaning> {
  return request<Cleaning>({
    method: 'PATCH',
    url: `/cleanings/${cleaningId}/cleaner`,
    data: { cleanerId },
  });
}

export function updateCleaningSchedule(
  cleaningId: number,
  dateTimeStart: string,
  dateTimeEnd: string,
): Promise<Cleaning> {
  return request<Cleaning>({
    method: 'PATCH',
    url: `/cleanings/${cleaningId}/schedule`,
    data: { dateTimeStart, dateTimeEnd },
  });
}

export function getCompletedCleanings(userId: number): Promise<Cleaning[]> {
  return request<Cleaning[]>({
    method: 'GET',
    url: `/cleanings/completed/${userId}`,
  });
}

export function deleteCleaning(cleaningId: number): Promise<void> {
  return request<void>({
    method: 'DELETE',
    url: `/cleanings/${cleaningId}`,
  });
}

export function addCleaningChecklistItem(
  cleaningId: number,
  description: string,
): Promise<CleaningChecklistItem> {
  return request<CleaningChecklistItem>({
    method: 'POST',
    url: `/cleanings/${cleaningId}/checklist-items`,
    data: { description },
  });
}

export function deleteCleaningChecklistItem(
  cleaningId: number,
  itemId: number,
): Promise<void> {
  return request<void>({
    method: 'DELETE',
    url: `/cleanings/${cleaningId}/checklist-items/${itemId}`,
  });
}

export function updateCleaningChecklistItem(
  cleaningId: number,
  itemId: number,
  isComplete: boolean,
): Promise<CleaningChecklistItem> {
  return request<CleaningChecklistItem>({
    method: 'PATCH',
    url: `/cleanings/${cleaningId}/checklist-items/${itemId}`,
    data: { isComplete },
  });
}