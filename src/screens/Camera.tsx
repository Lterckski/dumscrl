import * as Haptics from 'expo-haptics';
import { useEffect } from 'react';
import { AppState, PermissionsAndroid, StyleSheet, Text, View } from 'react-native';
import { CameraPreview, Engine } from '../../modules/dumscrl-engine';
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
              {state.status.startsWith('Test: ') ? `Would do: ${state.status.slice(6)}` : seen?.does ? `Does: ${seen.does}` : 'Practice mode'}
            </Text>
          </View>
        )}
      </View>
    </Page>
  );
}

const styles = StyleSheet.create({
  frame: { aspectRatio: 3 / 4, borderRadius: 28, overflow: 'hidden' },
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: space.m, padding: space.l },
  button: { paddingHorizontal: space.l, paddingVertical: 12, borderRadius: radius.pill, overflow: 'hidden' },
  badge: { position: 'absolute', left: space.m, right: space.m, bottom: space.m, borderRadius: radius.row, paddingHorizontal: space.m, paddingVertical: 10, gap: 2 },
});
