import { useCallback, useState } from 'react';
import { getOrCreateAnonymousOwner } from '@/features/user/anonymousOwner';
import { completeUpload } from '../api/completeUpload';
import { createVideo } from '../api/createVideo';
import { getUploadUrl } from '../api/getUploadUrl';
import { uploadFileToUrl } from '../api/uploadFileToUrl';
import type { VideoUploadState } from '../types/upload';

const ACCEPTED_MIME_PREFIX = 'video/';
// No confirmed backend limit yet — this is a client-side sanity cap only, to
// fail fast on an obviously wrong file instead of starting a doomed upload.
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 * 1024;

function validateFile(file: File): string | null {
  if (!file.type.startsWith(ACCEPTED_MIME_PREFIX)) {
    return 'Unsupported file type. Please select a video file.';
  }
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return 'File is too large.';
  }
  return null;
}

function titleFromFilename(filename: string): string {
  const dotIndex = filename.lastIndexOf('.');
  return dotIndex > 0 ? filename.slice(0, dotIndex) : filename;
}

export function useVideoUpload() {
  const [state, setState] = useState<VideoUploadState>({ status: 'idle' });

  const selectFile = useCallback((file: File) => {
    const validationError = validateFile(file);
    setState(
      validationError
        ? { status: 'error', message: validationError, file }
        : { status: 'selecting', file },
    );
  }, []);

  const startUpload = useCallback(async () => {
    if (state.status !== 'selecting') return;
    const { file } = state;
    setState({ status: 'uploading', file });

    try {
      const owner = await getOrCreateAnonymousOwner();
      const video = await createVideo(owner, titleFromFilename(file.name));
      const { uploadUrl } = await getUploadUrl(video.id);
      await uploadFileToUrl(uploadUrl, file);
      const completed = await completeUpload(video.id);

      if (completed.status === 'FAILED') {
        setState({
          status: 'error',
          file,
          message: completed.failedReason ?? 'Upload failed.',
        });
        return;
      }

      setState({ status: 'uploaded', file, videoId: completed.id });
    } catch (error) {
      setState({
        status: 'error',
        file,
        message: error instanceof Error ? error.message : 'Upload failed.',
      });
    }
  }, [state]);

  const reset = useCallback(() => setState({ status: 'idle' }), []);

  return { state, selectFile, startUpload, reset };
}
