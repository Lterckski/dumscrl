/**
 * The hand signs the engine knows, in the order people learn them. inFlicks: still a sign in Flicks mode;
 * the others are done by flicking there (the engine's signAllowed in HandPose.kt).
 */
export const SIGNS = [
  { key: 'point', emoji: '☝️', name: 'Index finger up', does: 'Next video', inFlicks: false },
  { key: 'peace', emoji: '✌️', name: 'Peace, thumb tucked', does: 'Previous video', inFlicks: false },
  { key: 'thumbs up', emoji: '👍', name: 'Thumbs up', does: 'Like', inFlicks: true },
  { key: 'thumb left', emoji: '👈', name: 'Thumb out to your left', does: 'Next photo', inFlicks: false },
  { key: 'pinky', emoji: '🤙', name: 'Pinky out sideways', does: 'Previous photo', inFlicks: false },
  { key: 'three', emoji: '🤟', name: 'Any three fingers', does: 'Next app', inFlicks: true },
  { key: 'four', emoji: '🖖', name: 'Any four fingers', does: 'Previous app', inFlicks: true },
  { key: 'open palm', emoji: '✋', name: 'Open palm, hold 2 s', does: 'Pause or resume', inFlicks: true },
] as const;

/** Flicks mode: which fingers you flick decides the video action, not the direction. The thumb doesn't matter. */
export const FLICKS = [
  { key: 'index', emoji: '☝️', name: 'Index, flick up or down', does: 'Next video' },
  { key: 'two', emoji: '✌️', name: 'Index + middle, flick up or down', does: 'Previous video' },
  { key: 'left', emoji: '👈', name: 'Index, flick to your left', does: 'Next photo' },
  { key: 'right', emoji: '👉', name: 'Index, flick to your right', does: 'Previous photo' },
] as const;

// Shapes the engine reports that don't do anything, so they stay out of the Signs guide.
const OTHERS: Record<string, { emoji: string; name: string }> = {
  fist: { emoji: '✊', name: 'Fist' },
  'thumb right': { emoji: '👉', name: 'Thumb out to your right' },
  other: { emoji: '🤚', name: 'Hand' },
};

export const signLabel = (sign: string) =>
  SIGNS.find((s) => s.key === sign) ??
  (sign === 'no hand' ? null : { key: sign, does: '', inFlicks: true, ...(OTHERS[sign] ?? { emoji: '🤚', name: sign }) });
