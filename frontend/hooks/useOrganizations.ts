import { useQuery } from '@tanstack/react-query';
import { getAllOrganizations } from '@/api/organizationsApi';

export function useOrganizations() {
  return useQuery({
    queryKey: ['organizations'],
    queryFn: () => getAllOrganizations(),
  });
}
