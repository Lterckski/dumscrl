import { StyleSheet, Text, View } from 'react-native';
import { Metal } from '../Metal';
import type { Mode } from '../../modules/dumscrl-engine';
import { FLICKS, SIGNS } from '../signs';
import { Card, Page, Row, SectionLabel } from '../ui';
import { radius, space, type, useColors } from '../theme';

export function Signs({ current, mode }: { current: string; mode: Mode }) {
  const c = useColors();
  const flicks = mode === 'flicks';
  const signs = SIGNS.filter((s) => !flicks || s.inFlicks);
  return (
    <Page title="Signs" subtitle="Hold a sign still for a moment. To repeat it, change the sign or drop your hand, then show it again.">
      {flicks && (
        <>
          <SectionLabel>Flicks</SectionLabel>
          <Card>
            {FLICKS.map((f, i) => (
              <Row key={f.key} first={i === 0} label={`${f.emoji}   ${f.name}`} right={<Text style={[type.headline, { color: c.secondary, fontSize: 15 }]}>{f.does}</Text>} />
            ))}
          </Card>
          <SectionLabel>Signs</SectionLabel>
        </>
      )}
      <Card>
        {signs.map((s, i) => (
          <Row
            key={s.key}
            first={i === 0}
            label={`${s.emoji}   ${s.name}`}
            right={
              s.key === current ? (
                <Metal style={styles.on}>
                  <Text style={[type.headline, { color: c.onMetal, fontSize: 14 }]}>{s.does}</Text>
                </Metal>
              ) : (
                <Text style={[type.headline, { color: c.secondary, fontSize: 15 }]}>{s.does}</Text>
              )
            }
          />
        ))}
      </Card>
      <SectionLabel>Tips</SectionLabel>
      <Card>
        <View style={{ padding: space.m, gap: space.s }}>
          <Text style={[type.body, { color: c.text }]}>Prop the phone up or hold it at face height, with your other hand in front of the camera.</Text>
          <Text style={[type.body, { color: c.text }]}>While dumscrl is on, the sign you're making lights up here.</Text>
        </View>
      </Card>
    </Page>
  );
}

const styles = StyleSheet.create({
  on: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: radius.pill, overflow: 'hidden' },
});
