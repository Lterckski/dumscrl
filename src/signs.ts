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

export const signLabel = (sign: string) =>
  SIGNS.find((s) => s.key === sign) ?? (sign === 'no hand' ? null : { key: sign, emoji: '🤚', name: sign, does: '' });
