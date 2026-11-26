import type { ReactNode } from 'react';
import { ScrollView, StyleSheet, Text, View, type StyleProp, type ViewStyle } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { radius, space, type, useColors } from './theme';

/** A scrolling page with a large title, padded clear of the floating tab bar. */
export function Page({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  const c = useColors();
  const insets = useSafeAreaInsets();
  return (
    <ScrollView style={{ flex: 1, backgroundColor: c.bg }} contentContainerStyle={{ paddingTop: insets.top + space.l, paddingBottom: insets.bottom + 120, paddingHorizontal: space.m, gap: space.m }}>
      <View style={{ gap: space.xs, paddingHorizontal: space.xs }}>
        <Text style={[type.title, { color: c.text }]} accessibilityRole="header">{title}</Text>
        {subtitle ? <Text style={[type.body, { color: c.secondary }]}>{subtitle}</Text> : null}
      </View>
      {children}
    </ScrollView>
  );
}

export function Card({ children, style }: { children: ReactNode; style?: StyleProp<ViewStyle> }) {
  const c = useColors();
  return <View style={[{ backgroundColor: c.surface, borderRadius: radius.card, overflow: 'hidden' }, style]}>{children}</View>;
}

/** One line in a card: label on the left, value or control on the right. Hairline between rows. */
export function Row({ label, detail, right, first }: { label: string; detail?: string; right?: ReactNode; first?: boolean }) {
  const c = useColors();
  return (
    <View style={[styles.row, !first && { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: c.separator }]}>
      <View style={{ flex: 1, gap: 2 }}>
        <Text style={[type.body, { color: c.text }]}>{label}</Text>
        {detail ? <Text style={[type.footnote, { color: c.secondary }]}>{detail}</Text> : null}
      </View>
      {right}
    </View>
  );
}

export function SectionLabel({ children }: { children: string }) {
  const c = useColors();
  return <Text style={[type.footnote, { color: c.secondary, textTransform: 'uppercase', letterSpacing: 0.6, marginLeft: space.m, marginBottom: -space.s }]}>{children}</Text>;
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: space.m, paddingHorizontal: space.m, paddingVertical: 14, minHeight: 52 },
});
