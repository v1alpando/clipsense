import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckCircle2, Loader2, UploadCloud, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { formatBytes } from '@/lib/formatBytes';
import { useVideoUpload } from '../hooks/useVideoUpload';

const ACCEPTED_FORMATS_LABEL = 'MP4, MOV, WebM, MKV';

export function VideoUpload() {
  const { state, selectFile, startUpload, reset } = useVideoUpload();
  const [isDragActive, setIsDragActive] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const openFilePicker = () => inputRef.current?.click();

  const handleFiles = (files: FileList | null) => {
    const file = files?.[0];
    if (file) selectFile(file);
  };

  if (state.status === 'uploaded') {
    return (
      <div className="flex w-full max-w-md flex-col items-center gap-4 rounded-lg border p-8 text-center">
        <CheckCircle2 className="size-10 text-primary" />
        <div>
          <p className="font-medium">Uploaded {state.file.name}</p>
          <p className="text-sm text-muted-foreground">
            {formatBytes(state.file.size)}
          </p>
        </div>
        <div className="flex gap-2">
          <Button asChild>
            <Link to={`/videos/${state.videoId}`}>View video</Link>
          </Button>
          <Button variant="outline" onClick={reset}>
            Upload another
          </Button>
        </div>
      </div>
    );
  }

  if (state.status === 'uploading') {
    return (
      <div className="flex w-full max-w-md flex-col items-center gap-3 rounded-lg border p-8 text-center">
        <Loader2 className="size-8 animate-spin text-muted-foreground" />
        <p className="text-sm">Uploading {state.file.name}...</p>
      </div>
    );
  }

  return (
    <div className="flex w-full flex-col items-center gap-4">
      <div
        role="button"
        tabIndex={0}
        onClick={openFilePicker}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') openFilePicker();
        }}
        onDragOver={(event) => {
          event.preventDefault();
          setIsDragActive(true);
        }}
        onDragLeave={() => setIsDragActive(false)}
        onDrop={(event) => {
          event.preventDefault();
          setIsDragActive(false);
          handleFiles(event.dataTransfer.files);
        }}
        className={`flex w-full max-w-md cursor-pointer flex-col items-center gap-3 rounded-lg border-2 border-dashed p-10 text-center transition-colors ${
          isDragActive ? 'border-primary bg-accent' : 'border-border'
        }`}
      >
        <UploadCloud className="size-8 text-muted-foreground" />
        <div>
          <p className="font-medium">Upload video</p>
          <p className="text-sm text-muted-foreground">
            Drag and drop, or click to browse
          </p>
        </div>
        <p className="text-xs text-muted-foreground">
          Supported formats: {ACCEPTED_FORMATS_LABEL}
        </p>
      </div>

      <input
        ref={inputRef}
        type="file"
        accept="video/*"
        className="hidden"
        onChange={(event) => handleFiles(event.target.files)}
      />

      {state.status === 'selecting' && (
        <div className="flex w-full max-w-md items-center justify-between rounded-lg border p-4">
          <div className="min-w-0">
            <p className="truncate text-sm font-medium">{state.file.name}</p>
            <p className="text-xs text-muted-foreground">
              {formatBytes(state.file.size)}
            </p>
          </div>
          <Button onClick={startUpload}>Upload</Button>
        </div>
      )}

      {state.status === 'error' && (
        <div className="flex w-full max-w-md items-start gap-2 rounded-lg border border-destructive/50 bg-destructive/5 p-4 text-sm">
          <XCircle className="size-4 shrink-0 text-destructive" />
          <p>{state.message}</p>
        </div>
      )}
    </div>
  );
}
