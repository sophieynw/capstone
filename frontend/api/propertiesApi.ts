// api/propertiesApi.ts
import { request } from '@/api/apiClient';
import {
  CalendarImportResponse,
  CreatePropertyPayload,
  Property,
  UpdatePropertyPayload,
} from '@/types/entityTypes';

export function getPropertyById(propertyId: number): Promise<Property> {
  return request<Property>({
    method: 'GET',
    url: `/properties/${propertyId}`,
  });
}

export function getAllProperties(): Promise<Property[]> {
  return request<Property[]>({
    method: 'GET',
    url: `/properties`,
  });
}

export function createProperty(
  property: CreatePropertyPayload,
): Promise<Property> {
  return request<Property>({
    method: 'POST',
    url: '/properties',
    data: property,
  });
}

export function deletePropertyById(propertyId: number): Promise<void> {
  return request<void>({
    method: 'DELETE',
    url: `/properties/${propertyId}`,
  });
}

export function updatePropertyById(
  propertyId: number,
  property: UpdatePropertyPayload,
): Promise<Property> {
  return request<Property>({
    method: 'PATCH',
    url: `/properties/${propertyId}`,
    data: property,
  });
}

export function importAirbnbCalendar(
  propertyId: number,
  icalUrl: string,
): Promise<CalendarImportResponse> {
  return request<CalendarImportResponse>({
    method: 'POST',
    url: `/properties/${propertyId}/calendar-imports`,
    data: { icalUrl },
  });
}

type PickedImage = {
  uri: string;
  mimeType?: string | null;
  fileName?: string | null;
};

export function uploadPropertyPicture(
  propertyId: number,
  image: PickedImage,
): Promise<Property> {
  const formData = new FormData();
  formData.append('file', {
    uri: image.uri,
    type: image.mimeType ?? 'image/jpeg',
    name: image.fileName ?? `property-${Date.now()}.jpg`,
  } as any);

  return request<Property>({
    method: 'POST',
    url: `/properties/${propertyId}/picture`,
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}
