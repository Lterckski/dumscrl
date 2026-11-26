import { LinearGradient } from 'expo-linear-gradient';
import type { ReactNode } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';
import { useColors } from './theme';

/** A brushed-metal surface: diagonal gradient plus a soft sheen across the top half. */
export function Metal({ style, children }: { style?: StyleProp<ViewStyle>; children?: ReactNode }) {
  const c = useColors();
  return (
    <LinearGradient colors={c.metal} locations={[0, 0.45, 0.7, 1]} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={style}>
      <View pointerEvents="none" style={[StyleSheet.absoluteFill, styles.sheen, { backgroundColor: c.sheen }]} />
      {children}
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  sheen: { bottom: '55%', opacity: 0.5 },
});
