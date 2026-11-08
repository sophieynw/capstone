import { request } from '@/api/apiClient';
import { AvailabilitySlot, DaysOfTheWeek } from '@/types/entityTypes';

export function getAvailabilitySlots(
  cleanerId: number,
): Promise<AvailabilitySlot[]> {
  return request<AvailabilitySlot[]>({
    method: 'GET',
    url: `/availability-slots/cleaner/${cleanerId}`,
  });
}

export function createAvailabilitySlot(payload: {
  dayOfWeek: DaysOfTheWeek;
  startTime: string;
  endTime: string;
}): Promise<AvailabilitySlot> {
  return request<AvailabilitySlot>({
    method: 'POST',
    url: '/availability-slots',
    data: payload,
  });
}

export function updateAvailabilitySlot(
  id: number,
  payload: { startTime: string; endTime: string },
): Promise<AvailabilitySlot> {
  return request<AvailabilitySlot>({
    method: 'PATCH',
    url: `/availability-slots/${id}`,
    data: payload,
  });
}

export function deleteAvailabilitySlot(id: number): Promise<void> {
  return request<void>({
    method: 'DELETE',
    url: `/availability-slots/${id}`,
  });
}