// hooks/useChecklistItems.ts
import { useQuery } from '@tanstack/react-query';
import { getChecklistItems, getDueChecklistItems } from '@/api/checklistItemsApi';

export function useChecklistItems(propertyId?: number) {
  return useQuery({
    queryKey: ['checklist-items', propertyId],
    queryFn: () => getChecklistItems(propertyId!),
    enabled: propertyId !== undefined,
  });
}

export function useDueChecklistItems(propertyId: number | undefined, start: string) {
  return useQuery({
    queryKey: ['due-checklist-items', propertyId, start],
    queryFn: () => getDueChecklistItems(propertyId!, start),
    enabled: propertyId !== undefined,
  });
}
