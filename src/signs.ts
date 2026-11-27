/**
 * The hand signs the engine knows, in the order people learn them. gesture: the engine's name for it, used to
 * switch it off. inFlicks: still a sign in Flicks mode;
 * the others are done by flicking there (the engine's signAllowed in HandPose.kt).
 */
export const SIGNS = [
  { key: 'point', gesture: 'SWIPE_UP', emoji: '☝️', name: 'Index finger up', does: 'Next video', inFlicks: false },
  { key: 'peace', gesture: 'SWIPE_DOWN', emoji: '✌️', name: 'Peace, thumb tucked', does: 'Previous video', inFlicks: false },
  { key: 'thumbs up', gesture: 'THUMBS_UP', emoji: '👍', name: 'Thumbs up', does: 'Like', inFlicks: true },
  { key: 'thumb left', gesture: 'PHOTO_NEXT', emoji: '👈', name: 'Thumb out to your left', does: 'Next photo', inFlicks: false },
  { key: 'pinky', gesture: 'PHOTO_PREV', emoji: '🤙', name: 'Pinky out sideways', does: 'Previous photo', inFlicks: false },
  { key: 'three', gesture: 'SWIPE_LEFT', emoji: '🤟', name: 'Any three fingers', does: 'Next app', inFlicks: true },
  { key: 'four', gesture: 'SWIPE_RIGHT', emoji: '🖖', name: 'Any four fingers', does: 'Previous app', inFlicks: true },
  { key: 'open palm', gesture: 'TOGGLE', emoji: '✋', name: 'Open palm, hold 2 s', does: 'Pause or resume', inFlicks: true },
] as const;

/**
 * Flicks mode: the hand's direction picks videos (fingers up, flick up/down) or photos (hand sideways, flick
 * sideways); the fingers pick next or previous. Flick direction and thumb don't matter.
 */
export const FLICKS = [
  { key: 'index', emoji: '☝️', name: 'Index up, flick', does: 'Next video' },
  { key: 'two', emoji: '✌️', name: 'Index + middle up, flick', does: 'Previous video' },
  { key: 'index side', emoji: '👉', name: 'Index sideways, flick', does: 'Next photo' },
  { key: 'two side', emoji: '🫱', name: 'Index + middle sideways, flick', does: 'Previous photo' },
] as const;

// Shapes the engine reports that don't do anything, so they stay out of the Signs guide.
const OTHERS: Record<string, { emoji: string; name: string }> = {
  fist: { emoji: '✊', name: 'Fist' },
  'thumb right': { emoji: '👉', name: 'Thumb out to your right' },
  other: { emoji: '🤚', name: 'Hand' },
};

export const signLabel = (sign: string) =>
  SIGNS.find((s) => s.key === sign) ??
  (sign === 'no hand' ? null : { key: sign, gesture: '', does: '', inFlicks: true, ...(OTHERS[sign] ?? { emoji: '🤚', name: sign }) });
