import Ionicons from '@expo/vector-icons/Ionicons';
import { useEffect, useRef } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';
import type { EngineApi } from '../engine';
import { Metal } from '../Metal';
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
    <Page title="dumscrl" icon={require('../../assets/logo.png')} subtitle="Scroll, like and flip photos with a hand sign. No touching the screen.">
      <View style={styles.center}>
        <Press
          onPress={on ? stop : start}
          scaleTo={0.94}
          style={styles.big}
          accessibilityRole="button"
          accessibilityLabel={on ? 'Stop dumscrl' : 'Start dumscrl'}
        >
          {/* Metal ring when off; the metal fills in from the centre when on. */}
          <Metal style={StyleSheet.absoluteFill} />
          <View style={[styles.inner, { backgroundColor: c.disc }]} />
          <Animated.View style={[StyleSheet.absoluteFill, styles.round, { opacity: fill, transform: [{ scale: fill }] }]}>
            <Metal style={[StyleSheet.absoluteFill, styles.round]} />
          </Animated.View>
          <Ionicons name={on ? 'stop' : 'play'} size={44} color={on ? c.onMetal : c.accent} />
          <Text style={[type.headline, { color: on ? c.onMetal : c.accent, marginTop: space.xs }]}>{on ? 'Stop' : 'Start'}</Text>
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
          detail={state.phoneControl ? "On. Turn off opens Accessibility; tap dumscrl there." : "Lets dumscrl swipe and tap for you."}
          right={<Action label={state.phoneControl ? 'Turn off' : 'Turn on'} onPress={openPhoneControl} />}
        />
      </Card>
    </Page>
  );
}

function Ok() {
  const c = useColors();
  return <Ionicons name="checkmark-circle" size={24} color={c.accent} accessibilityLabel="Done" />;
}

function Action({ label, onPress }: { label: string; onPress: () => void }) {
  const c = useColors();
  return (
    <Press onPress={onPress}>
      <Metal style={styles.action}>
        <Text style={[type.headline, { color: c.onMetal, fontSize: 15 }]}>{label}</Text>
      </Metal>
    </Press>
  );
}

const styles = StyleSheet.create({
  center: { alignItems: 'center', gap: space.m, paddingVertical: space.l },
  big: { width: 188, height: 188, borderRadius: radius.pill, alignItems: 'center', justifyContent: 'center', overflow: 'hidden' },
  round: { borderRadius: radius.pill, overflow: 'hidden' },
  inner: { position: 'absolute', top: 3, left: 3, right: 3, bottom: 3, borderRadius: radius.pill },
  chip: { paddingHorizontal: space.l, paddingVertical: 10, borderRadius: radius.pill },
  action: { paddingHorizontal: space.m, paddingVertical: 8, borderRadius: radius.pill, overflow: 'hidden' },
});
