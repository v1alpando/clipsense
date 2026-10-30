import { useCallback, useEffect, useState } from 'react';
import { getOrCreateAnonymousOwner } from '@/features/user/anonymousOwner';
import { getMessages } from '../api/getMessages';
import { sendMessage } from '../api/sendMessage';
import { getOrCreateChatSessionId } from '../chatSessionCache';
import type { ChatMessage } from '../types/chatMessage';

type ChatState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ready'; sessionId: string; messages: ChatMessage[] }
  | { status: 'error'; message: string };

export function useChat(videoId: string, enabled: boolean) {
  const [state, setState] = useState<ChatState>({ status: 'idle' });
  const [isSending, setIsSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);

  const init = useCallback(async () => {
    setState({ status: 'loading' });
    try {
      const owner = await getOrCreateAnonymousOwner();
      const sessionId = await getOrCreateChatSessionId(videoId, owner);
      const messages = await getMessages(sessionId);
      setState({ status: 'ready', sessionId, messages });
    } catch (error) {
      setState({
        status: 'error',
        message: error instanceof Error ? error.message : 'Failed to load chat.',
      });
    }
  }, [videoId]);

  useEffect(() => {
    if (!enabled) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    init();
  }, [enabled, init]);

  const send = useCallback(
    async (content: string) => {
      if (state.status !== 'ready') return;
      const { sessionId } = state;
      setSendError(null);

      const optimisticMessage: ChatMessage = {
        id: crypto.randomUUID(),
        sessionId,
        role: 'USER',
        content,
        createdAt: new Date().toISOString(),
      };
      setState({ status: 'ready', sessionId, messages: [...state.messages, optimisticMessage] });
      setIsSending(true);

      try {
        const reply = await sendMessage(sessionId, content);
        setState((current) =>
          current.status === 'ready'
            ? { ...current, messages: [...current.messages, reply] }
            : current,
        );
      } catch (error) {
        // Roll back the optimistic message but keep the rest of the
        // conversation — a failed send shouldn't wipe out chat history.
        setState((current) =>
          current.status === 'ready'
            ? {
                ...current,
                messages: current.messages.filter((message) => message.id !== optimisticMessage.id),
              }
            : current,
        );
        setSendError(error instanceof Error ? error.message : 'Failed to send message.');
      } finally {
        setIsSending(false);
      }
    },
    [state],
  );

  return { state, send, isSending, sendError };
}
