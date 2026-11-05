// hooks/useChecklistItems.ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getChecklistItems } from '@/api/checklistItemsApi';

export function useChecklistItems(propertyId?: number) {
  return useQuery({
    queryKey: ['checklist-items', propertyId],
    queryFn: () => getChecklistItems(propertyId!),
    enabled: propertyId !== undefined,
  });
}