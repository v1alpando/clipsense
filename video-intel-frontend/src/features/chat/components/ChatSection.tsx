import { Loader2, Send, XCircle } from 'lucide-react';
import { type FormEvent, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { useChat } from '../hooks/useChat';

interface ChatSectionProps {
  videoId: string;
  enabled: boolean;
}

export function ChatSection({ videoId, enabled }: ChatSectionProps) {
  const { state, send, isSending, sendError } = useChat(videoId, enabled);
  const [draft, setDraft] = useState('');

  if (!enabled) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Chat</CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Chat about this video will be available once it&apos;s uploaded.
        </CardContent>
      </Card>
    );
  }

  if (state.status === 'idle' || state.status === 'loading') {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Chat</CardTitle>
        </CardHeader>
        <CardContent className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          Loading chat...
        </CardContent>
      </Card>
    );
  }

  if (state.status === 'error') {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Chat</CardTitle>
        </CardHeader>
        <CardContent className="flex items-start gap-2 text-sm">
          <XCircle className="size-4 shrink-0 text-destructive" />
          <p>{state.message}</p>
        </CardContent>
      </Card>
    );
  }

  const { messages } = state;

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const content = draft.trim();
    if (!content || isSending) return;
    setDraft('');
    send(content);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Chat</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="flex max-h-80 flex-col gap-3 overflow-y-auto">
          {messages.length === 0 && (
            <p className="text-sm text-muted-foreground">Ask something about this video.</p>
          )}
          {messages.map((message) => (
            <div
              key={message.id}
              className={`max-w-[80%] rounded-lg px-3 py-2 text-sm ${
                message.role === 'USER'
                  ? 'self-end bg-primary text-primary-foreground'
                  : 'self-start bg-muted'
              }`}
            >
              {message.content}
            </div>
          ))}
        </div>

        {sendError && <p className="text-sm text-destructive">{sendError}</p>}

        <form onSubmit={handleSubmit} className="flex gap-2">
          <Input
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="Ask about this video..."
            disabled={isSending}
          />
          <Button type="submit" size="icon" disabled={isSending || !draft.trim()}>
            {isSending ? (
              <Loader2 className="size-4 animate-spin" />
            ) : (
              <Send className="size-4" />
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
