import { Text, View } from 'react-native';
import { SIGNS } from '../signs';
import { Card, Page, Row, SectionLabel } from '../ui';
import { space, type, useColors } from '../theme';

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
            right={<Text style={[type.headline, { color: s.key === current ? c.green : c.secondary, fontSize: 15 }]}>{s.does}</Text>}
          />
        ))}
      </Card>
      <SectionLabel>Tips</SectionLabel>
      <Card>
        <View style={{ padding: space.m, gap: space.s }}>
          <Text style={[type.body, { color: c.text }]}>Prop the phone up or hold it at face height, with your other hand in front of the camera.</Text>
          <Text style={[type.body, { color: c.text }]}>The sign you're making lights up green here while dumscrl is on.</Text>
        </View>
      </Card>
    </Page>
  );
}
