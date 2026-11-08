import { Organization } from '@/types/entityTypes';
import { request } from '@/api/apiClient';

export function getAllOrganizations(): Promise<Organization[]> {
  return request<Organization[]>({
    method: 'GET',
    url: '/organizations',
  });
}
