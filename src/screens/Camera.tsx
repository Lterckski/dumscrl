import * as Haptics from 'expo-haptics';
import Ionicons from '@expo/vector-icons/Ionicons';
import { useEffect, useRef, useState } from 'react';
import { Animated, AppState, PermissionsAndroid, StyleSheet, Text, View } from 'react-native';
import { CameraPreview, Engine, type FlickMeter } from '../../modules/dumscrl-engine';
import type { EngineApi } from '../engine';
import { Metal } from '../Metal';
import { Press } from '../Press';
import { signLabel } from '../signs';
import { Page } from '../ui';
import { radius, space, type, useColors } from '../theme';

/** Practice: what the camera sees, the tracked hand and the sign it reads. Nothing is performed on the phone here. */
export function Camera({ engine }: { engine: EngineApi }) {
  const c = useColors();
  const { state, stop } = engine;
  const seen = signLabel(state.sign);
  const flicks = state.mode === 'flicks';

  // While this tab is open, signs never act on the phone. If Start on Home is off, the camera runs only while
  // this tab is open and in front.
  useEffect(() => {
    let ownCamera = false;
    let left = false;
    const open = async () => {
      if (AppState.currentState !== 'active') return;
      Engine.setPractice(true);
      // Only with the camera already allowed: asking from here flips the app to background and back, which restarts this.
      if (Engine.isRunning() || !(await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.CAMERA)) || left) return;
      ownCamera = true;
      Engine.start();
    };
    // In another app, Start on Home works as normal again.
    const close = () => {
      Engine.setPractice(false);
      if (ownCamera) stop();
      ownCamera = false;
    };
    open();
    const sub = AppState.addEventListener('change', (a) => (a === 'active' ? open() : close()));
    return () => {
      left = true;
      sub.remove();
      close();
    };
  }, [stop]);

  // A tick when a sign would have fired. shortcut: the same sign twice in a row ticks once; count events if that matters.
  useEffect(() => {
    if (state.status.startsWith('Test: ')) Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  }, [state.status]);

  return (
    <Page title="Camera" subtitle="Practice your signs here. Nothing happens on your phone while this tab is open.">
      <View style={[styles.frame, { backgroundColor: c.surface }]}>
        {state.running ? (
          <CameraPreview style={StyleSheet.absoluteFill} />
        ) : (
          <View style={styles.empty}>
            <Text style={[type.body, { color: c.secondary, textAlign: 'center' }]}>
              {state.camera ? 'Starting the camera…' : 'dumscrl needs the camera to see your hand.'}
            </Text>
            {/* The dialog sends the app to the background and back, and coming back starts the camera above. */}
            {!state.camera && (
              <Press onPress={() => PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.CAMERA)}>
                <Metal style={styles.button}>
                  <Text style={[type.headline, { color: c.onMetal }]}>Allow camera</Text>
                </Metal>
              </Press>
            )}
          </View>
        )}
        {state.running && (
          <View style={[styles.badge, { backgroundColor: c.scrim }]}>
            <Text style={[type.headline, { color: '#fff' }]}>{seen ? `${seen.emoji}  ${seen.name}` : 'No hand'}</Text>
            <Text style={[type.footnote, { color: 'rgba(255,255,255,0.75)' }]}>
              {state.status.startsWith('Test: ')
                ? `Would do: ${state.status.slice(6)}`
                : flicks && state.sign === 'point'
                  ? 'Flick up or down to scroll, left or right for photos'
                  : seen?.does ? `Does: ${seen.does}` : 'Practice mode'}
            </Text>
            {flicks && <FlickReadout />}
          </View>
        )}
        {state.running && flicks && <FlickArrow />}
      </View>
    </Page>
  );
}

/** Latest flick meter from the engine (flicks mode only, ~10 a second while the preview shows). */
function useFlickMeter() {
  const [m, setM] = useState<FlickMeter | null>(null);
  useEffect(() => {
    const sub = Engine.addListener('onFlickMeter', setM);
    return () => sub.remove();
  }, []);
  return m;
}

const ARROWS = { up: 'arrow-up', down: 'arrow-down', left: 'arrow-back', right: 'arrow-forward' } as const;
// The engine names the action; the preview is mirrored like a selfie, so these are the directions the user sees.
const flickDirection = (action: string): keyof typeof ARROWS =>
  action === 'Next video' ? 'up' : action === 'Previous video' ? 'down' : action.startsWith('Next photo') ? 'left' : 'right';

/** Fingertip speed against the flick threshold, and the wait left before the next flick counts. */
function FlickReadout() {
  const m = useFlickMeter();
  if (!m) return null;
  const speed = Math.min(m.speed / m.threshold, 1);
  const wait = m.cooldown > 0 ? m.cooldownLeft / m.cooldown : 0;
  return (
    <View style={styles.meters}>
      <Meter label={`Speed ${m.speed.toFixed(1)} / ${m.threshold.toFixed(0)}`} fill={speed} />
      <Meter label={wait > 0 ? `Wait ${(m.cooldownLeft / 1000).toFixed(1)} s` : 'Ready'} fill={wait} />
    </View>
  );
}

function Meter({ label, fill }: { label: string; fill: number }) {
  return (
    <View style={{ flex: 1, gap: 4 }}>
      <Text style={[type.caption, { color: 'rgba(255,255,255,0.75)', fontVariant: ['tabular-nums'] }]}>{label}</Text>
      <View style={styles.track}>
        <View style={[styles.fill, { width: `${Math.round(fill * 100)}%` }]} />
      </View>
    </View>
  );
}

/** A big arrow that glides the way the finger flicked and fades. */
function FlickArrow() {
  const m = useFlickMeter();
  const [dir, setDir] = useState<keyof typeof ARROWS | null>(null);
  const shown = useRef(-1);
  const t = useRef(new Animated.Value(1)).current;
  useEffect(() => {
    if (!m?.last || m.at === shown.current) return;
    const first = shown.current < 0;
    shown.current = m.at;
    if (first) return; // a flick from before this tab opened
    setDir(flickDirection(m.last));
    t.setValue(0);
    Animated.timing(t, { toValue: 1, duration: 600, useNativeDriver: true }).start();
  }, [m, t]);
  if (!dir) return null;
  const d = 40;
  const [x, y] = { up: [0, -d], down: [0, d], left: [-d, 0], right: [d, 0] }[dir];
  return (
    <Animated.View
      pointerEvents="none"
      style={[styles.arrow, {
        opacity: t.interpolate({ inputRange: [0, 0.2, 1], outputRange: [0, 1, 0] }),
        transform: [{ translateX: t.interpolate({ inputRange: [0, 1], outputRange: [0, x] }) }, { translateY: t.interpolate({ inputRange: [0, 1], outputRange: [0, y] }) }],
      }]}
    >
      <Ionicons name={ARROWS[dir]} size={72} color="#fff" />
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  meters: { flexDirection: 'row', gap: space.m, marginTop: space.xs },
  track: { height: 4, borderRadius: 2, backgroundColor: 'rgba(255,255,255,0.2)', overflow: 'hidden' },
  fill: { height: 4, borderRadius: 2, backgroundColor: '#fff' },
  arrow: { position: 'absolute', top: 0, bottom: 0, left: 0, right: 0, alignItems: 'center', justifyContent: 'center' },
  frame: { aspectRatio: 3 / 4, borderRadius: 28, overflow: 'hidden' },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: space.m, padding: space.l },
  button: { paddingHorizontal: space.l, paddingVertical: 12, borderRadius: radius.pill, overflow: 'hidden' },
  badge: { position: 'absolute', left: space.m, right: space.m, bottom: space.m, borderRadius: radius.row, paddingHorizontal: space.m, paddingVertical: 10, gap: 2 },
});
