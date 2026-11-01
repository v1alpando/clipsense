/**
 * Formats a time in seconds into MM:SS or HH:MM:SS format.
 * Examples:
 *   formatTimestamp(84) => "01:24"
 *   formatTimestamp(84, { brackets: true }) => "[01:24]"
 *   formatTimestamp(3665) => "01:01:05"
 *   formatTimestamp(3665, { brackets: true }) => "[01:01:05]"
 */
export function formatTimestamp(
  seconds: number,
  options: { brackets?: boolean } = {},
): string {
  if (typeof seconds !== 'number' || isNaN(seconds) || seconds < 0) {
    return options.brackets ? '[00:00]' : '00:00';
  }

  const totalSeconds = Math.floor(seconds);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const remainingSeconds = totalSeconds % 60;

  const pad = (n: number) => n.toString().padStart(2, '0');

  const formatted =
    hours > 0
      ? `${pad(hours)}:${pad(minutes)}:${pad(remainingSeconds)}`
      : `${pad(minutes)}:${pad(remainingSeconds)}`;

  return options.brackets ? `[${formatted}]` : formatted;
}

/**
 * Formats a segment duration in seconds into a friendly string.
 * Examples:
 *   formatDuration(4.5) => "4.5s"
 *   formatDuration(12) => "12s"
 *   formatDuration(84) => "1m 24s"
 */
export function formatDuration(seconds: number): string {
  if (typeof seconds !== 'number' || isNaN(seconds) || seconds <= 0) {
    return '0s';
  }

  if (seconds < 60) {
    const rounded = Math.round(seconds * 10) / 10;
    return `${rounded}s`;
  }

  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.round(seconds % 60);
  return remainingSeconds > 0 ? `${minutes}m ${remainingSeconds}s` : `${minutes}m`;
}
