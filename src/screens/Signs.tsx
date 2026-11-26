import { StyleSheet, Text, View } from 'react-native';
import { Metal } from '../Metal';
import { SIGNS } from '../signs';
import { Card, Page, Row, SectionLabel } from '../ui';
import { radius, space, type, useColors } from '../theme';

export function Signs({ current }: { current: string }) {
  const c = useColors();
  return (
    <Page title="Signs" subtitle="Hold a sign for a moment. To repeat it, change the sign or drop your hand, then show it again.">
      <Card>
        {SIGNS.map((s, i) => (
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
