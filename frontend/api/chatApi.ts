import { request } from '@/api/apiClient';
import { ChatMessage, Conversation } from '@/types/entityTypes';

type MessagePage = {
  content: ChatMessage[];
  totalPages: number;
  totalElements: number;
  last: boolean;
};

export function findOrCreateConversation(
  userId: number,
): Promise<Conversation> {
  return request({
    method: 'POST',
    url: `/chat/conversations/with/${userId}`,
  });
}

export function findOrCreateManagerConversation(): Promise<Conversation> {
  return request<Conversation>({
    method: 'POST',
    url: '/chat/conversations/manager',
  });
}

export function getMessages(
  conversationId: number,
  page = 0,
): Promise<MessagePage> {
  return request({
    method: 'GET',
    url: `/chat/conversations/${conversationId}/messages`,
    params: { page, size: 30 },
  });
}
