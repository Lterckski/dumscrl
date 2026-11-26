import Ionicons from '@expo/vector-icons/Ionicons';
import { useEffect, useRef, useState } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Press } from './Press';
import { radius, spring, type, useColors } from './theme';

export const TABS = [
  { key: 'home', label: 'Home', icon: 'radio-button-on' },
  { key: 'camera', label: 'Camera', icon: 'videocam' },
  { key: 'signs', label: 'Signs', icon: 'hand-left' },
  { key: 'settings', label: 'Settings', icon: 'options' },
] as const;
export type Tab = (typeof TABS)[number]['key'];

/** Floating pill tab bar; the highlight slides to the chosen tab on a spring. */
export function TabBar({ tab, onChange }: { tab: Tab; onChange: (t: Tab) => void }) {
  const c = useColors();
  const insets = useSafeAreaInsets();
  const [width, setWidth] = useState(0);
  const index = TABS.findIndex((t) => t.key === tab);
  const x = useRef(new Animated.Value(index)).current;
  useEffect(() => Animated.spring(x, { toValue: index, ...spring }).start(), [index, x]);
  const slot = width / TABS.length;

  return (
    <View style={[styles.wrap, { bottom: insets.bottom + 12 }]} pointerEvents="box-none">
      <View style={[styles.bar, { backgroundColor: c.bar, borderColor: c.separator }]} onLayout={(e) => setWidth(e.nativeEvent.layout.width - 12)}>
        {width > 0 && (
          <Animated.View
            style={[
              styles.highlight,
              { width: slot, backgroundColor: c.raised, transform: [{ translateX: Animated.multiply(x, slot) }] },
            ]}
          />
        )}
        {TABS.map((t) => {
          const on = t.key === tab;
          return (
            <Press key={t.key} containerStyle={{ flex: 1 }} style={styles.item} scaleTo={0.9} onPress={() => onChange(t.key)} accessibilityRole="tab" accessibilityState={{ selected: on }} accessibilityLabel={t.label}>
              <Ionicons name={t.icon} size={22} color={on ? c.text : c.secondary} />
              <Text style={[type.caption, { color: on ? c.text : c.secondary, marginTop: 2 }]}>{t.label}</Text>
            </Press>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { position: 'absolute', left: 16, right: 16, alignItems: 'center' },
  bar: { flexDirection: 'row', padding: 6, borderRadius: radius.pill, borderWidth: StyleSheet.hairlineWidth, width: '100%', maxWidth: 420 },
  highlight: { position: 'absolute', top: 6, bottom: 6, left: 6, borderRadius: radius.pill },
  item: { alignItems: 'center', justifyContent: 'center', paddingVertical: 8 },
});
