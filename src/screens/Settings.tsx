import Ionicons from '@expo/vector-icons/Ionicons';
import { Linking, StyleSheet, Text, View } from 'react-native';
import type { EngineApi } from '../engine';
import { Press } from '../Press';
import { Card, Page, Row, SectionLabel } from '../ui';
import { applyTheme, radius, savedTheme, space, type, useColors } from '../theme';
import Slider from '@react-native-community/slider';
import { Engine, type ThemePref } from '../../modules/dumscrl-engine';
import { Metal } from '../Metal';
import { useState } from 'react';

const THEMES: { key: ThemePref; label: string }[] = [
  { key: 'system', label: 'System' },
  { key: 'light', label: 'Light' },
  { key: 'dark', label: 'Dark' },
];

export function Settings({ engine }: { engine: EngineApi }) {
  const c = useColors();
  const { state, setMode, openPhoneControl } = engine;
  const [theme, setTheme] = useState<ThemePref>(savedTheme);
  const [cooldown, setCooldown] = useState(Engine.getFlickCooldown);
  const pickTheme = (t: ThemePref) => {
    setTheme(t);
    applyTheme(t);
  };
  return (
    <Page title="Settings">
      <SectionLabel>Control with</SectionLabel>
      <Card>
        <Press onPress={() => setMode('signs')} scaleTo={0.98} accessibilityRole="radio" accessibilityState={{ checked: state.mode === 'signs' }}>
          <Row first label="Hand signs" detail="Hold up a sign to scroll, like or flip photos." right={state.mode === 'signs' ? <Ionicons name="checkmark" size={22} color={c.accent} /> : null} />
        </Press>
        <Press onPress={() => setMode('flicks')} scaleTo={0.98} accessibilityRole="radio" accessibilityState={{ checked: state.mode === 'flicks' }}>
          <Row label="Flicks" detail="Fingers up and flick for videos, hand sideways and flick for photos. Index for next, index and middle for previous. Like, three, four and the palm pause stay as signs." right={state.mode === 'flicks' ? <Ionicons name="checkmark" size={22} color={c.accent} /> : null} />
        </Press>
      </Card>

      {state.mode === 'flicks' && (
        <>
          <SectionLabel>Flicks</SectionLabel>
          <Card style={styles.sliderCard}>
            <View style={styles.sliderHead}>
              <Text style={[type.body, { color: c.text }]}>Wait between flicks</Text>
              <Text style={[type.body, { color: c.secondary, fontVariant: ['tabular-nums'] }]}>{(cooldown / 1000).toFixed(1)} s</Text>
            </View>
            <Slider
              minimumValue={300}
              maximumValue={2000}
              step={100}
              value={cooldown}
              onValueChange={setCooldown}
              onSlidingComplete={(v) => Engine.setFlickCooldown(Math.round(v))}
              minimumTrackTintColor={c.accent}
              maximumTrackTintColor={c.separator}
              thumbTintColor={c.accent}
              accessibilityLabel="Wait between flicks"
            />
            <Text style={[type.footnote, { color: c.secondary }]}>Longer stops one flick from scrolling twice. Bringing your finger back never counts as the opposite flick.</Text>
          </Card>
        </>
      )}

      <SectionLabel>Appearance</SectionLabel>
      <View style={[styles.segment, { backgroundColor: c.surface }]}>
        {THEMES.map((t) => {
          const on = t.key === theme;
          return (
            <Press key={t.key} containerStyle={{ flex: 1 }} scaleTo={0.94} onPress={() => pickTheme(t.key)} accessibilityRole="radio" accessibilityState={{ checked: on }}>
              {on ? (
                <Metal style={styles.segmentItem}>
                  <Text style={[type.headline, { color: c.onMetal, fontSize: 15 }]}>{t.label}</Text>
                </Metal>
              ) : (
                <View style={styles.segmentItem}>
                  <Text style={[type.headline, { color: c.secondary, fontSize: 15 }]}>{t.label}</Text>
                </View>
              )}
            </Press>
          );
        })}
      </View>

      <SectionLabel>Phone</SectionLabel>
      <Card>
        <Press onPress={openPhoneControl} scaleTo={0.98}>
          <Row first label="Phone control" detail={state.phoneControl ? 'On' : 'Off: dumscrl can see signs but not act on them'} right={<Ionicons name="chevron-forward" size={20} color={c.secondary} />} />
        </Press>
        <Press onPress={() => Linking.openSettings()} scaleTo={0.98}>
          <Row label="App permissions" detail="Camera and notifications" right={<Ionicons name="chevron-forward" size={20} color={c.secondary} />} />
        </Press>
      </Card>

      <SectionLabel>About</SectionLabel>
      <Card>
        <Row first label="dumscrl" detail="Hand tracking runs on the phone. Nothing is recorded or sent." right={<Text style={[type.footnote, { color: c.secondary }]}>1.0</Text>} />
      </Card>
    </Page>
  );
}

const styles = StyleSheet.create({
  sliderCard: { padding: space.m, gap: space.s },
  sliderHead: { flexDirection: 'row', justifyContent: 'space-between' },
  segment: { flexDirection: 'row', padding: 4, borderRadius: radius.pill, gap: 4 },
  segmentItem: { alignItems: 'center', paddingVertical: 10, borderRadius: radius.pill, overflow: 'hidden' },
});
