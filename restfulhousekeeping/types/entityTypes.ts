// types/entityTypes.ts

export enum Role {
  MANAGER = 'MANAGER',
  CLEANER = 'CLEANER',
}

export enum IssueType {
  DAMAGE = 'Damage',
  MISSING_SUPPLIES = 'Missing Supplies',
  ACCESS_PROBLEM = 'Access Problem',
  OTHER = 'Other',
}

export enum DaysOfTheWeek {
  MONDAY = 'MONDAY',
  TUESDAY = 'TUESDAY',
  WEDNESDAY = 'WEDNESDAY',
  THURSDAY = 'THURSDAY',
  FRIDAY = 'FRIDAY',
  SATURDAY = 'SATURDAY',
  SUNDAY = 'SUNDAY',
}

export interface Organization {
  id: number;
  name: string;
  description: string | null;
}

export interface User {
  id: number;
  organization: Organization | null;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  phoneNumber: string;
  role: Role;
  profilePicturePath?: string | null;
}

export interface Cleaning {
  id: number;
  managerId: number;
  cleanerId: number | null;
  property: Property;
  dateTimeStart: string;
  dateTimeEnd: string;
  dateTimeStarted: string | null;
  dateTimeCompleted: string | null;
  notes: string | null;
  cleaningChecklistItems: CleaningChecklistItem[];
  isComplete: boolean;
}

// the default items associated with each property
// or before they are attached to a cleaning
export interface ChecklistItem {
  id: number;
  description: string;
  frequencyDays: number | null;
  lastCompleted: string | null;
}

// an item attached to one specific cleaning
export interface CleaningChecklistItem {
  id: number;
  description: string;
  isComplete: boolean;
}

export interface CleaningNote {
  id: number;
  authorFirstName: string | null;
  authorLastName: string | null;
  content: string;
  createdAt: string;
}

export interface Property {
  id: number;
  name: string;
  street: string | null;
  unit: string | null;
  city: string | null;
  province: string | null;
  postalCode: string | null;
  country: string | null;
  checkoutTime: string | null;
  checkinTime: string | null;
  accessInstructions: string | null;
  propertyPicturePath?: string | null;
  airbnbCalendarConnected?: boolean;
}

export interface AvailabilitySlot {
  id: number;
  //private Availability availability;
  cleanerId: number;
  dayOfWeek: DaysOfTheWeek;
  startTime: string;
  endTime: string;
}

export type CreateCleaningChecklistItem =
  | {
      checklistItem: {
        id: number;
      };
    }
  | {
      customDescription: string;
    };

export interface CreateCleaningPayload {
  manager: {
    id: number;
    role: Role.MANAGER;
  };
  cleaner: {
    id: number;
    role: Role.CLEANER;
  } | null;
  property: {
    id: number;
  };
  dateTimeStart: string;
  dateTimeEnd: string;
  notes: string | null;
  cleaningChecklistItems: CreateCleaningChecklistItem[];
}

export interface CreateChecklistItemPayload {
  description: string;
  frequencyDays: number | null;
}

export interface CreatePropertyPayload {
  property: {
    manager: {
      id: number;
      role: Role.MANAGER;
    };
    name: string;
    street: string;
    unit: string;
    city: string;
    province: string;
    postalCode: string;
    country: string;
    checkoutTime: string;
    checkinTime: string;
    accessInstructions: string | null;
  };
  checklistItems: CreateChecklistItemPayload[];
}

export interface UpdatePropertyPayload {
  name?: string;
  street?: string;
  unit?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  country?: string;
  checkoutTime?: string;
  checkinTime?: string;
  accessInstructions?: string;
  managerId?: number;
}

export interface CalendarImportResponse {
  propertyId: number;
  eventsFound: number;
  reservationsFound: number;
  cleaningsCreated: number;
  duplicatesSkipped: number;
  pastReservationsIgnored: number;
  eventsIgnored: number;
  cleaningIds: number[];
}

export interface CreateUserPayload {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  phoneNumber: string;
  role: Role;
  organizationName?: string;
}

export interface UpdateUserPayload {
  firstName?: string;
  lastName?: string;
  email?: string;
  phoneNumber?: string;
  organization?: Organization;
}

export interface Conversation {
  id: number;
  otherUserId: number;
  otherUserName: string;
  createdAt: string;
}

export interface ChatMessage {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  body: string;
  sentAt: string;
  readAt: string | null;
}

export interface SendChatMessagePayload {
  conversationId: number;
  body: string;
}

export enum InvitationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
}

export interface Invitation {
  id: number;
  cleanerId: number | null;
  cleanerName: string | null;
  invitedEmail: string;
  organizationId: number;
  organizationName: string;
  invitedByUserId: number;
  status: InvitationStatus;
  createdAt: string;
}

export interface CreateInvitationPayload {
  email: string;
}
