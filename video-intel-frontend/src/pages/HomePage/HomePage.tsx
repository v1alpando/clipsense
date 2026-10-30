import { VideoUpload } from '@/features/video/components/VideoUpload';

export function HomePage() {
  return (
    <div className="flex flex-col items-center gap-8 py-16">
      <div className="flex flex-col items-center gap-2 text-center">
        <h1 className="text-3xl font-semibold tracking-tight">Video Intel</h1>
        <p className="max-w-md text-sm text-muted-foreground">
          Upload a video and analyze it with AI.
        </p>
      </div>
      <VideoUpload />
    </div>
  );
}
