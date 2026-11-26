import { StyleSheet, Text, View } from 'react-native';
import { CameraPreview } from '../../modules/dumscrl-engine';
import type { EngineApi } from '../engine';
import { Metal } from '../Metal';
import { Press } from '../Press';
import { signLabel } from '../signs';
import { Page } from '../ui';
import { radius, space, type, useColors } from '../theme';

/** What the camera sees, with the tracked hand drawn on top and the sign it reads. */
export function Camera({ engine, visible }: { engine: EngineApi; visible: boolean }) {
  const c = useColors();
  const { state, start } = engine;
  const seen = signLabel(state.sign);
  return (
    <Page title="Camera" subtitle="Keep your hand in the middle, 30–50 cm from the phone.">
      <View style={[styles.frame, { backgroundColor: c.surface }]}>
        {state.running && visible ? (
          <CameraPreview style={StyleSheet.absoluteFill} />
        ) : (
          <View style={styles.empty}>
            <Text style={[type.body, { color: c.secondary, textAlign: 'center' }]}>The camera is off.</Text>
            <Press onPress={start}>
              <Metal style={styles.button}>
                <Text style={[type.headline, { color: c.onMetal }]}>Start</Text>
              </Metal>
            </Press>
          </View>
        )}
        {state.running && (
          <View style={[styles.badge, { backgroundColor: c.scrim }]}>
            <Text style={[type.headline, { color: '#fff' }]}>
              {state.paused ? 'Paused' : seen ? `${seen.emoji}  ${seen.name}` : 'No hand'}
            </Text>
            {seen?.does && !state.paused ? <Text style={[type.footnote, { color: 'rgba(255,255,255,0.75)' }]}>{seen.does}</Text> : null}
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
