/** The hand signs the engine knows, in the order people learn them. */
export const SIGNS = [
  { key: 'point', emoji: '☝️', name: 'Index finger up', does: 'Next video' },
  { key: 'peace', emoji: '✌️', name: 'Peace', does: 'Previous video' },
  { key: 'thumbs up', emoji: '👍', name: 'Thumbs up', does: 'Like' },
  { key: 'thumb left', emoji: '👈', name: 'Thumb out to your left', does: 'Next photo' },
  { key: 'pinky', emoji: '🤙', name: 'Pinky out sideways', does: 'Previous photo' },
  { key: 'three', emoji: '🤟', name: 'Three fingers', does: 'Next app' },
  { key: 'four', emoji: '🖖', name: 'Four fingers, thumb tucked', does: 'Previous app' },
  { key: 'open palm', emoji: '✋', name: 'Open palm, hold 2 s', does: 'Pause or resume' },
] as const;

// Shapes the engine reports that don't do anything, so they stay out of the Signs guide.
const OTHERS: Record<string, { emoji: string; name: string }> = {
  fist: { emoji: '✊', name: 'Fist' },
  'thumb right': { emoji: '👉', name: 'Thumb out to your right' },
  other: { emoji: '🤚', name: 'Hand' },
};

export const signLabel = (sign: string) =>
  SIGNS.find((s) => s.key === sign) ??
  (sign === 'no hand' ? null : { key: sign, does: '', ...(OTHERS[sign] ?? { emoji: '🤚', name: sign }) });
