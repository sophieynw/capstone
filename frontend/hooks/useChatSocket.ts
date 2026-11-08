import { Client } from '@stomp/stompjs';
import { useContext, useEffect, useRef, useState } from 'react';

import { API_BASE_URL } from '@/api/apiClient';
import { AuthContext } from '@/auth/AuthContext';
import { ChatMessage, SendChatMessagePayload } from '@/types/entityTypes';

const socketUrl = API_BASE_URL
  ? `${API_BASE_URL.replace(/^http/, 'ws')}/ws`
  : 'wss://restfulhousekeeping-v1.onrender.com/ws';

export function useChatSocket(onMessage: (message: ChatMessage) => void) {
  const { token } = useContext(AuthContext);
  const clientRef = useRef<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    console.log('Starting chat socket:', {
      hasToken: !!token,
      socketUrl,
    });

    if (!token) {
      console.error('Chat socket not started: AuthContext has no token');
      return;
    }

    const client = new Client({
      brokerURL: socketUrl,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      connectionTimeout: 15000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      forceBinaryWSFrames: true,
      appendMissingNULLonIncoming: true,
    });

    client.onConnect = () => {
      console.log('STOMP connected successfully');
      setIsConnected(true);

      client.subscribe('/user/queue/messages', (frame) => {
        console.log('Received chat message:', frame.body);

        const message: ChatMessage = JSON.parse(frame.body);
        onMessage(message);
      });
    };

    client.onStompError = (frame) => {
      console.error('STOMP broker error:', {
        message: frame.headers.message,
        body: frame.body,
      });
    };

    client.onWebSocketError = (error) => {
      console.error('WebSocket transport error:', error);
    };

    client.onWebSocketClose = (event) => {
      setIsConnected(false);

      console.debug('WebSocket closed:', {
        code: event.code,
        reason: event.reason,
        wasClean: event.wasClean,
      });
    };

    clientRef.current = client;
    client.activate();

    return () => {
      console.log('Deactivating chat socket');
      clientRef.current = null;
      void client.deactivate();
    };
  }, [token, onMessage]);
  function sendMessage(payload: SendChatMessagePayload) {
    const client = clientRef.current;

    if (!client?.connected) {
      throw new Error('Chat is not connected');
    }

    client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(payload),
    });
  }

  return { isConnected, sendMessage };
}
