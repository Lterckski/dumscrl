import { useCallback, useEffect, useState } from 'react';
import { AppState, PermissionsAndroid, Platform } from 'react-native';
import { Engine, type Mode } from '../modules/dumscrl-engine';

export type EngineState = {
  running: boolean;
  paused: boolean;
  status: string;
  sign: string;
  mode: Mode;
  phoneControl: boolean;
  camera: boolean;
};

const read = (): Omit<EngineState, 'sign' | 'camera'> => ({
  running: Engine.isRunning(),
  paused: Engine.isPaused(),
  status: Engine.status(),
  mode: Engine.getMode(),
  phoneControl: Engine.isAccessibilityEnabled(),
});

const hasCamera = () => PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.CAMERA);

/** Live engine state; refreshed on events and whenever the app comes back to the front (e.g. from Settings). */
export function useEngine() {
  const [state, setState] = useState<EngineState>(() => ({ ...read(), sign: 'no hand', camera: false }));
  const refresh = useCallback(async () => {
    const camera = await hasCamera();
    setState((s) => ({ ...s, ...read(), camera }));
  }, []);

  useEffect(() => {
    refresh();
    const subs = [
      Engine.addListener('onStatus', ({ label }) => setState((s) => ({ ...s, ...read(), status: label }))),
      Engine.addListener('onSign', ({ sign, paused }) => setState((s) => (s.sign === sign && s.paused === paused ? s : { ...s, sign, paused }))),
      AppState.addEventListener('change', (a) => a === 'active' && refresh()),
    ];
    return () => subs.forEach((s) => s.remove());
  }, [refresh]);

  const start = useCallback(async () => {
    const cam = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.CAMERA);
    if (Platform.OS === 'android' && Platform.Version >= 33) {
      await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS); // optional: shows the last sign
    }
    if (cam === PermissionsAndroid.RESULTS.GRANTED) Engine.start();
    refresh();
  }, [refresh]);

  const stop = useCallback(() => {
    Engine.stop();
    setState((s) => ({ ...s, running: false, sign: 'no hand' }));
  }, []);

  const setMode = useCallback((mode: Mode) => {
    Engine.setMode(mode);
    setState((s) => ({ ...s, mode }));
  }, []);

  const setPaused = useCallback(async (paused: boolean) => {
    await Engine.setPaused(paused);
    setState((s) => ({ ...s, paused }));
  }, []);

  return { state, start, stop, setMode, setPaused, openPhoneControl: () => Engine.openAccessibilitySettings() } // not passed straight to onPress: the press event would be an extra argument;
}

export type EngineApi = ReturnType<typeof useEngine>;
