// Presigned MinIO URL — goes straight to storage, not through our API base URL.
export async function uploadFileToUrl(uploadUrl: string, file: File): Promise<void> {
  const response = await fetch(uploadUrl, { method: 'PUT', body: file });
  if (!response.ok) {
    throw new Error(`Upload to storage failed with status ${response.status}`);
  }
}
