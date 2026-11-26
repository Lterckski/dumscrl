import { Appearance, useColorScheme } from 'react-native';
import { Engine, type ThemePref } from '../modules/dumscrl-engine';

// Everything is metal: a gunmetal backdrop with chrome accents in dark mode,
// brushed aluminium with gunmetal accents in light mode. No colour.
const dark = {
  backdrop: ['#2B2C31', '#17181B', '#25262A', '#111215'] as readonly [string, string, string, string],
  disc: '#1C1D21', // solid fill inside the start button's ring
  surface: 'rgba(255,255,255,0.06)',
  raised: 'rgba(255,255,255,0.10)',
  text: '#F4F4F6',
  secondary: '#9A9BA1',
  separator: 'rgba(255,255,255,0.10)',
  accent: '#D8D8DE', // solid metal for icons and text
  onMetal: '#0A0A0B',
  metal: ['#FAFAFC', '#A4A4AB', '#E9E9EE', '#7D7D84'] as readonly [string, string, string, string], // chrome
  sheen: 'rgba(255,255,255,0.35)',
  bar: 'rgba(32,33,37,0.96)',
  scrim: 'rgba(0,0,0,0.55)',
};
const light: typeof dark = {
  backdrop: ['#EDEDF0', '#D3D4D8', '#E8E8EB', '#C6C7CC'],
  disc: '#E2E3E6',
  surface: 'rgba(255,255,255,0.55)',
  raised: 'rgba(255,255,255,0.8)',
  text: '#111114',
  secondary: '#5E5E64',
  separator: 'rgba(0,0,0,0.08)',
  accent: '#3A3A3D',
  onMetal: '#FFFFFF',
  metal: ['#6B6B71', '#1F1F22', '#5C5C62', '#2A2A2E'] as const, // gunmetal
  sheen: 'rgba(255,255,255,0.18)',
  bar: 'rgba(238,238,241,0.96)',
  scrim: 'rgba(0,0,0,0.55)',
};

export type Colors = typeof dark;
export const useColors = (): Colors => (useColorScheme() === 'light' ? light : dark);

/** System, light or dark; remembered on the phone. */
export const applyTheme = (pref: ThemePref) => {
  Appearance.setColorScheme(pref === 'system' ? 'unspecified' : pref);
  Engine.setTheme(pref);
};
export const savedTheme = (): ThemePref => Engine.getTheme();

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
