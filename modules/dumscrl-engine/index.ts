import { requireNativeModule, requireNativeView, type EventSubscription } from 'expo';
import type { ViewProps } from 'react-native';

export type Mode = 'signs' | 'flicks' | 'head' | 'signs+head';
export type ThemePref = 'system' | 'light' | 'dark';

interface DumscrlEngineNative {
  start(): void;
  /** While on, signs are read and shown but nothing happens on the phone. */
  setPractice(on: boolean): void;
  stop(): void;
  isRunning(): boolean;
  status(): string;
  getMode(): Mode;
  setMode(mode: Mode): void;
  isPaused(): boolean;
  setPaused(paused: boolean): Promise<void>;
  isAccessibilityEnabled(): boolean;
  openAccessibilitySettings(): void;
  getTheme(): ThemePref;
  setTheme(theme: ThemePref): void;
  addListener(event: 'onStatus', fn: (e: { label: string }) => void): EventSubscription;
  addListener(event: 'onSign', fn: (e: { sign: string; paused: boolean }) => void): EventSubscription;
}

export const Engine = requireNativeModule<DumscrlEngineNative>('DumscrlEngine');

/** Live front camera with the tracked hand drawn on top. */
export const CameraPreview = requireNativeView<ViewProps>('DumscrlEngine');
