import { useColorScheme } from 'react-native';

const dark = {
  bg: '#000000',
  surface: '#1C1C1E',
  raised: '#2C2C2E',
  text: '#F5F5F7',
  secondary: '#8E8E93',
  separator: 'rgba(84,84,88,0.45)',
  accent: '#0A84FF',
  green: '#30D158',
  orange: '#FF9F0A',
  red: '#FF453A',
  bar: 'rgba(28,28,30,0.92)',
};
const light: typeof dark = {
  bg: '#F2F2F7',
  surface: '#FFFFFF',
  raised: '#E5E5EA',
  text: '#1C1C1E',
  secondary: '#6C6C70',
  separator: 'rgba(60,60,67,0.18)',
  accent: '#007AFF',
  green: '#34C759',
  orange: '#FF9500',
  red: '#FF3B30',
  bar: 'rgba(255,255,255,0.94)',
};

export type Colors = typeof dark;
export const useColors = (): Colors => (useColorScheme() === 'light' ? light : dark);

export const radius = { card: 20, row: 14, pill: 999 };
export const space = { xs: 4, s: 8, m: 16, l: 24, xl: 32 };

// Tracking tightens as type grows; body stays near zero.
export const type = {
  hero: { fontSize: 44, fontWeight: '800', letterSpacing: -1.2, lineHeight: 48 },
  title: { fontSize: 28, fontWeight: '700', letterSpacing: -0.6, lineHeight: 34 },
  headline: { fontSize: 17, fontWeight: '600', letterSpacing: -0.2 },
  body: { fontSize: 16, fontWeight: '400', lineHeight: 22 },
  footnote: { fontSize: 13, fontWeight: '400', letterSpacing: 0.1, lineHeight: 18 },
  caption: { fontSize: 11, fontWeight: '600', letterSpacing: 0.4 },
} as const;

// Critically damped by default (no overshoot): Apple's response ≈ 0.3 s.
export const spring = { damping: 26, stiffness: 320, mass: 1, useNativeDriver: true } as const;
