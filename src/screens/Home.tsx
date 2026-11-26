import Ionicons from '@expo/vector-icons/Ionicons';
import { useEffect, useRef } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';
import type { EngineApi } from '../engine';
import { Press } from '../Press';
import { signLabel } from '../signs';
import { Card, Page, Row, SectionLabel } from '../ui';
import { radius, space, spring, type, useColors } from '../theme';

/** Landing: what dumscrl is, one big start button, and what's left to set up. */
export function Home({ engine }: { engine: EngineApi }) {
  const c = useColors();
  const { state, start, stop, setPaused, openPhoneControl } = engine;
  const on = state.running;
  const seen = signLabel(state.sign);

  // The button's colour fill grows from the centre when it turns on.
  const fill = useRef(new Animated.Value(on ? 1 : 0)).current;
  useEffect(() => Animated.spring(fill, { toValue: on ? 1 : 0, ...spring }).start(), [on, fill]);

  return (
    <Page title="dumscrl" subtitle="Scroll, like and flip photos with a hand sign. No touching the screen.">
      <View style={styles.center}>
        <Press
          onPress={on ? stop : start}
          scaleTo={0.94}
          style={[styles.big, { borderColor: on ? c.green : c.accent }]}
          accessibilityRole="button"
          accessibilityLabel={on ? 'Stop dumscrl' : 'Start dumscrl'}
        >
          <Animated.View style={[StyleSheet.absoluteFill, styles.bigFill, { backgroundColor: c.green, opacity: fill, transform: [{ scale: fill }] }]} />
          <Ionicons name={on ? 'stop' : 'play'} size={44} color={on ? '#000' : c.accent} />
          <Text style={[type.headline, { color: on ? '#000' : c.accent, marginTop: space.xs }]}>{on ? 'Stop' : 'Start'}</Text>
        </Press>
        <Text style={[type.footnote, { color: c.secondary, textAlign: 'center' }]}>
          {!on ? 'Tap to start watching for hand signs.' : state.paused ? 'Paused. Hold an open palm 2 s, or tap Resume.' : seen ? `Seeing ${seen.emoji}  ${seen.name.toLowerCase()}` : 'Watching. Show your hand to the front camera.'}
        </Text>
        {on && (
          <Press onPress={() => setPaused(!state.paused)} style={[styles.chip, { backgroundColor: c.surface }]}>
            <Text style={[type.headline, { color: c.text }]}>{state.paused ? 'Resume' : 'Pause'}</Text>
          </Press>
        )}
      </View>

      <SectionLabel>Setup</SectionLabel>
      <Card>
        <Row
          first
          label="Camera"
          detail="Sees your hand. Frames never leave the phone."
          right={state.camera ? <Ok /> : <Action label="Allow" onPress={start} />}
        />
        <Row
          label="Phone control"
          detail="Lets dumscrl swipe and tap for you."
          right={state.phoneControl ? <Ok /> : <Action label="Turn on" onPress={openPhoneControl} />}
        />
      </Card>
    </Page>
  );
}

function Ok() {
  const c = useColors();
  return <Ionicons name="checkmark-circle" size={24} color={c.green} accessibilityLabel="Done" />;
}

function Action({ label, onPress }: { label: string; onPress: () => void }) {
  const c = useColors();
  return (
    <Press onPress={onPress} style={[styles.action, { backgroundColor: c.accent }]}>
      <Text style={[type.headline, { color: '#fff', fontSize: 15 }]}>{label}</Text>
    </Press>
  );
}

const styles = StyleSheet.create({
  center: { alignItems: 'center', gap: space.m, paddingVertical: space.l },
  big: { width: 188, height: 188, borderRadius: radius.pill, borderWidth: 3, alignItems: 'center', justifyContent: 'center', overflow: 'hidden' },
  bigFill: { borderRadius: radius.pill },
  chip: { paddingHorizontal: space.l, paddingVertical: 10, borderRadius: radius.pill },
  action: { paddingHorizontal: space.m, paddingVertical: 8, borderRadius: radius.pill },
});
