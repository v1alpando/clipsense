export type VideoUploadState =
  | { status: 'idle' }
  | { status: 'selecting'; file: File }
  | { status: 'uploading'; file: File }
  | { status: 'uploaded'; file: File; videoId: string }
  | { status: 'error'; message: string; file?: File };
