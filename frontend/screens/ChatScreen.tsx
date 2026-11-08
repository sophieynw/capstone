import { useCallback, useContext, useEffect, useRef, useState } from 'react';
import {
  Alert,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { ArrowLeft, Send } from 'lucide-react-native';

import { AuthContext } from '@/auth/AuthContext';
import { getMessages } from '@/api/chatApi';
import { useChatSocket } from '@/hooks/useChatSocket';
import { ChatMessage } from '@/types/entityTypes';
import { Heading } from '@/components/ui/heading';
import { Text } from '@/components/ui/text';
import { Icon } from '@/components/ui/icon';
import { LoadingSpinner } from '@/components/LoadingSpinner';
import { ErrorState } from '@/components/ErrorState';

type ChatScreenRouteParams = {
  conversationId: number;
  otherUserName: string;
};

type ChatScreenProps = {
  navigation: any;
  route: {
    params: ChatScreenRouteParams;
  };
};

/**
 * Combines existing and incoming messages without creating duplicates.
 *
 * Messages are sorted from oldest to newest so the newest message appears
 * at the bottom of the chat.
 */
function mergeMessages(
  currentMessages: ChatMessage[],
  incomingMessages: ChatMessage[],
): ChatMessage[] {
  const messagesById = new Map<number, ChatMessage>();

  currentMessages.forEach((message) => {
    messagesById.set(message.id, message);
  });

  incomingMessages.forEach((message) => {
    messagesById.set(message.id, message);
  });

  return Array.from(messagesById.values()).sort(
    (first, second) =>
      new Date(first.sentAt).getTime() - new Date(second.sentAt).getTime(),
  );
}

/**
 * Formats a message timestamp as a short time such as "2:35 PM".
 */
function formatMessageTime(sentAt: string): string {
  const date = new Date(sentAt);

  if (Number.isNaN(date.getTime())) {
    return '';
  }

  return date.toLocaleTimeString([], {
    hour: 'numeric',
    minute: '2-digit',
  });
}

export default function ChatScreen({ navigation, route }: ChatScreenProps) {
  const { conversationId, otherUserName } = route.params;
  const { user } = useContext(AuthContext);

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState<string | null>(null);

  const messageListRef = useRef<FlatList<ChatMessage>>(null);

  /**
   * Called whenever the WebSocket receives a message.
   *
   * Ignore messages belonging to a different conversation. The ID check in
   * mergeMessages prevents duplicate messages after reconnecting or reloading
   * history.
   */
  const handleIncomingMessage = useCallback(
    (message: ChatMessage) => {
      if (message.conversationId !== conversationId) {
        return;
      }

      setMessages((currentMessages) =>
        mergeMessages(currentMessages, [message]),
      );
    },
    [conversationId],
  );

  const { isConnected, sendMessage } = useChatSocket(handleIncomingMessage);

  /**
   * Load existing messages when the screen first opens.
   */
  useEffect(() => {
    let isCancelled = false;

    async function loadMessageHistory() {
      setIsHistoryLoading(true);
      setHistoryError(null);

      try {
        const response = await getMessages(conversationId);

        if (isCancelled) {
          return;
        }

        /*
         * The backend returns newest messages first. mergeMessages sorts them
         * from oldest to newest for display.
         *
         * Merging instead of replacing also preserves a WebSocket message that
         * might arrive while the history request is still loading.
         */
        setMessages((currentMessages) =>
          mergeMessages(currentMessages, response.content),
        );
      } catch (error) {
        console.error('Could not load chat messages:', error);

        if (!isCancelled) {
          setHistoryError('Could not load previous messages.');
        }
      } finally {
        if (!isCancelled) {
          setIsHistoryLoading(false);
        }
      }
    }

    void loadMessageHistory();

    return () => {
      isCancelled = true;
    };
  }, [conversationId]);

  /**
   * Publishes the draft through STOMP.
   *
   * We do not immediately insert a fake local message. The backend saves the
   * message and sends it back with its real database ID and timestamp.
   */
  function handleSend() {
    const body = draft.trim();

    if (!body) {
      return;
    }

    if (!isConnected) {
      Alert.alert(
        'Chat is connecting',
        'Please wait for the chat connection and try again.',
      );
      return;
    }

    try {
      sendMessage({
        conversationId,
        body,
      });

      setDraft('');
    } catch (error) {
      console.error('Could not send chat message:', error);

      Alert.alert(
        'Message not sent',
        'The message could not be sent. Please try again.',
      );
    }
  }

  function renderMessage({ item }: { item: ChatMessage }) {
    const isMyMessage = item.senderId === user?.id;

    return (
      <View
        style={[
          chatStyles.messageRow,
          isMyMessage ? chatStyles.myMessageRow : chatStyles.otherMessageRow,
        ]}
      >
        <View
          style={[
            chatStyles.messageBubble,
            isMyMessage
              ? chatStyles.myMessageBubble
              : chatStyles.otherMessageBubble,
          ]}
        >
          {!isMyMessage && (
            <Text style={chatStyles.senderName}>{item.senderName}</Text>
          )}

          <Text
            style={[
              chatStyles.messageBody,
              isMyMessage && chatStyles.myMessageText,
            ]}
          >
            {item.body}
          </Text>

          <Text
            style={[
              chatStyles.messageTime,
              isMyMessage && chatStyles.myMessageTime,
            ]}
          >
            {formatMessageTime(item.sentAt)}
          </Text>
        </View>
      </View>
    );
  }

  const canSend = draft.trim().length > 0 && isConnected;

  return (
    <SafeAreaView style={chatStyles.safeArea}>
      <KeyboardAvoidingView
        style={chatStyles.keyboardView}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        {/* Header */}
        <View style={chatStyles.header}>
          <Pressable
            style={chatStyles.backButton}
            onPress={() => navigation.goBack()}
            accessibilityRole='button'
            accessibilityLabel='Go back'
          >
            <Icon as={ArrowLeft} size='xl' />
          </Pressable>

          <View style={chatStyles.headerText}>
            <Heading size='lg'>{otherUserName}</Heading>

            <Text
              style={[
                chatStyles.connectionStatus,
                isConnected
                  ? chatStyles.connectedText
                  : chatStyles.connectingText,
              ]}
            >
              {isConnected ? 'Connected' : 'Connecting...'}
            </Text>
          </View>

          {/*
           * Empty view balances the back button and keeps the title centered.
           */}
          <View style={chatStyles.headerSpacer} />
        </View>

        {/* Message history */}
        {isHistoryLoading ? (
          <LoadingSpinner label='Loading messages...' />
        ) : historyError ? (
          <ErrorState />
        ) : (
          <FlatList
            ref={messageListRef}
            data={messages}
            renderItem={renderMessage}
            keyExtractor={(message) => message.id.toString()}
            style={chatStyles.messageList}
            contentContainerStyle={[
              chatStyles.messageListContent,
              messages.length === 0 && chatStyles.emptyMessageListContent,
            ]}
            keyboardShouldPersistTaps='handled'
            onContentSizeChange={() => {
              if (messages.length > 0) {
                messageListRef.current?.scrollToEnd({
                  animated: false,
                });
              }
            }}
            ListEmptyComponent={
              <View style={chatStyles.emptyState}>
                <Heading size='md'>No messages yet</Heading>
                <Text style={chatStyles.emptyStateText}>
                  Send the first message to {otherUserName}.
                </Text>
              </View>
            }
          />
        )}

        {/* Message composer */}
        <View style={chatStyles.composer}>
          <TextInput
            style={chatStyles.messageInput}
            value={draft}
            onChangeText={setDraft}
            placeholder={
              isConnected ? 'Write a message...' : 'Connecting to chat...'
            }
            editable={isConnected}
            multiline
            maxLength={2000}
            returnKeyType='send'
            blurOnSubmit={false}
            onSubmitEditing={() => {
              if (canSend) {
                handleSend();
              }
            }}
          />

          <Pressable
            style={[
              chatStyles.sendButton,
              !canSend && chatStyles.sendButtonDisabled,
            ]}
            onPress={handleSend}
            disabled={!canSend}
            accessibilityRole='button'
            accessibilityLabel='Send message'
          >
            <Icon as={Send} size='lg' style={chatStyles.sendIcon} />
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const chatStyles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  keyboardView: {
    flex: 1,
  },
  header: {
    minHeight: 72,
    flexDirection: 'row',
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
    backgroundColor: '#ffffff',
    paddingHorizontal: 16,
    paddingVertical: 10,
  },
  backButton: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 22,
  },
  headerText: {
    flex: 1,
    alignItems: 'center',
  },
  headerSpacer: {
    width: 44,
  },
  connectionStatus: {
    marginTop: 2,
    fontSize: 12,
  },
  connectedText: {
    color: '#15803d',
  },
  connectingText: {
    color: '#a16207',
  },
  centeredContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 10,
  },
  messageList: {
    flex: 1,
  },
  messageListContent: {
    paddingHorizontal: 16,
    paddingVertical: 18,
    gap: 8,
  },
  emptyMessageListContent: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  emptyState: {
    alignItems: 'center',
    padding: 24,
  },
  emptyStateText: {
    marginTop: 6,
    color: '#6b7280',
    textAlign: 'center',
  },
  messageRow: {
    width: '100%',
    flexDirection: 'row',
  },
  myMessageRow: {
    justifyContent: 'flex-end',
  },
  otherMessageRow: {
    justifyContent: 'flex-start',
  },
  messageBubble: {
    maxWidth: '80%',
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingVertical: 10,
  },
  myMessageBubble: {
    backgroundColor: '#2563eb',
    borderBottomRightRadius: 5,
  },
  otherMessageBubble: {
    backgroundColor: '#ffffff',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    borderBottomLeftRadius: 5,
  },
  senderName: {
    marginBottom: 3,
    color: '#4b5563',
    fontSize: 12,
    fontWeight: '600',
  },
  messageBody: {
    color: '#111827',
    fontSize: 16,
    lineHeight: 21,
  },
  myMessageText: {
    color: '#ffffff',
  },
  messageTime: {
    alignSelf: 'flex-end',
    marginTop: 4,
    color: '#6b7280',
    fontSize: 10,
  },
  myMessageTime: {
    color: '#dbeafe',
  },
  composer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 10,
    borderTopWidth: 1,
    borderTopColor: '#e5e7eb',
    backgroundColor: '#ffffff',
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  messageInput: {
    flex: 1,
    minHeight: 44,
    maxHeight: 120,
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 22,
    backgroundColor: '#f9fafb',
    paddingHorizontal: 16,
    paddingTop: 11,
    paddingBottom: 11,
    color: '#111827',
    fontSize: 16,
  },
  sendButton: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 22,
    backgroundColor: '#2563eb',
  },
  sendButtonDisabled: {
    backgroundColor: '#9ca3af',
    opacity: 0.7,
  },
  sendIcon: {
    color: '#ffffff',
  },
});
