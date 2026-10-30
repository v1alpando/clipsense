const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL;

if (!rawApiBaseUrl) {
  throw new Error(
    'Missing VITE_API_BASE_URL. Copy .env.example to .env and set it to your backend URL.',
  );
}

export const env = {
  apiBaseUrl: rawApiBaseUrl as string,
};
